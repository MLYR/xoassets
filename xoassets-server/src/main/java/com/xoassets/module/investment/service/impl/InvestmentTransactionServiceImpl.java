package com.xoassets.module.investment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.investment.dto.InvestmentTransactionRequest;
import com.xoassets.module.investment.service.AssetService;
import com.xoassets.module.investment.service.HoldingService;
import com.xoassets.module.investment.service.InvestmentTransactionService;
import com.xoassets.module.investment.vo.InvestmentTransactionVO;
import com.xoassets.persistence.entity.Asset;
import com.xoassets.persistence.entity.Holding;
import com.xoassets.persistence.entity.InvestmentTransaction;
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
    private final AssetService assetService;
    private final HoldingService holdingService;

    public InvestmentTransactionServiceImpl(
            InvestmentTransactionMapper transactionMapper,
            AssetMapper assetMapper,
            AssetService assetService,
            HoldingService holdingService) {
        this.transactionMapper = transactionMapper;
        this.assetMapper = assetMapper;
        this.assetService = assetService;
        this.holdingService = holdingService;
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
        BigDecimal fee = request.getFee() == null ? BigDecimal.ZERO : request.getFee();
        // 先联动持仓再保存交易记录，任一环节失败都回滚，避免交易和持仓数量不一致。
        Holding holding = TYPE_BUY.equals(request.getType())
                ? holdingService.applyBuy(userId, request.getHoldingId(), request.getAssetId(), request.getQuantity(), request.getPrice(), fee)
                : holdingService.applySell(userId, request.getHoldingId(), request.getAssetId(), request.getQuantity());

        InvestmentTransaction transaction = new InvestmentTransaction();
        transaction.setUserId(userId);
        transaction.setHoldingId(holding.getId());
        transaction.setAssetId(request.getAssetId());
        transaction.setType(request.getType());
        transaction.setQuantity(request.getQuantity());
        transaction.setPrice(request.getPrice());
        transaction.setAmount(request.getQuantity().multiply(request.getPrice()).setScale(4, RoundingMode.HALF_UP));
        transaction.setFee(fee);
        transaction.setTransactionTime(request.getTransactionTime());
        transaction.setNote(request.getNote());
        transaction.setDeleted(0);
        transactionMapper.insert(transaction);
        return toVO(transaction, assetMapper.selectById(transaction.getAssetId()));
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
        return transactions.stream().map(transaction -> toVO(transaction, assetMap.get(transaction.getAssetId()))).toList();
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
     * 转换投资交易展示对象。
     */
    private InvestmentTransactionVO toVO(InvestmentTransaction transaction, Asset asset) {
        return InvestmentTransactionVO.builder()
                .id(transaction.getId())
                .holdingId(transaction.getHoldingId())
                .assetId(transaction.getAssetId())
                .assetName(asset == null ? null : asset.getName())
                .symbol(asset == null ? null : asset.getSymbol())
                .type(transaction.getType())
                .quantity(transaction.getQuantity())
                .price(transaction.getPrice())
                .amount(transaction.getAmount())
                .fee(transaction.getFee())
                .transactionTime(transaction.getTransactionTime())
                .note(transaction.getNote())
                .build();
    }
}
