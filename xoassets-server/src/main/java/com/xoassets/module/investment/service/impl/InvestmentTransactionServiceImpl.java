package com.xoassets.module.investment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.account.service.AccountService;
import com.xoassets.module.investment.dto.InvestmentTransactionRequest;
import com.xoassets.module.investment.service.AssetService;
import com.xoassets.module.investment.service.HoldingService;
import com.xoassets.module.investment.service.HoldingTradeResult;
import com.xoassets.module.investment.service.InvestmentTransactionService;
import com.xoassets.module.investment.vo.InvestmentTransactionVO;
import com.xoassets.persistence.entity.Account;
import com.xoassets.persistence.entity.Asset;
import com.xoassets.persistence.entity.Holding;
import com.xoassets.persistence.entity.InvestmentTransaction;
import com.xoassets.persistence.mapper.AccountMapper;
import com.xoassets.persistence.mapper.AssetMapper;
import com.xoassets.persistence.mapper.InvestmentTransactionMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 投资交易服务实现。
 */
@Service
public class InvestmentTransactionServiceImpl implements InvestmentTransactionService {

    private static final String TYPE_BUY = "BUY";
    private static final String TYPE_SELL = "SELL";

    private final InvestmentTransactionMapper transactionMapper;
    private final AssetMapper assetMapper;
    private final AccountMapper accountMapper;
    private final AssetService assetService;
    private final HoldingService holdingService;
    private final AccountService accountService;

    public InvestmentTransactionServiceImpl(
            InvestmentTransactionMapper transactionMapper,
            AssetMapper assetMapper,
            AccountMapper accountMapper,
            AssetService assetService,
            HoldingService holdingService,
            AccountService accountService) {
        this.transactionMapper = transactionMapper;
        this.assetMapper = assetMapper;
        this.accountMapper = accountMapper;
        this.assetService = assetService;
        this.holdingService = holdingService;
        this.accountService = accountService;
    }

    /**
     * 创建买入或卖出交易，并在同一事务中联动持仓。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public InvestmentTransactionVO create(InvestmentTransactionRequest request) {
        Long userId = LoginUserContext.getUserId();
        ensureType(request.getType());
        assetService.findAsset(request.getAssetId());
        Account account = accountService.findOwnedAccount(request.getAccountId(), userId);
        BigDecimal quantity = scale4(request.getQuantity());
        BigDecimal price = scale4(request.getPrice());
        BigDecimal fee = scale4(request.getFee());
        BigDecimal amount = quantity.multiply(price).setScale(4, RoundingMode.HALF_UP);
        // 账户余额、持仓和交易记录在同一事务中完成，任一失败都会回滚。
        HoldingTradeResult tradeResult;
        if (TYPE_BUY.equals(request.getType())) {
            BigDecimal actualPay = amount.add(fee).setScale(4, RoundingMode.HALF_UP);
            if (account.getBalance().compareTo(actualPay) < 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账户余额不足");
            }
            accountService.adjustBalance(userId, account.getId(), actualPay.negate());
            tradeResult = holdingService.applyBuy(userId, request.getHoldingId(), request.getAssetId(), quantity, price, fee);
        } else {
            if (request.getHoldingId() == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "卖出必须选择持仓");
            }
            BigDecimal actualIncome = amount.subtract(fee).setScale(4, RoundingMode.HALF_UP);
            tradeResult = holdingService.applySell(userId, request.getHoldingId(), request.getAssetId(), quantity, price, fee);
            accountService.adjustBalance(userId, account.getId(), actualIncome);
        }
        Holding holding = tradeResult.holding();

        InvestmentTransaction transaction = new InvestmentTransaction();
        transaction.setUserId(userId);
        transaction.setHoldingId(holding.getId());
        transaction.setAssetId(request.getAssetId());
        transaction.setAccountId(account.getId());
        transaction.setType(request.getType());
        transaction.setQuantity(quantity);
        transaction.setPrice(price);
        transaction.setAmount(amount);
        transaction.setFee(fee);
        transaction.setRealizedProfit(tradeResult.realizedProfit());
        transaction.setTransactionTime(request.getTransactionTime());
        transaction.setNote(request.getNote());
        transaction.setDeleted(0);
        transactionMapper.insert(transaction);
        return toVO(transaction, assetMapper.selectById(transaction.getAssetId()), account);
    }

    /**
     * 查询当前用户投资交易，可按持仓过滤。
     */
    @Override
    public List<InvestmentTransactionVO> list(Long holdingId) {
        Long userId = LoginUserContext.getUserId();
        LambdaQueryWrapper<InvestmentTransaction> wrapper = new LambdaQueryWrapper<InvestmentTransaction>()
                .eq(InvestmentTransaction::getUserId, userId)
                .orderByDesc(InvestmentTransaction::getTransactionTime)
                .orderByDesc(InvestmentTransaction::getCreatedAt);
        if (holdingId != null) {
            holdingService.findOwnedHolding(holdingId, userId);
            wrapper.eq(InvestmentTransaction::getHoldingId, holdingId);
        }
        List<InvestmentTransaction> transactions = transactionMapper.selectList(wrapper);
        Map<Long, Asset> assetMap = transactions.isEmpty()
                ? Map.of()
                : assetMapper.selectBatchIds(transactions.stream().map(InvestmentTransaction::getAssetId).collect(Collectors.toSet()))
                        .stream()
                        .collect(Collectors.toMap(Asset::getId, asset -> asset));
        Map<Long, Account> accountMap = transactions.isEmpty()
                ? Map.of()
                : accountMapper.selectBatchIds(transactions.stream().map(InvestmentTransaction::getAccountId).collect(Collectors.toSet()))
                        .stream()
                        .collect(Collectors.toMap(Account::getId, account -> account));
        return transactions.stream().map(transaction -> toVO(transaction, assetMap.get(transaction.getAssetId()), accountMap.get(transaction.getAccountId()))).toList();
    }

    /**
     * 投资交易类型白名单。
     */
    private void ensureType(String type) {
        if (!TYPE_BUY.equals(type) && !TYPE_SELL.equals(type)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "投资交易类型只支持 BUY 或 SELL");
        }
    }

    /**
     * 投资交易入库前统一四位小数，确保持仓联动和交易记录金额口径一致。
     */
    private BigDecimal scale4(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 转换投资交易展示对象。
     */
    private InvestmentTransactionVO toVO(InvestmentTransaction transaction, Asset asset, Account account) {
        return InvestmentTransactionVO.builder()
                .id(transaction.getId())
                .holdingId(transaction.getHoldingId())
                .assetId(transaction.getAssetId())
                .accountId(transaction.getAccountId())
                .accountName(account == null ? null : account.getName())
                .assetName(asset == null ? null : asset.getName())
                .symbol(asset == null ? null : asset.getSymbol())
                .type(transaction.getType())
                .quantity(transaction.getQuantity())
                .price(transaction.getPrice())
                .amount(transaction.getAmount())
                .fee(transaction.getFee())
                .realizedProfit(transaction.getRealizedProfit())
                .transactionTime(transaction.getTransactionTime())
                .note(transaction.getNote())
                .build();
    }
}
