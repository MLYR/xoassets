package com.xoassets.module.investment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.account.service.AccountService;
import com.xoassets.module.investment.dto.InvestmentTransactionConvertRequest;
import com.xoassets.module.investment.dto.InvestmentTransactionRequest;
import com.xoassets.module.investment.dto.InvestmentTransactionRevokeRequest;
import com.xoassets.module.investment.scheduler.InvestmentDailySnapshotJob;
import com.xoassets.module.investment.service.AssetService;
import com.xoassets.module.investment.service.FundConfirmDateService;
import com.xoassets.module.investment.service.HoldingService;
import com.xoassets.module.investment.service.HoldingTradeResult;
import com.xoassets.module.investment.service.InvestmentTransactionService;
import com.xoassets.module.investment.vo.FundConfirmPreviewVO;
import com.xoassets.module.investment.vo.InvestmentTransactionVO;
import com.xoassets.module.snapshot.service.SnapshotService;
import com.xoassets.persistence.entity.Account;
import com.xoassets.persistence.entity.Asset;
import com.xoassets.persistence.entity.AssetPriceCurrent;
import com.xoassets.persistence.entity.AssetPriceDaily;
import com.xoassets.persistence.entity.Holding;
import com.xoassets.persistence.entity.InvestmentTransaction;
import com.xoassets.persistence.mapper.AccountMapper;
import com.xoassets.persistence.mapper.AssetMapper;
import com.xoassets.persistence.mapper.AssetPriceCurrentMapper;
import com.xoassets.persistence.mapper.AssetPriceDailyMapper;
import com.xoassets.persistence.mapper.InvestmentTransactionMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static final String ASSET_TYPE_FUND = "FUND";
    private static final String INPUT_MODE_QUANTITY_PRICE = "QUANTITY_PRICE";
    private static final String INPUT_MODE_AMOUNT_NAV = "AMOUNT_NAV";
    private static final String STATUS_NORMAL = "NORMAL";
    private static final String STATUS_REVOKED = "REVOKED";
    private static final String STATUS_PENDING_CONFIRM = "PENDING_CONFIRM";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final InvestmentTransactionMapper transactionMapper;
    private final AssetMapper assetMapper;
    private final AssetPriceDailyMapper assetPriceDailyMapper;
    private final AssetPriceCurrentMapper assetPriceCurrentMapper;
    private final AccountMapper accountMapper;
    private final AssetService assetService;
    private final FundConfirmDateService fundConfirmDateService;
    private final HoldingService holdingService;
    private final AccountService accountService;
    private final SnapshotService snapshotService;
    private final InvestmentDailySnapshotJob investmentDailySnapshotJob;

    public InvestmentTransactionServiceImpl(
            InvestmentTransactionMapper transactionMapper,
            AssetMapper assetMapper,
            AssetPriceDailyMapper assetPriceDailyMapper,
            AssetPriceCurrentMapper assetPriceCurrentMapper,
            AccountMapper accountMapper,
            AssetService assetService,
            FundConfirmDateService fundConfirmDateService,
            HoldingService holdingService,
            AccountService accountService,
            SnapshotService snapshotService,
            InvestmentDailySnapshotJob investmentDailySnapshotJob) {
        this.transactionMapper = transactionMapper;
        this.assetMapper = assetMapper;
        this.assetPriceDailyMapper = assetPriceDailyMapper;
        this.assetPriceCurrentMapper = assetPriceCurrentMapper;
        this.accountMapper = accountMapper;
        this.assetService = assetService;
        this.fundConfirmDateService = fundConfirmDateService;
        this.holdingService = holdingService;
        this.accountService = accountService;
        this.snapshotService = snapshotService;
        this.investmentDailySnapshotJob = investmentDailySnapshotJob;
    }

    /**
     * 创建买入或卖出交易，并在同一事务中联动持仓。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public InvestmentTransactionVO create(InvestmentTransactionRequest request) {
        Long userId = LoginUserContext.getUserId();
        ensureType(request.getType());
        Asset asset = assetService.findAsset(request.getAssetId());
        if (isFundAmountBuy(request, asset)) {
            return createFundAmountBuy(userId, request, asset);
        }
        Account account = accountService.findOwnedAccount(request.getAccountId(), userId);
        if (request.getQuantity() == null || request.getPrice() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数量和价格不能为空");
        }
        BigDecimal quantity = scaleQuantity(request.getQuantity());
        BigDecimal price = scale4(request.getPrice());
        BigDecimal fee = scale4(request.getFee());
        BigDecimal amount = quantity.multiply(price).setScale(4, RoundingMode.HALF_UP);
        // 账户余额、持仓和交易记录在同一事务中完成，任一失败都会回滚。
        HoldingTradeResult tradeResult;
        BigDecimal costAmount;
        if (TYPE_BUY.equals(request.getType())) {
            BigDecimal actualPay = amount.add(fee).setScale(4, RoundingMode.HALF_UP);
            if (account.getBalance().compareTo(actualPay) < 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账户余额不足");
            }
            accountService.adjustBalance(userId, account.getId(), actualPay.negate());
            tradeResult = holdingService.applyBuy(userId, request.getHoldingId(), request.getAssetId(), quantity, price, fee);
            costAmount = actualPay;
        } else {
            if (request.getHoldingId() == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "卖出必须选择持仓");
            }
            BigDecimal actualIncome = amount.subtract(fee).setScale(4, RoundingMode.HALF_UP);
            tradeResult = holdingService.applySell(userId, request.getHoldingId(), request.getAssetId(), quantity, price, fee);
            accountService.adjustBalance(userId, account.getId(), actualIncome);
            costAmount = tradeResult.sellCost();
        }
        Holding holding = tradeResult.holding();

        InvestmentTransaction transaction = new InvestmentTransaction();
        transaction.setUserId(userId);
        transaction.setHoldingId(holding.getId());
        transaction.setAssetId(request.getAssetId());
        transaction.setAccountId(account.getId());
        transaction.setType(request.getType());
        transaction.setInputMode(INPUT_MODE_QUANTITY_PRICE);
        transaction.setTradeAmount(TYPE_BUY.equals(request.getType()) ? amount.add(fee).setScale(4, RoundingMode.HALF_UP) : amount.subtract(fee).setScale(4, RoundingMode.HALF_UP));
        transaction.setTradeQuantity(quantity);
        transaction.setTradePrice(price);
        transaction.setQuantity(quantity);
        transaction.setPrice(price);
        transaction.setAmount(amount);
        transaction.setFee(fee);
        transaction.setCostAmount(costAmount);
        transaction.setRealizedProfit(tradeResult.realizedProfit());
        transaction.setTradeDate(request.getTransactionTime().toLocalDate());
        transaction.setConfirmedDate(request.getTransactionTime().toLocalDate());
        transaction.setConfirmedNav(price);
        transaction.setConfirmedQuantity(quantity);
        transaction.setStatus(STATUS_CONFIRMED);
        transaction.setTransactionTime(request.getTransactionTime());
        transaction.setNote(request.getNote());
        transaction.setDeleted(0);
        transactionMapper.insert(transaction);
        return toVO(transaction, assetMapper.selectById(transaction.getAssetId()), account);
    }

    /**
     * 转换持仓使用“先卖出源持仓、再买入目标持仓”的真实交易链路，保证账户余额和持仓成本仍由原业务规则维护。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public List<InvestmentTransactionVO> convert(InvestmentTransactionConvertRequest request) {
        Long userId = LoginUserContext.getUserId();
        if (request.getSourceHoldingId().equals(request.getTargetHoldingId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "源持仓和目标持仓不能相同");
        }
        Holding sourceHolding = holdingService.findOwnedHolding(request.getSourceHoldingId(), userId);
        Holding targetHolding = holdingService.findOwnedHolding(request.getTargetHoldingId(), userId);

        InvestmentTransactionRequest sellRequest = new InvestmentTransactionRequest();
        sellRequest.setHoldingId(sourceHolding.getId());
        sellRequest.setAssetId(sourceHolding.getAssetId());
        sellRequest.setAccountId(request.getAccountId());
        sellRequest.setType(TYPE_SELL);
        sellRequest.setQuantity(request.getSourceQuantity());
        sellRequest.setPrice(request.getSourcePrice());
        sellRequest.setFee(BigDecimal.ZERO);
        sellRequest.setTransactionTime(request.getTransactionTime());
        sellRequest.setNote(convertNote(request.getNote(), "转换转出"));

        InvestmentTransactionRequest buyRequest = new InvestmentTransactionRequest();
        buyRequest.setHoldingId(targetHolding.getId());
        buyRequest.setAssetId(targetHolding.getAssetId());
        buyRequest.setAccountId(request.getAccountId());
        buyRequest.setType(TYPE_BUY);
        buyRequest.setQuantity(request.getTargetQuantity());
        buyRequest.setPrice(request.getTargetPrice());
        buyRequest.setFee(request.getFee());
        buyRequest.setTransactionTime(request.getTransactionTime());
        buyRequest.setNote(convertNote(request.getNote(), "转换转入"));

        InvestmentTransactionVO sell = create(sellRequest);
        InvestmentTransactionVO buy = create(buyRequest);
        return List.of(sell, buy);
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
     * 撤销投资交易，按交易记录里的资金金额和成本金额做精确反向恢复。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public InvestmentTransactionVO revoke(Long id, InvestmentTransactionRevokeRequest request) {
        Long userId = LoginUserContext.getUserId();
        InvestmentTransaction transaction = findOwnedTransaction(id, userId);
        if (STATUS_REVOKED.equals(transaction.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "投资交易已撤销，不能重复撤销");
        }
        if (STATUS_CANCELLED.equals(transaction.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "投资交易已取消，不能重复撤销");
        }
        Account account = accountService.findOwnedAccount(transaction.getAccountId(), userId);
        if (STATUS_PENDING_CONFIRM.equals(transaction.getStatus())) {
            accountService.adjustBalance(userId, account.getId(), transaction.getTradeAmount());
            transaction.setStatus(STATUS_CANCELLED);
            transaction.setRevokeTime(LocalDateTime.now());
            transaction.setRevokeReason(request == null ? null : request.getReason());
            transactionMapper.update(transaction, new LambdaUpdateWrapper<InvestmentTransaction>()
                    .eq(InvestmentTransaction::getId, id)
                    .eq(InvestmentTransaction::getUserId, userId)
                    .eq(InvestmentTransaction::getStatus, STATUS_PENDING_CONFIRM));
            return toVO(transaction, assetMapper.selectById(transaction.getAssetId()), account);
        }
        BigDecimal costAmount = transaction.getCostAmount();
        if (costAmount == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "历史交易缺少成本金额，无法撤销");
        }
        // 撤销资金账户和持仓必须同事务完成，避免只恢复一边造成资产口径不一致。
        if (TYPE_BUY.equals(transaction.getType())) {
            accountService.adjustBalance(userId, account.getId(), transaction.getAmount().add(transaction.getFee()));
            holdingService.revokeBuy(userId, transaction.getHoldingId(), transaction.getAssetId(), transaction.getQuantity(), costAmount);
        } else {
            BigDecimal actualIncome = transaction.getAmount().subtract(transaction.getFee()).setScale(4, RoundingMode.HALF_UP);
            if (account.getBalance().compareTo(actualIncome) < 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账户余额不足，无法撤销该卖出交易");
            }
            accountService.adjustBalance(userId, account.getId(), actualIncome.negate());
            holdingService.revokeSell(userId, transaction.getHoldingId(), transaction.getAssetId(), transaction.getQuantity(), costAmount);
        }
        transaction.setStatus(STATUS_REVOKED);
        transaction.setRevokeTime(LocalDateTime.now());
        transaction.setRevokeReason(request == null ? null : request.getReason());
        transactionMapper.update(transaction, new LambdaUpdateWrapper<InvestmentTransaction>()
                .eq(InvestmentTransaction::getId, id)
                .eq(InvestmentTransaction::getUserId, userId));
        return toVO(transaction, assetMapper.selectById(transaction.getAssetId()), account);
    }

    /**
     * 预估基金申购确认日期，供前端在保存前展示顺延和 QDII 提示。
     */
    @Override
    public FundConfirmPreviewVO previewFundConfirm(Long assetId, LocalDateTime transactionTime) {
        Asset asset = assetService.findAsset(assetId);
        return fundConfirmDateService.preview(asset, transactionTime);
    }

    /**
     * 定时确认所有待确认基金买入；查不到确认净值的交易保持待确认，下一轮继续扫描。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void confirmPendingFundBuys() {
        List<InvestmentTransaction> pendingList = transactionMapper.selectList(new LambdaQueryWrapper<InvestmentTransaction>()
                .eq(InvestmentTransaction::getType, TYPE_BUY)
                .eq(InvestmentTransaction::getInputMode, INPUT_MODE_AMOUNT_NAV)
                .eq(InvestmentTransaction::getStatus, STATUS_PENDING_CONFIRM)
                .orderByAsc(InvestmentTransaction::getTransactionTime)
                .last("limit 100"));
        for (InvestmentTransaction transaction : pendingList) {
            confirmPendingFundBuy(transaction);
        }
    }

    /**
     * 基金金额买入：先扣资金账户；已有确认净值则同步确认，否则保留待确认交易。
     */
    private InvestmentTransactionVO createFundAmountBuy(Long userId, InvestmentTransactionRequest request, Asset asset) {
        Account account = accountService.findOwnedAccount(request.getAccountId(), userId);
        BigDecimal tradeAmount = scaleMoney2(request.getTradeAmount());
        BigDecimal fee = scaleMoney2(request.getFee());
        if (tradeAmount.compareTo(BigDecimal.ZERO) <= 0 || tradeAmount.compareTo(fee) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "基金买入总金额必须大于手续费");
        }
        if (account.getBalance().compareTo(tradeAmount) < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账户余额不足");
        }
        Holding holding = request.getHoldingId() == null
                ? holdingService.applyConfirmedBuy(userId, null, asset.getId(), BigDecimal.ZERO, BigDecimal.ZERO).holding()
                : holdingService.findOwnedHolding(request.getHoldingId(), userId);
        LocalDate tradeDate = request.getTransactionTime().toLocalDate();
        LocalDate navDate = fundConfirmDateService.effectiveTradeDate(asset, request.getTransactionTime());
        LocalDate confirmedDate = request.getConfirmedDate() == null
                ? fundConfirmDateService.confirmedDate(asset, request.getTransactionTime())
                : request.getConfirmedDate();
        // 基金申购份额按有效申请日净值计算，确认日只表示份额到账/状态确认日期。
        BigDecimal confirmedNav = fundNavOnDate(asset.getId(), navDate);
        BigDecimal netAmount = tradeAmount.subtract(fee).setScale(2, RoundingMode.HALF_UP);
        accountService.adjustBalance(userId, account.getId(), tradeAmount.negate());
        InvestmentTransaction transaction = new InvestmentTransaction();
        transaction.setUserId(userId);
        transaction.setHoldingId(holding == null ? null : holding.getId());
        transaction.setAssetId(asset.getId());
        transaction.setAccountId(account.getId());
        transaction.setType(TYPE_BUY);
        transaction.setInputMode(INPUT_MODE_AMOUNT_NAV);
        transaction.setTradeAmount(tradeAmount);
        transaction.setTradeQuantity(null);
        transaction.setTradePrice(null);
        transaction.setQuantity(BigDecimal.ZERO.setScale(10, RoundingMode.HALF_UP));
        transaction.setPrice(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        transaction.setAmount(netAmount.setScale(4, RoundingMode.HALF_UP));
        transaction.setFee(fee.setScale(4, RoundingMode.HALF_UP));
        transaction.setTradeDate(tradeDate);
        transaction.setConfirmedDate(confirmedDate);
        transaction.setTransactionTime(request.getTransactionTime());
        transaction.setNote(request.getNote());
        transaction.setDeleted(0);
        if (confirmedNav == null) {
            transaction.setStatus(STATUS_PENDING_CONFIRM);
            transactionMapper.insert(transaction);
            return toVO(transaction, asset, account);
        }
        confirmFundTransactionFields(transaction, confirmedNav);
        HoldingTradeResult tradeResult = holdingService.applyConfirmedBuy(userId, transaction.getHoldingId(), asset.getId(), transaction.getConfirmedQuantity(), transaction.getCostAmount());
        transaction.setHoldingId(tradeResult.holding().getId());
        transactionMapper.insert(transaction);
        refreshSnapshotsAfterConfirmation(transaction);
        return toVO(transaction, asset, account);
    }

    /**
     * 确认单条待确认基金交易，使用状态条件更新保证定时任务幂等。
     */
    private void confirmPendingFundBuy(InvestmentTransaction transaction) {
        Asset asset = assetMapper.selectById(transaction.getAssetId());
        if (asset == null) {
            return;
        }
        LocalDate navDate = fundConfirmDateService.effectiveTradeDate(asset, transaction.getTransactionTime());
        // 历史待确认交易同样按有效申请日净值确认，避免 QDII 等确认日净值延迟导致交易长期卡住。
        BigDecimal confirmedNav = fundNavOnDate(transaction.getAssetId(), navDate);
        if (confirmedNav == null) {
            return;
        }
        confirmFundTransactionFields(transaction, confirmedNav);
        int updated = transactionMapper.update(transaction, new LambdaUpdateWrapper<InvestmentTransaction>()
                .eq(InvestmentTransaction::getId, transaction.getId())
                .eq(InvestmentTransaction::getUserId, transaction.getUserId())
                .eq(InvestmentTransaction::getStatus, STATUS_PENDING_CONFIRM));
        if (updated > 0) {
            // 只有成功抢到待确认状态的任务才能更新持仓，避免重复扫描导致份额重复累加。
            holdingService.applyConfirmedBuy(transaction.getUserId(), transaction.getHoldingId(), transaction.getAssetId(), transaction.getConfirmedQuantity(), transaction.getCostAmount());
            refreshSnapshotsAfterConfirmation(transaction);
        }
    }

    private void refreshSnapshotsAfterConfirmation(InvestmentTransaction transaction) {
        Long userId = transaction.getUserId();
        LocalDate today = LocalDate.now();
        for (LocalDate snapshotDate : confirmationSnapshotDates(transaction, today)) {
            investmentDailySnapshotJob.snapshotForUser(userId, snapshotDate);
            snapshotService.generateForUser(userId, snapshotDate);
        }
    }

    private Set<LocalDate> confirmationSnapshotDates(InvestmentTransaction transaction, LocalDate today) {
        Set<LocalDate> dates = new LinkedHashSet<>();
        LocalDate start = transaction.getTransactionTime() == null ? transaction.getTradeDate() : transaction.getTransactionTime().toLocalDate();
        if (start == null) {
            start = transaction.getConfirmedDate();
        }
        LocalDate end = transaction.getConfirmedDate() == null ? today : transaction.getConfirmedDate();
        if (start != null && end != null) {
            LocalDate cursor = start;
            LocalDate safeEnd = end.isAfter(today) ? today : end;
            while (!cursor.isAfter(safeEnd)) {
                // 基金确认会改变确认日前后的在途资产口径，确认成功后顺手修复该区间历史快照。
                dates.add(cursor);
                cursor = cursor.plusDays(1);
            }
        }
        dates.add(today);
        return dates;
    }

    /**
     * 根据确认净值计算基金确认份额和成本字段。
     */
    private void confirmFundTransactionFields(InvestmentTransaction transaction, BigDecimal confirmedNav) {
        BigDecimal tradeAmount = scaleMoney2(transaction.getTradeAmount());
        BigDecimal fee = scaleMoney2(transaction.getFee());
        BigDecimal netAmount = tradeAmount.subtract(fee).setScale(2, RoundingMode.HALF_UP);
        BigDecimal confirmedQuantity = netAmount.divide(confirmedNav, 10, RoundingMode.DOWN);
        if (confirmedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "确认份额必须大于0");
        }
        transaction.setConfirmedNav(confirmedNav.setScale(6, RoundingMode.HALF_UP));
        transaction.setConfirmedQuantity(confirmedQuantity);
        transaction.setQuantity(confirmedQuantity.setScale(10, RoundingMode.HALF_UP));
        transaction.setPrice(confirmedNav.setScale(4, RoundingMode.HALF_UP));
        transaction.setAmount(netAmount.setScale(4, RoundingMode.HALF_UP));
        transaction.setCostAmount(tradeAmount.setScale(4, RoundingMode.HALF_UP));
        transaction.setRealizedProfit(null);
        transaction.setStatus(STATUS_CONFIRMED);
    }

    /**
     * 查询某日基金单位净值；旧价格快照表退役后只使用 daily，current 仅允许同净值日兜底。
     */
    private BigDecimal fundNavOnDate(Long assetId, LocalDate date) {
        if (date == null) {
            return null;
        }
        // 基金确认优先使用长期日级净值，避免只查当前价导致 QDII 延迟时确认口径漂移。
        AssetPriceDaily dailyPrice = assetPriceDailyMapper.selectOne(new LambdaQueryWrapper<AssetPriceDaily>()
                .eq(AssetPriceDaily::getAssetId, assetId)
                .eq(AssetPriceDaily::getTradeDate, date)
                .orderByDesc(AssetPriceDaily::getCreatedAt)
                .last("limit 1"));
        if (dailyPrice != null && dailyPrice.getClosePrice() != null) {
            return dailyPrice.getClosePrice().setScale(6, RoundingMode.HALF_UP);
        }
        AssetPriceCurrent current = assetPriceCurrentMapper.selectById(assetId);
        if (current != null && current.getPrice() != null && current.getPrice().compareTo(BigDecimal.ZERO) > 0
                && current.getQuoteTime() != null && date.equals(current.getQuoteTime().toLocalDate())) {
            return current.getPrice().setScale(6, RoundingMode.HALF_UP);
        }
        return null;
    }

    private boolean isFundAmountBuy(InvestmentTransactionRequest request, Asset asset) {
        return TYPE_BUY.equals(request.getType())
                && ASSET_TYPE_FUND.equals(asset.getType())
                && (INPUT_MODE_AMOUNT_NAV.equals(request.getInputMode()) || request.getTradeAmount() != null);
    }

    private String convertNote(String note, String prefix) {
        if (note == null || note.isBlank()) {
            return prefix;
        }
        return prefix + "：" + note;
    }

    /**
     * 查询当前用户自己的投资交易。
     */
    private InvestmentTransaction findOwnedTransaction(Long id, Long userId) {
        InvestmentTransaction transaction = transactionMapper.selectOne(new LambdaQueryWrapper<InvestmentTransaction>()
                .eq(InvestmentTransaction::getId, id)
                .eq(InvestmentTransaction::getUserId, userId));
        if (transaction == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "投资交易不存在");
        }
        return transaction;
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
     * 投资金额入库前统一四位小数，确保持仓联动和交易记录金额口径一致。
     */
    private BigDecimal scale4(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 基金金额买入的用户输入金额按两位小数保存。
     */
    private BigDecimal scaleMoney2(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 投资数量保留十位小数，避免虚拟货币数量被截断。
     */
    private BigDecimal scaleQuantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(10, RoundingMode.HALF_UP);
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
                .inputMode(transaction.getInputMode())
                .tradeAmount(transaction.getTradeAmount())
                .tradeQuantity(transaction.getTradeQuantity())
                .tradePrice(transaction.getTradePrice())
                .quantity(transaction.getQuantity())
                .price(transaction.getPrice())
                .amount(transaction.getAmount())
                .fee(transaction.getFee())
                .costAmount(transaction.getCostAmount())
                .realizedProfit(transaction.getRealizedProfit())
                .tradeDate(transaction.getTradeDate())
                .confirmedDate(transaction.getConfirmedDate())
                .confirmedNav(transaction.getConfirmedNav())
                .confirmedQuantity(transaction.getConfirmedQuantity())
                .status(transaction.getStatus())
                .revokeTime(transaction.getRevokeTime())
                .revokeReason(transaction.getRevokeReason())
                .transactionTime(transaction.getTransactionTime())
                .note(transaction.getNote())
                .build();
    }
}
