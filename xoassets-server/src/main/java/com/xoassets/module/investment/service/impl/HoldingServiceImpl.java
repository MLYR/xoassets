package com.xoassets.module.investment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.investment.dto.HoldingRequest;
import com.xoassets.module.investment.service.AssetService;
import com.xoassets.module.investment.service.HoldingService;
import com.xoassets.module.investment.service.HoldingTradeResult;
import com.xoassets.module.investment.service.QuoteService;
import com.xoassets.module.investment.vo.AssetPriceVO;
import com.xoassets.module.investment.vo.HoldingChartPointVO;
import com.xoassets.module.investment.vo.HoldingDetailSummaryVO;
import com.xoassets.module.investment.vo.HoldingDetailVO;
import com.xoassets.module.investment.vo.HoldingSummaryVO;
import com.xoassets.module.investment.vo.HoldingVO;
import com.xoassets.module.investment.vo.InvestmentTransactionVO;
import com.xoassets.persistence.entity.Account;
import com.xoassets.persistence.entity.Asset;
import com.xoassets.persistence.entity.AssetPriceDaily;
import com.xoassets.persistence.entity.AssetPrice;
import com.xoassets.persistence.entity.AssetPriceCurrent;
import com.xoassets.persistence.entity.Holding;
import com.xoassets.persistence.entity.InvestmentDailySnapshot;
import com.xoassets.persistence.entity.InvestmentTransaction;
import com.xoassets.persistence.mapper.AccountMapper;
import com.xoassets.persistence.mapper.AssetMapper;
import com.xoassets.persistence.mapper.AssetPriceCurrentMapper;
import com.xoassets.persistence.mapper.AssetPriceDailyMapper;
import com.xoassets.persistence.mapper.AssetPriceMapper;
import com.xoassets.persistence.mapper.HoldingMapper;
import com.xoassets.persistence.mapper.InvestmentDailySnapshotMapper;
import com.xoassets.persistence.mapper.InvestmentTransactionMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户持仓服务实现。
 */
@Service
public class HoldingServiceImpl implements HoldingService {

    private static final List<String> ASSET_TYPES = List.of("STOCK", "FUND", "CRYPTO", "OTHER");
    private static final List<String> QUOTE_SOURCES = List.of("MANUAL", "COINGECKO", "EASTMONEY", "SINA", "YAHOO", "ALPHA_VANTAGE", "TUSHARE", "AKSHARE");
    private static final String ASSET_TYPE_FUND = "FUND";

    private final HoldingMapper holdingMapper;
    private final AssetMapper assetMapper;
    private final AssetPriceMapper assetPriceMapper;
    private final AssetPriceCurrentMapper assetPriceCurrentMapper;
    private final AssetPriceDailyMapper assetPriceDailyMapper;
    private final InvestmentDailySnapshotMapper investmentDailySnapshotMapper;
    private final InvestmentTransactionMapper investmentTransactionMapper;
    private final AccountMapper accountMapper;
    private final AssetService assetService;
    private final QuoteService quoteService;

    public HoldingServiceImpl(
            HoldingMapper holdingMapper,
            AssetMapper assetMapper,
            AssetPriceMapper assetPriceMapper,
            AssetPriceCurrentMapper assetPriceCurrentMapper,
            AssetPriceDailyMapper assetPriceDailyMapper,
            InvestmentDailySnapshotMapper investmentDailySnapshotMapper,
            InvestmentTransactionMapper investmentTransactionMapper,
            AccountMapper accountMapper,
            AssetService assetService,
            QuoteService quoteService) {
        this.holdingMapper = holdingMapper;
        this.assetMapper = assetMapper;
        this.assetPriceMapper = assetPriceMapper;
        this.assetPriceCurrentMapper = assetPriceCurrentMapper;
        this.assetPriceDailyMapper = assetPriceDailyMapper;
        this.investmentDailySnapshotMapper = investmentDailySnapshotMapper;
        this.investmentTransactionMapper = investmentTransactionMapper;
        this.accountMapper = accountMapper;
        this.assetService = assetService;
        this.quoteService = quoteService;
    }

    /**
     * 查询当前用户持仓并补齐价格估值。
     */
    @Override
    public java.util.List<HoldingVO> list() {
        Long userId = LoginUserContext.getUserId();
        java.util.List<Holding> holdings = holdingMapper.selectList(new LambdaQueryWrapper<Holding>()
                .eq(Holding::getUserId, userId)
                .orderByDesc(Holding::getCreatedAt));
        return toVOList(holdings);
    }

    /**
     * 汇总当前用户持仓市值、收益和持仓数量。
     */
    @Override
    public HoldingSummaryVO summary() {
        Long userId = LoginUserContext.getUserId();
        List<HoldingVO> holdings = list();
        BigDecimal totalMarketValue = holdings.stream().map(HoldingVO::getMarketValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = holdings.stream().map(HoldingVO::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        InvestmentDailySnapshot previousSnapshot = previousInvestmentSnapshot(userId, LocalDate.now());
        // 投资总资产较昨日使用用户投资日快照，而不是单持仓今日收益求和，避免用户入金/调仓被误解为价格收益。
        BigDecimal fallbackTodayProfit = holdings.stream().map(item -> nullToZero(item.getTodayProfit())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal fallbackTodayProfitBase = holdings.stream()
                .filter(item -> item.getPreviousPrice() != null)
                .map(item -> item.getQuantity().multiply(item.getPreviousPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayProfit = previousSnapshot == null ? fallbackTodayProfit.setScale(4, RoundingMode.HALF_UP) : totalMarketValue.subtract(scale4(previousSnapshot.getMarketValue())).subtract(todayNetInflow()).setScale(4, RoundingMode.HALF_UP);
        BigDecimal todayProfitRate = previousSnapshot == null
                ? (fallbackTodayProfitBase.compareTo(BigDecimal.ZERO) <= 0 ? null : rate(todayProfit, fallbackTodayProfitBase))
                : rate(todayProfit, scale4(previousSnapshot.getMarketValue()));
        BigDecimal yesterdayProfit = holdings.stream().map(item -> nullToZero(item.getYesterdayProfit())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal yesterdayProfitBase = holdings.stream()
                .filter(item -> item.getBeforePreviousPrice() != null)
                .map(item -> item.getQuantity().multiply(item.getBeforePreviousPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal yesterdayProfitRate = yesterdayProfitBase.compareTo(BigDecimal.ZERO) <= 0 ? null : rate(yesterdayProfit, yesterdayProfitBase);
        BigDecimal floatingProfit = holdings.stream().map(HoldingVO::getFloatingProfit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal floatingProfitRate = totalCost.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : floatingProfit.multiply(BigDecimal.valueOf(100)).divide(totalCost, 4, RoundingMode.HALF_UP);
        InvestmentDailySnapshot lastMonthEndSnapshot = lastMonthEndSnapshot(userId, LocalDate.now());
        // TODO: 后续需要汇总本月投资账户外部转入/转出，避免充值被算成较上月收益。
        BigDecimal monthNetInflow = BigDecimal.ZERO;
        BigDecimal lastMonthProfit = lastMonthEndSnapshot == null ? null : totalMarketValue.subtract(scale4(lastMonthEndSnapshot.getMarketValue())).subtract(monthNetInflow).setScale(4, RoundingMode.HALF_UP);
        BigDecimal lastMonthProfitRate = lastMonthEndSnapshot == null ? null : rate(lastMonthProfit, scale4(lastMonthEndSnapshot.getMarketValue()));
        return HoldingSummaryVO.builder()
                .totalMarketValue(totalMarketValue.setScale(4, RoundingMode.HALF_UP))
                .totalCost(totalCost.setScale(4, RoundingMode.HALF_UP))
                .todayProfit(todayProfit)
                .todayProfitRate(todayProfitRate)
                .yesterdayProfit(yesterdayProfit.setScale(4, RoundingMode.HALF_UP))
                .yesterdayProfitRate(yesterdayProfitRate)
                .lastMonthProfit(lastMonthProfit)
                .lastMonthProfitRate(lastMonthProfitRate)
                .floatingProfit(floatingProfit.setScale(4, RoundingMode.HALF_UP))
                .floatingProfitRate(floatingProfitRate)
                .holdingCount(holdings.size())
                .build();
    }

    /**
     * 查询单个持仓详情，交易明细保留已撤销记录，但汇总只统计正常交易。
     */
    @Override
    public HoldingDetailVO detail(Long id) {
        Long userId = LoginUserContext.getUserId();
        Holding holding = findOwnedHolding(id, userId);
        Asset asset = assetMapper.selectById(holding.getAssetId());
        List<AssetPrice> priceSnapshots = latestPriceSnapshots(holding.getAssetId());
        HoldingVO holdingVO = toVO(holding, asset, priceContext(holding.getAssetId()));
        List<InvestmentTransaction> transactions = investmentTransactionMapper.selectList(new LambdaQueryWrapper<InvestmentTransaction>()
                .eq(InvestmentTransaction::getUserId, userId)
                .eq(InvestmentTransaction::getHoldingId, id)
                .orderByDesc(InvestmentTransaction::getTransactionTime)
                .orderByDesc(InvestmentTransaction::getCreatedAt));
        Map<Long, Account> accountMap = accountMap(transactions);
        List<InvestmentTransactionVO> transactionVOList = transactions.stream()
                .map(transaction -> toTransactionVO(transaction, asset, accountMap.get(transaction.getAccountId())))
                .toList();
        HoldingDetailSummaryVO detailSummary = detailSummary(holdingVO, transactions);
        return HoldingDetailVO.builder()
                .holding(holdingVO)
                .summary(detailSummary)
                .transactions(transactionVOList)
                .priceSnapshots(priceSnapshots.stream().map(this::toAssetPriceVO).toList())
                .chartPoints(holdingChartPoints(holdingVO, detailSummary, priceSnapshots))
                .build();
    }

    /**
     * 手动新增持仓，总成本按数量乘以平均成本初始化。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public HoldingVO create(HoldingRequest request) {
        Long userId = LoginUserContext.getUserId();
        Asset asset = resolveAsset(request);
        BigDecimal quantity = scaleQuantity(request.getQuantity());
        BigDecimal avgCost = scale4(request.getAvgCost());
        ensureInitialQuantity(request.getAssetType(), quantity);
        ensureNoDuplicatedHolding(userId, asset.getId(), null);
        Holding holding = new Holding();
        holding.setUserId(userId);
        holding.setAssetId(asset.getId());
        holding.setQuantity(quantity);
        holding.setAvgCost(avgCost);
        holding.setTotalCost(quantity.multiply(avgCost).setScale(4, RoundingMode.HALF_UP));
        holding.setRemark(request.getRemark());
        holding.setStatus(1);
        holding.setDeleted(0);
        holdingMapper.insert(holding);
        saveLookupPriceSnapshot(request, asset);
        return toVO(holding);
    }

    /**
     * 修改当前用户自己的持仓基础信息。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public HoldingVO update(Long id, HoldingRequest request) {
        Long userId = LoginUserContext.getUserId();
        Asset asset = resolveAsset(request);
        Holding holding = findOwnedHolding(id, userId);
        BigDecimal quantity = scaleQuantity(request.getQuantity());
        BigDecimal avgCost = scale4(request.getAvgCost());
        ensureInitialQuantity(request.getAssetType(), quantity);
        ensureNoDuplicatedHolding(userId, asset.getId(), id);
        holding.setAssetId(asset.getId());
        holding.setQuantity(quantity);
        holding.setAvgCost(avgCost);
        holding.setTotalCost(quantity.multiply(avgCost).setScale(4, RoundingMode.HALF_UP));
        holding.setRemark(request.getRemark());
        holdingMapper.update(holding, new LambdaUpdateWrapper<Holding>()
                .eq(Holding::getId, id)
                .eq(Holding::getUserId, userId));
        return toVO(holding);
    }

    /**
     * 删除持仓前检查是否已有投资交易，避免历史交易失去持仓归属。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long id) {
        Long userId = LoginUserContext.getUserId();
        findOwnedHolding(id, userId);
        Long transactionCount = investmentTransactionMapper.selectCount(new LambdaQueryWrapper<InvestmentTransaction>()
                .eq(InvestmentTransaction::getUserId, userId)
                .eq(InvestmentTransaction::getHoldingId, id));
        if (transactionCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "持仓已有投资交易，第一版不允许删除");
        }
        holdingMapper.delete(new LambdaQueryWrapper<Holding>().eq(Holding::getId, id).eq(Holding::getUserId, userId));
    }

    /**
     * 查询当前用户持仓，不存在时返回业务错误。
     */
    @Override
    public Holding findOwnedHolding(Long id, Long userId) {
        Holding holding = holdingMapper.selectOne(new LambdaQueryWrapper<Holding>()
                .eq(Holding::getId, id)
                .eq(Holding::getUserId, userId));
        if (holding == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "持仓不存在");
        }
        return holding;
    }

    /**
     * 买入时使用移动平均成本法更新数量、总成本和平均成本。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public HoldingTradeResult applyBuy(Long userId, Long holdingId, Long assetId, BigDecimal quantity, BigDecimal price, BigDecimal fee) {
        quantity = scaleQuantity(quantity);
        price = scale4(price);
        fee = scale4(fee);
        Holding holding = holdingId == null ? findOrCreateHolding(userId, assetId) : findOwnedHolding(holdingId, userId);
        if (!Objects.equals(holding.getAssetId(), assetId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "交易资产与持仓资产不一致");
        }
        BigDecimal buyCost = quantity.multiply(price).add(fee).setScale(4, RoundingMode.HALF_UP);
        BigDecimal newQuantity = holding.getQuantity().add(quantity).setScale(10, RoundingMode.HALF_UP);
        BigDecimal newTotalCost = holding.getTotalCost().add(buyCost);
        holding.setQuantity(newQuantity);
        holding.setTotalCost(newTotalCost);
        holding.setAvgCost(newQuantity.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : newTotalCost.divide(newQuantity, 4, RoundingMode.HALF_UP));
        updateHoldingBalance(holding);
        return new HoldingTradeResult(holding, null, null);
    }

    /**
     * 基金金额买入确认后使用确认份额和用户实际投入金额更新持仓，避免份额向下取整后成本被反推变小。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public HoldingTradeResult applyConfirmedBuy(Long userId, Long holdingId, Long assetId, BigDecimal quantity, BigDecimal costAmount) {
        quantity = quantity == null ? BigDecimal.ZERO : quantity.setScale(4, RoundingMode.DOWN);
        costAmount = costAmount == null ? BigDecimal.ZERO : costAmount.setScale(4, RoundingMode.HALF_UP);
        Holding holding = holdingId == null ? findOrCreateHolding(userId, assetId) : findOwnedHolding(holdingId, userId);
        if (!Objects.equals(holding.getAssetId(), assetId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "交易资产与持仓资产不一致");
        }
        BigDecimal newQuantity = holding.getQuantity().add(quantity).setScale(10, RoundingMode.HALF_UP);
        BigDecimal newTotalCost = holding.getTotalCost().add(costAmount).setScale(4, RoundingMode.HALF_UP);
        holding.setQuantity(newQuantity);
        holding.setTotalCost(newTotalCost);
        holding.setAvgCost(newQuantity.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : newTotalCost.divide(newQuantity, 4, RoundingMode.HALF_UP));
        updateHoldingBalance(holding);
        return new HoldingTradeResult(holding, null, null);
    }

    /**
     * 卖出时校验持仓数量不能不足，并按当前平均成本减少总成本。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public HoldingTradeResult applySell(Long userId, Long holdingId, Long assetId, BigDecimal quantity, BigDecimal price, BigDecimal fee) {
        quantity = scaleQuantity(quantity);
        price = scale4(price);
        fee = scale4(fee);
        Holding holding = holdingId == null ? findHoldingByAsset(userId, assetId) : findOwnedHolding(holdingId, userId);
        if (!Objects.equals(holding.getAssetId(), assetId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "交易资产与持仓资产不一致");
        }
        if (holding.getQuantity().compareTo(quantity) < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "持仓数量不足");
        }
        BigDecimal sellCost = holding.getAvgCost().multiply(quantity).setScale(4, RoundingMode.HALF_UP);
        BigDecimal realizedProfit = quantity.multiply(price).subtract(fee).subtract(sellCost).setScale(4, RoundingMode.HALF_UP);
        BigDecimal newQuantity = holding.getQuantity().subtract(quantity).setScale(10, RoundingMode.HALF_UP);
        BigDecimal newTotalCost = holding.getTotalCost().subtract(sellCost).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
        holding.setQuantity(newQuantity);
        holding.setTotalCost(newQuantity.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : newTotalCost);
        holding.setAvgCost(newQuantity.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : holding.getTotalCost().divide(newQuantity, 4, RoundingMode.HALF_UP));
        updateHoldingBalance(holding);
        return new HoldingTradeResult(holding, sellCost, realizedProfit);
    }

    /**
     * 撤销买入时按原交易成本反向减少持仓；若数量不足说明后续交易已改变持仓，直接拒绝撤销。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void revokeBuy(Long userId, Long holdingId, Long assetId, BigDecimal quantity, BigDecimal costAmount) {
        Holding holding = findOwnedHolding(holdingId, userId);
        if (!Objects.equals(holding.getAssetId(), assetId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "交易资产与持仓资产不一致");
        }
        quantity = scaleQuantity(quantity);
        costAmount = scale4(costAmount);
        if (holding.getQuantity().compareTo(quantity) < 0 || holding.getTotalCost().compareTo(costAmount) < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "持仓不足，无法撤销该买入交易");
        }
        BigDecimal newQuantity = holding.getQuantity().subtract(quantity).setScale(10, RoundingMode.HALF_UP);
        BigDecimal newTotalCost = holding.getTotalCost().subtract(costAmount).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
        holding.setQuantity(newQuantity);
        holding.setTotalCost(newQuantity.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : newTotalCost);
        holding.setAvgCost(newQuantity.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : holding.getTotalCost().divide(newQuantity, 4, RoundingMode.HALF_UP));
        updateHoldingBalance(holding);
    }

    /**
     * 撤销卖出时按原 sellCost 恢复数量和总成本，避免用当前价格重新推导历史成本。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void revokeSell(Long userId, Long holdingId, Long assetId, BigDecimal quantity, BigDecimal costAmount) {
        Holding holding = findOwnedHolding(holdingId, userId);
        if (!Objects.equals(holding.getAssetId(), assetId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "交易资产与持仓资产不一致");
        }
        quantity = scaleQuantity(quantity);
        costAmount = scale4(costAmount);
        BigDecimal newQuantity = holding.getQuantity().add(quantity).setScale(10, RoundingMode.HALF_UP);
        BigDecimal newTotalCost = holding.getTotalCost().add(costAmount).setScale(4, RoundingMode.HALF_UP);
        holding.setQuantity(newQuantity);
        holding.setTotalCost(newTotalCost);
        holding.setAvgCost(newQuantity.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : newTotalCost.divide(newQuantity, 4, RoundingMode.HALF_UP));
        updateHoldingBalance(holding);
    }

    /**
     * 查询或创建用户某资产持仓，供买入交易自动建仓。
     */
    private Holding findOrCreateHolding(Long userId, Long assetId) {
        Holding holding = holdingMapper.selectOne(new LambdaQueryWrapper<Holding>()
                .eq(Holding::getUserId, userId)
                .eq(Holding::getAssetId, assetId));
        if (holding != null) {
            return holding;
        }
        assetService.findAsset(assetId);
        Holding created = new Holding();
        created.setUserId(userId);
        created.setAssetId(assetId);
        created.setQuantity(BigDecimal.ZERO);
        created.setAvgCost(BigDecimal.ZERO);
        created.setTotalCost(BigDecimal.ZERO);
        created.setStatus(1);
        created.setDeleted(0);
        holdingMapper.insert(created);
        return created;
    }

    /**
     * 按资产查询当前用户持仓，卖出时必须存在。
     */
    private Holding findHoldingByAsset(Long userId, Long assetId) {
        Holding holding = holdingMapper.selectOne(new LambdaQueryWrapper<Holding>()
                .eq(Holding::getUserId, userId)
                .eq(Holding::getAssetId, assetId));
        if (holding == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "持仓不存在");
        }
        return holding;
    }

    /**
     * 更新持仓数量和成本，按 user_id 限定写入范围。
     */
    private void updateHoldingBalance(Holding holding) {
        holdingMapper.update(holding, new LambdaUpdateWrapper<Holding>()
                .eq(Holding::getId, holding.getId())
                .eq(Holding::getUserId, holding.getUserId()));
    }

    /**
     * 投资金额统一按四位小数计算，保持成本、市值和盈亏口径稳定。
     */
    private BigDecimal scale4(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 投资页较本月初收益率按月初投资资产做分母，缺少月初快照时由调用方返回空值。
     */
    private BigDecimal rate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return scale4(numerator).multiply(BigDecimal.valueOf(100)).divide(denominator, 4, RoundingMode.HALF_UP);
    }

    /**
     * 查询上月最后一条投资日快照，作为投资总资产较上月的权威对比基准。
     */
    private InvestmentDailySnapshot lastMonthEndSnapshot(Long userId, LocalDate date) {
        LocalDate monthStart = date.withDayOfMonth(1);
        return investmentDailySnapshotMapper.selectList(new LambdaQueryWrapper<InvestmentDailySnapshot>()
                        .eq(InvestmentDailySnapshot::getUserId, userId)
                        .lt(InvestmentDailySnapshot::getSnapshotDate, monthStart)
                        .orderByDesc(InvestmentDailySnapshot::getSnapshotDate)
                        .last("limit 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * 查询今天之前最近一条投资日快照，作为投资总资产较昨日的权威对比基准。
     */
    private InvestmentDailySnapshot previousInvestmentSnapshot(Long userId, LocalDate date) {
        return investmentDailySnapshotMapper.selectList(new LambdaQueryWrapper<InvestmentDailySnapshot>()
                        .eq(InvestmentDailySnapshot::getUserId, userId)
                        .lt(InvestmentDailySnapshot::getSnapshotDate, date)
                        .orderByDesc(InvestmentDailySnapshot::getSnapshotDate)
                        .last("limit 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * 第一版暂未区分投资账户外部入金/出金；先固定为 0，并在日快照任务中保留同一 TODO。
     */
    private BigDecimal todayNetInflow() {
        // TODO: 后续需要区分投资账户外部转入/转出，避免充值被算成较昨日收益。
        return BigDecimal.ZERO;
    }

    /**
     * 投资数量保留十位小数，满足虚拟货币小额持仓记录需求。
     */
    private BigDecimal scaleQuantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(10, RoundingMode.HALF_UP);
    }

    /**
     * 基金允许先建 0 份额持仓再通过金额买入确认份额，股票和虚拟货币仍要求初始数量大于 0。
     */
    private void ensureInitialQuantity(String assetType, BigDecimal quantity) {
        if (!ASSET_TYPE_FUND.equals(assetType) && quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非基金持仓数量必须大于0");
        }
    }

    /**
     * 同一用户同一资产只保留一条持仓。
     */
    private void ensureNoDuplicatedHolding(Long userId, Long assetId, Long excludeId) {
        LambdaQueryWrapper<Holding> wrapper = new LambdaQueryWrapper<Holding>()
                .eq(Holding::getUserId, userId)
                .eq(Holding::getAssetId, assetId);
        if (excludeId != null) {
            wrapper.ne(Holding::getId, excludeId);
        }
        if (holdingMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该资产已存在持仓");
        }
    }

    /**
     * 批量转换持仓列表。
     */
    private java.util.List<HoldingVO> toVOList(java.util.List<Holding> holdings) {
        if (holdings.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> assetIds = holdings.stream().map(Holding::getAssetId).collect(Collectors.toSet());
        Map<Long, Asset> assetMap = assetMapper.selectBatchIds(assetIds).stream().collect(Collectors.toMap(Asset::getId, asset -> asset));
        Map<Long, HoldingPriceContext> priceContextMap = priceContextMap(assetIds);
        return holdings.stream().map(holding -> toVO(holding, assetMap.get(holding.getAssetId()), priceContextMap.get(holding.getAssetId()))).toList();
    }

    /**
     * 转换单条持仓。
     */
    private HoldingVO toVO(Holding holding) {
        Asset asset = assetMapper.selectById(holding.getAssetId());
        return toVO(holding, asset, priceContext(holding.getAssetId()));
    }

    /**
     * 计算市值、浮动盈亏和收益率；没有价格时用 avgCost 兜底。
     */
    private HoldingVO toVO(Holding holding, Asset asset, HoldingPriceContext priceContext) {
        AssetPrice matchedPrice = priceContext == null || !priceMatchesAssetCurrency(asset, priceContext.currentPrice()) ? null : priceContext.currentPrice();
        AssetPriceDaily previousDaily = priceContext == null || !priceMatchesAssetCurrency(asset, priceContext.previousDaily()) ? null : priceContext.previousDaily();
        AssetPriceDaily beforePreviousDaily = priceContext == null || !priceMatchesAssetCurrency(asset, priceContext.beforePreviousDaily()) ? null : priceContext.beforePreviousDaily();
        AssetPrice previousAudit = priceContext == null || !priceMatchesAssetCurrency(asset, priceContext.previousAudit()) ? null : priceContext.previousAudit();
        AssetPrice beforePreviousAudit = priceContext == null || !priceMatchesAssetCurrency(asset, priceContext.beforePreviousAudit()) ? null : priceContext.beforePreviousAudit();
        BigDecimal latestPrice = matchedPrice == null ? holding.getAvgCost() : matchedPrice.getPrice();
        // 今日收益按“当前价 - 最近交易日日收盘价”计算，不能用自然日 yesterday 推断交易日。
        BigDecimal previous = matchedPrice != null && matchedPrice.getPreviousClose() != null
                ? matchedPrice.getPreviousClose()
                : previousDaily != null ? previousDaily.getClosePrice() : previousAudit == null ? null : previousAudit.getPrice();
        BigDecimal yesterdayPrevious = yesterdayPreviousPrice(previousDaily, beforePreviousDaily, previousAudit, beforePreviousAudit);
        BigDecimal beforePrevious = yesterdayBeforePreviousPrice(previousDaily, beforePreviousDaily, previousAudit, beforePreviousAudit);
        BigDecimal marketValue = holding.getQuantity().multiply(latestPrice).setScale(4, RoundingMode.HALF_UP);
        BigDecimal profit = marketValue.subtract(holding.getTotalCost());
        BigDecimal profitRate = holding.getTotalCost().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : profit.multiply(BigDecimal.valueOf(100)).divide(holding.getTotalCost(), 4, RoundingMode.HALF_UP);
        BigDecimal todayProfit = priceDiffProfit(holding.getQuantity(), latestPrice, previous);
        BigDecimal todayChangeRate = changeRate(latestPrice, previous);
        BigDecimal yesterdayProfit = priceDiffProfit(holding.getQuantity(), yesterdayPrevious, beforePrevious);
        BigDecimal yesterdayChangeRate = changeRate(yesterdayPrevious, beforePrevious);
        BigDecimal breakEvenRate = matchedPrice == null ? null : breakEvenRate(holding.getAvgCost(), latestPrice);
        return HoldingVO.builder()
                .id(holding.getId())
                .assetId(holding.getAssetId())
                .assetName(asset == null ? null : asset.getName())
                .symbol(asset == null ? null : asset.getSymbol())
                .assetType(asset == null ? null : asset.getType())
                .market(asset == null ? null : asset.getMarket())
                .quoteSource(asset == null ? null : asset.getQuoteSource())
                .currency(asset == null ? null : asset.getCurrency())
                .quantity(holding.getQuantity())
                .avgCost(holding.getAvgCost())
                .totalCost(holding.getTotalCost())
                .latestPrice(latestPrice)
                .previousPrice(previous)
                .beforePreviousPrice(beforePrevious)
                .priceScale(priceScale(asset))
                .latestPriceTime(matchedPrice == null ? null : matchedPrice.getQuoteTime())
                .previousPriceTime(previousDaily != null ? previousDaily.getTradeDate().atStartOfDay() : previousAudit == null ? null : previousAudit.getQuoteTime())
                .latestPriceSource(matchedPrice == null ? null : matchedPrice.getSource())
                .marketStatus(matchedPrice == null ? null : matchedPrice.getMarketStatus())
                .marketValue(marketValue)
                .todayProfit(todayProfit)
                .todayChangeRate(todayChangeRate)
                .yesterdayProfit(yesterdayProfit)
                .yesterdayChangeRate(yesterdayChangeRate)
                .floatingProfit(profit)
                .floatingProfitRate(profitRate)
                .breakEvenRate(breakEvenRate)
                .remark(holding.getRemark())
                .status(holding.getStatus())
                .build();
    }

    /**
     * 查询详情页最近 30 条价格快照；详情页只展示趋势，不在前端重新计算权威收益。
     */
    private List<AssetPrice> latestPriceSnapshots(Long assetId) {
        return assetPriceMapper.selectList(new LambdaQueryWrapper<AssetPrice>()
                .eq(AssetPrice::getAssetId, assetId)
                .orderByDesc(AssetPrice::getQuoteTime)
                .orderByDesc(AssetPrice::getCreatedAt)
                .last("limit 30"));
    }

    /**
     * 批量读取资金账户名称，交易记录展示用；账户归属已由交易 user_id 限定。
     */
    private Map<Long, Account> accountMap(List<InvestmentTransaction> transactions) {
        Set<Long> accountIds = transactions.stream().map(InvestmentTransaction::getAccountId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (accountIds.isEmpty()) {
            return Map.of();
        }
        return accountMapper.selectBatchIds(accountIds).stream().collect(Collectors.toMap(Account::getId, account -> account));
    }

    /**
     * 生成持仓详情金额曲线：总资产和总收益都由后端基于真实价格快照、当前份额和成本口径计算。
     */
    private List<HoldingChartPointVO> holdingChartPoints(HoldingVO holding, HoldingDetailSummaryVO summary, List<AssetPrice> priceSnapshots) {
        List<AssetPrice> points = priceSnapshots == null ? List.of() : priceSnapshots.stream()
                .filter(price -> price.getPrice() != null)
                .sorted(java.util.Comparator.comparing(AssetPrice::getQuoteTime))
                .toList();
        if (points.isEmpty()) {
            BigDecimal assetAmount = scale4(holding.getMarketValue());
            return List.of(HoldingChartPointVO.builder()
                    .quoteTime(holding.getLatestPriceTime() == null ? java.time.LocalDateTime.now() : holding.getLatestPriceTime())
                    .totalAssetAmount(assetAmount)
                    .totalProfitAmount(scale4(summary.getTotalProfit()))
                    .build());
        }
        BigDecimal quantity = nullToZero(holding.getQuantity());
        BigDecimal totalCost = scale4(holding.getTotalCost());
        BigDecimal realizedProfit = scale4(summary.getRealizedProfit());
        return points.stream()
                .map(price -> {
                    BigDecimal assetAmount = quantity.multiply(price.getPrice()).setScale(4, RoundingMode.HALF_UP);
                    BigDecimal profitAmount = realizedProfit.add(assetAmount.subtract(totalCost)).setScale(4, RoundingMode.HALF_UP);
                    return HoldingChartPointVO.builder()
                            .quoteTime(price.getQuoteTime())
                            .totalAssetAmount(assetAmount)
                            .totalProfitAmount(profitAmount)
                            .build();
                })
                .toList();
    }

    /**
     * 计算单个持仓详情汇总，撤销交易保留明细展示但不参与收益统计。
     */
    private HoldingDetailSummaryVO detailSummary(HoldingVO holding, List<InvestmentTransaction> transactions) {
        BigDecimal totalBuyAmount = BigDecimal.ZERO;
        BigDecimal totalSellAmount = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal pendingConfirmAmount = BigDecimal.ZERO;
        BigDecimal realizedProfit = BigDecimal.ZERO;
        int buyCount = 0;
        int sellCount = 0;
        java.time.LocalDateTime firstBuyDateTime = null;
        java.time.LocalDateTime lastTradeTime = null;
        for (InvestmentTransaction transaction : transactions) {
            if ("PENDING_CONFIRM".equals(transaction.getStatus()) && "BUY".equals(transaction.getType())) {
                pendingConfirmAmount = pendingConfirmAmount.add(scale4(transaction.getAmount()).add(scale4(transaction.getFee())));
            }
            if (!isEffectiveInvestmentTransaction(transaction)) {
                continue;
            }
            BigDecimal fee = scale4(transaction.getFee());
            totalFee = totalFee.add(fee);
            if ("BUY".equals(transaction.getType())) {
                totalBuyAmount = totalBuyAmount.add(scale4(transaction.getAmount()).add(fee));
                buyCount++;
                if (transaction.getTransactionTime() != null && (firstBuyDateTime == null || transaction.getTransactionTime().isBefore(firstBuyDateTime))) {
                    firstBuyDateTime = transaction.getTransactionTime();
                }
            }
            if ("SELL".equals(transaction.getType())) {
                totalSellAmount = totalSellAmount.add(scale4(transaction.getAmount()).subtract(fee));
                realizedProfit = realizedProfit.add(scale4(transaction.getRealizedProfit()));
                sellCount++;
            }
            if (transaction.getTransactionTime() != null && (lastTradeTime == null || transaction.getTransactionTime().isAfter(lastTradeTime))) {
                lastTradeTime = transaction.getTransactionTime();
            }
        }
        BigDecimal floatingProfit = scale4(holding.getFloatingProfit());
        BigDecimal totalProfit = realizedProfit.add(floatingProfit).setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalProfitRate = totalBuyAmount.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ZERO
                : totalProfit.multiply(BigDecimal.valueOf(100)).divide(totalBuyAmount, 4, RoundingMode.HALF_UP);
        return HoldingDetailSummaryVO.builder()
                .totalBuyAmount(totalBuyAmount.setScale(4, RoundingMode.HALF_UP))
                .totalSellAmount(totalSellAmount.setScale(4, RoundingMode.HALF_UP))
                .totalFee(totalFee.setScale(4, RoundingMode.HALF_UP))
                .pendingConfirmAmount(pendingConfirmAmount.setScale(4, RoundingMode.HALF_UP))
                .realizedProfit(realizedProfit.setScale(4, RoundingMode.HALF_UP))
                .floatingProfit(floatingProfit)
                .totalProfit(totalProfit)
                .totalProfitRate(totalProfitRate)
                .buyCount(buyCount)
                .sellCount(sellCount)
                .firstBuyTime(firstBuyDateTime)
                .lastTradeTime(lastTradeTime)
                .build();
    }

    /**
     * 只有正常已确认交易参与持仓详情汇总，待确认、已撤销和已取消都只保留明细展示。
     */
    private boolean isEffectiveInvestmentTransaction(InvestmentTransaction transaction) {
        return "NORMAL".equals(transaction.getStatus()) || "CONFIRMED".equals(transaction.getStatus());
    }

    /**
     * 转换详情页投资交易记录，保持和投资交易列表相同字段。
     */
    private InvestmentTransactionVO toTransactionVO(InvestmentTransaction transaction, Asset asset, Account account) {
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

    /**
     * 转换价格快照供前端绘制轻量价格趋势。
     */
    private AssetPriceVO toAssetPriceVO(AssetPrice price) {
        return AssetPriceVO.builder()
                .id(price.getId())
                .assetId(price.getAssetId())
                .price(price.getPrice())
                .currency(price.getCurrency())
                .previousClose(price.getPreviousClose())
                .changeAmount(price.getChangeAmount())
                .changePercent(price.getChangePercent())
                .source(price.getSource())
                .quoteTime(price.getQuoteTime())
                .marketStatus(price.getMarketStatus())
                .build();
    }

    /**
     * 批量读取 current + 最近两个交易日 daily，核心收益计算不再依赖旧原始快照表。
     */
    private Map<Long, HoldingPriceContext> priceContextMap(Set<Long> assetIds) {
        if (assetIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, AssetPrice> currentPriceMap = quoteService.latestPriceMap(assetIds);
        Map<Long, AssetPrice> safeCurrentPriceMap = currentPriceMap == null ? Map.of() : currentPriceMap;
        List<AssetPriceDaily> dailyRows = assetPriceDailyMapper.selectList(new LambdaQueryWrapper<AssetPriceDaily>()
                        .in(AssetPriceDaily::getAssetId, assetIds)
                        .lt(AssetPriceDaily::getTradeDate, LocalDate.now())
                        .orderByDesc(AssetPriceDaily::getTradeDate));
        Map<Long, List<AssetPriceDaily>> dailyPriceMap = (dailyRows == null ? List.<AssetPriceDaily>of() : dailyRows)
                .stream()
                .collect(Collectors.groupingBy(AssetPriceDaily::getAssetId));
        Map<Long, List<AssetPrice>> auditPriceMap = auditPriceHistoryMap(assetIds);
        return assetIds.stream().collect(Collectors.toMap(assetId -> assetId, assetId -> {
            List<AssetPriceDaily> prices = dailyPriceMap.getOrDefault(assetId, List.of());
            List<AssetPrice> auditPrices = auditPriceMap.getOrDefault(assetId, List.of());
            return new HoldingPriceContext(
                    safeCurrentPriceMap.get(assetId),
                    prices.isEmpty() ? null : prices.get(0),
                    prices.size() < 2 ? null : prices.get(1),
                    auditPrices.isEmpty() ? null : auditPrices.get(0),
                    auditPrices.size() < 2 ? null : auditPrices.get(1));
        }));
    }

    /**
     * daily 迁移数据不足时，用旧审计快照按不同报价日兜底，避免历史真实价格存在但昨日收益被展示为 0。
     */
    private Map<Long, List<AssetPrice>> auditPriceHistoryMap(Set<Long> assetIds) {
        List<AssetPrice> rows = assetPriceMapper.selectList(new LambdaQueryWrapper<AssetPrice>()
                .in(AssetPrice::getAssetId, assetIds)
                .lt(AssetPrice::getQuoteTime, LocalDate.now().atStartOfDay())
                .orderByDesc(AssetPrice::getQuoteTime)
                .orderByDesc(AssetPrice::getCreatedAt));
        return (rows == null ? List.<AssetPrice>of() : rows)
                .stream()
                .collect(Collectors.groupingBy(AssetPrice::getAssetId))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> distinctDailyAuditPrices(entry.getValue())));
    }

    private List<AssetPrice> distinctDailyAuditPrices(List<AssetPrice> prices) {
        Map<LocalDate, AssetPrice> byDate = new LinkedHashMap<>();
        for (AssetPrice price : prices) {
            if (price.getQuoteTime() == null) {
                continue;
            }
            byDate.putIfAbsent(price.getQuoteTime().toLocalDate(), price);
            if (byDate.size() >= 2) {
                break;
            }
        }
        return new ArrayList<>(byDate.values());
    }

    /**
     * 读取单个资产的价格上下文。
     */
    private HoldingPriceContext priceContext(Long assetId) {
        return priceContextMap(Set.of(assetId)).get(assetId);
    }

    /**
     * 若迁移期 daily 被 current 反向兜底成连续相同价格，优先使用旧审计快照恢复昨日收益口径。
     */
    private BigDecimal yesterdayPreviousPrice(AssetPriceDaily previousDaily, AssetPriceDaily beforePreviousDaily, AssetPrice previousAudit, AssetPrice beforePreviousAudit) {
        if (isFlatDailyWithUsefulAudit(previousDaily, beforePreviousDaily, previousAudit, beforePreviousAudit)) {
            return previousAudit.getPrice();
        }
        return previousDaily != null ? previousDaily.getClosePrice() : previousAudit == null ? null : previousAudit.getPrice();
    }

    private BigDecimal yesterdayBeforePreviousPrice(AssetPriceDaily previousDaily, AssetPriceDaily beforePreviousDaily, AssetPrice previousAudit, AssetPrice beforePreviousAudit) {
        if (isFlatDailyWithUsefulAudit(previousDaily, beforePreviousDaily, previousAudit, beforePreviousAudit)) {
            return beforePreviousAudit.getPrice();
        }
        return beforePreviousDaily != null ? beforePreviousDaily.getClosePrice() : beforePreviousAudit == null ? null : beforePreviousAudit.getPrice();
    }

    private boolean isFlatDailyWithUsefulAudit(AssetPriceDaily previousDaily, AssetPriceDaily beforePreviousDaily, AssetPrice previousAudit, AssetPrice beforePreviousAudit) {
        return previousDaily != null
                && beforePreviousDaily != null
                && previousDaily.getClosePrice() != null
                && beforePreviousDaily.getClosePrice() != null
                && previousDaily.getClosePrice().compareTo(beforePreviousDaily.getClosePrice()) == 0
                && previousAudit != null
                && beforePreviousAudit != null
                && previousAudit.getPrice() != null
                && beforePreviousAudit.getPrice() != null
                && previousAudit.getPrice().compareTo(beforePreviousAudit.getPrice()) != 0;
    }

    /**
     * 价格差收益，缺少历史价格时返回 null，前端展示暂无。
     */
    private BigDecimal priceDiffProfit(BigDecimal quantity, BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null) {
            return null;
        }
        return quantity.multiply(current.subtract(previous)).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 涨跌幅百分比，历史价格缺失或非正时返回 null。
     */
    private BigDecimal changeRate(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null || previous.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return current.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous, 4, RoundingMode.HALF_UP);
    }

    /**
     * 亏损时计算回本所需涨幅，盈利或打平时返回 0。
     */
    private BigDecimal breakEvenRate(BigDecimal avgCost, BigDecimal latestPrice) {
        if (latestPrice == null || latestPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        if (latestPrice.compareTo(avgCost) >= 0) {
            return BigDecimal.ZERO;
        }
        return avgCost.subtract(latestPrice).multiply(BigDecimal.valueOf(100)).divide(latestPrice, 4, RoundingMode.HALF_UP);
    }

    /**
     * 汇总收益时缺少历史价格的收益按 0 处理。
     */
    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 前端只暴露“持仓”表单；这里把资产信息解析为内部公共资产，兼容原 assetId 调用。
     */
    private Asset resolveAsset(HoldingRequest request) {
        if (request.getAssetId() != null) {
            return assetService.findAsset(request.getAssetId());
        }
        ensureAssetFields(request);
        String symbol = request.getSymbol().trim().toUpperCase();
        String type = request.getAssetType();
        Asset exists = assetMapper.selectOne(new LambdaQueryWrapper<Asset>()
                .eq(Asset::getType, type)
                .eq(Asset::getMarket, normalizeMarket(type, request.getMarket(), symbol))
                .eq(Asset::getSymbol, symbol)
                .last("LIMIT 1"));
        if (exists != null) {
            return exists;
        }
        Asset asset = new Asset();
        asset.setSymbol(symbol);
        asset.setName(request.getAssetName().trim());
        asset.setType(type);
        asset.setMarket(normalizeMarket(type, request.getMarket(), symbol));
        asset.setCurrency(request.getCurrency());
        asset.setQuoteSource(request.getQuoteSource());
        asset.setQuoteKey(StringUtils.hasText(request.getQuoteKey()) ? request.getQuoteKey().trim() : symbol);
        asset.setStatus(1);
        asset.setDeleted(0);
        assetMapper.insert(asset);
        return asset;
    }

    /**
     * 资产识别结果带回的当前价在新增持仓时落入价格快照，后续估值统一从 xo_asset_price 读取。
     */
    private void saveLookupPriceSnapshot(HoldingRequest request, Asset asset) {
        if (request.getLatestPrice() == null || request.getLatestPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        AssetPrice price = new AssetPrice();
        price.setAssetId(asset.getId());
        price.setPrice(request.getLatestPrice().setScale(8, RoundingMode.HALF_UP));
        price.setCurrency(StringUtils.hasText(request.getCurrency()) ? request.getCurrency() : asset.getCurrency());
        price.setPreviousClose(request.getPreviousClose() == null ? null : request.getPreviousClose().setScale(8, RoundingMode.HALF_UP));
        if (request.getPreviousClose() != null) {
            price.setChangeAmount(request.getLatestPrice().subtract(request.getPreviousClose()).setScale(8, RoundingMode.HALF_UP));
        }
        price.setChangePercent(request.getChangePercent() == null ? null : request.getChangePercent().setScale(4, RoundingMode.HALF_UP));
        price.setSource(StringUtils.hasText(request.getQuoteSource()) ? request.getQuoteSource() : "MANUAL");
        price.setQuoteTime(request.getQuoteTime() == null ? java.time.LocalDateTime.now() : request.getQuoteTime());
        price.setMarketStatus(StringUtils.hasText(request.getMarketStatus()) ? request.getMarketStatus() : "LOOKUP");
        price.setDeleted(0);
        assetPriceMapper.insert(price);
        upsertCurrentPrice(price);
    }

    /**
     * 自动识别带回的初始价同步写 current，保证新增持仓后估值优先使用当前价表。
     */
    private void upsertCurrentPrice(AssetPrice price) {
        AssetPriceCurrent current = new AssetPriceCurrent();
        current.setAssetId(price.getAssetId());
        current.setPrice(price.getPrice());
        current.setCurrency(price.getCurrency());
        current.setPreviousClose(price.getPreviousClose());
        current.setChangeAmount(price.getChangeAmount());
        current.setChangePercent(price.getChangePercent());
        current.setSource(price.getSource());
        current.setQuoteTime(price.getQuoteTime());
        current.setMarketStatus(price.getMarketStatus());
        current.setRawJson(price.getRawJson());
        if (assetPriceCurrentMapper.selectById(price.getAssetId()) == null) {
            assetPriceCurrentMapper.insert(current);
            return;
        }
        assetPriceCurrentMapper.updateById(current);
    }

    /**
     * 虚拟货币当前价至少展示六位小数，股票和基金保留四位小数。
     */
    private Integer priceScale(Asset asset) {
        return asset != null && "CRYPTO".equals(asset.getType()) ? 6 : 4;
    }

    /**
     * 价格快照币种必须与资产币种一致，否则第一版不做跨币种估值换算。
     */
    private boolean priceMatchesAssetCurrency(Asset asset, AssetPrice price) {
        if (asset == null || price == null) {
            return false;
        }
        return Objects.equals(asset.getCurrency(), price.getCurrency());
    }

    private boolean priceMatchesAssetCurrency(Asset asset, AssetPriceDaily price) {
        if (asset == null || price == null) {
            return false;
        }
        return Objects.equals(asset.getCurrency(), price.getCurrency());
    }

    private record HoldingPriceContext(AssetPrice currentPrice, AssetPriceDaily previousDaily, AssetPriceDaily beforePreviousDaily, AssetPrice previousAudit, AssetPrice beforePreviousAudit) {
    }

    /**
     * 市场字段用于区分同代码不同市场资产；手动录入时按类型给出低认知默认值。
     */
    private String normalizeMarket(String type, String market, String symbol) {
        if (StringUtils.hasText(market)) {
            return market.trim().toUpperCase();
        }
        if ("CRYPTO".equals(type)) {
            return "CRYPTO";
        }
        if ("FUND".equals(type)) {
            return "CN_FUND";
        }
        if ("STOCK".equals(type) && StringUtils.hasText(symbol)) {
            String normalized = symbol.trim().toUpperCase();
            if (normalized.endsWith(".SH")) return "SH";
            if (normalized.endsWith(".SZ")) return "SZ";
            if (normalized.endsWith(".BJ")) return "BJ";
            return normalized.matches("\\d{6}") ? (normalized.startsWith("6") ? "SH" : "SZ") : "US";
        }
        return "UNKNOWN";
    }

    /**
     * 校验持仓内联资产字段，资产表仍作为行情和价格快照的内部基础数据。
     */
    private void ensureAssetFields(HoldingRequest request) {
        if (!StringUtils.hasText(request.getAssetName()) || !StringUtils.hasText(request.getSymbol())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请输入持仓名称和资产代码");
        }
        if (!ASSET_TYPES.contains(request.getAssetType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "资产类型只支持 STOCK、FUND、CRYPTO、OTHER");
        }
        if (!QUOTE_SOURCES.contains(request.getQuoteSource())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "行情来源不支持");
        }
        if (!StringUtils.hasText(request.getCurrency())) {
            request.setCurrency("CNY");
        }
    }
}
