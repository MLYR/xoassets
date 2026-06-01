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
import com.xoassets.module.investment.vo.HoldingDetailSummaryVO;
import com.xoassets.module.investment.vo.HoldingDetailVO;
import com.xoassets.module.investment.vo.HoldingSummaryVO;
import com.xoassets.module.investment.vo.HoldingVO;
import com.xoassets.module.investment.vo.InvestmentTransactionVO;
import com.xoassets.persistence.entity.Account;
import com.xoassets.persistence.entity.Asset;
import com.xoassets.persistence.entity.AssetPrice;
import com.xoassets.persistence.entity.Holding;
import com.xoassets.persistence.entity.InvestmentTransaction;
import com.xoassets.persistence.mapper.AccountMapper;
import com.xoassets.persistence.mapper.AssetMapper;
import com.xoassets.persistence.mapper.AssetPriceMapper;
import com.xoassets.persistence.mapper.HoldingMapper;
import com.xoassets.persistence.mapper.InvestmentTransactionMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
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
    private static final List<String> QUOTE_SOURCES = List.of("MANUAL", "COINGECKO", "ALPHA_VANTAGE", "TUSHARE", "AKSHARE");

    private final HoldingMapper holdingMapper;
    private final AssetMapper assetMapper;
    private final AssetPriceMapper assetPriceMapper;
    private final InvestmentTransactionMapper investmentTransactionMapper;
    private final AccountMapper accountMapper;
    private final AssetService assetService;
    private final QuoteService quoteService;

    public HoldingServiceImpl(
            HoldingMapper holdingMapper,
            AssetMapper assetMapper,
            AssetPriceMapper assetPriceMapper,
            InvestmentTransactionMapper investmentTransactionMapper,
            AccountMapper accountMapper,
            AssetService assetService,
            QuoteService quoteService) {
        this.holdingMapper = holdingMapper;
        this.assetMapper = assetMapper;
        this.assetPriceMapper = assetPriceMapper;
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
        List<HoldingVO> holdings = list();
        BigDecimal totalMarketValue = holdings.stream().map(HoldingVO::getMarketValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = holdings.stream().map(HoldingVO::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayProfit = holdings.stream().map(item -> nullToZero(item.getTodayProfit())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal yesterdayProfit = holdings.stream().map(item -> nullToZero(item.getYesterdayProfit())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal floatingProfit = holdings.stream().map(HoldingVO::getFloatingProfit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal floatingProfitRate = totalCost.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : floatingProfit.multiply(BigDecimal.valueOf(100)).divide(totalCost, 4, RoundingMode.HALF_UP);
        return HoldingSummaryVO.builder()
                .totalMarketValue(totalMarketValue.setScale(4, RoundingMode.HALF_UP))
                .totalCost(totalCost.setScale(4, RoundingMode.HALF_UP))
                .todayProfit(todayProfit.setScale(4, RoundingMode.HALF_UP))
                .yesterdayProfit(yesterdayProfit.setScale(4, RoundingMode.HALF_UP))
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
        HoldingVO holdingVO = toVO(holding, asset, priceSnapshots);
        List<InvestmentTransaction> transactions = investmentTransactionMapper.selectList(new LambdaQueryWrapper<InvestmentTransaction>()
                .eq(InvestmentTransaction::getUserId, userId)
                .eq(InvestmentTransaction::getHoldingId, id)
                .orderByDesc(InvestmentTransaction::getTransactionTime)
                .orderByDesc(InvestmentTransaction::getCreatedAt));
        Map<Long, Account> accountMap = accountMap(transactions);
        List<InvestmentTransactionVO> transactionVOList = transactions.stream()
                .map(transaction -> toTransactionVO(transaction, asset, accountMap.get(transaction.getAccountId())))
                .toList();
        return HoldingDetailVO.builder()
                .holding(holdingVO)
                .summary(detailSummary(holdingVO, transactions))
                .transactions(transactionVOList)
                .priceSnapshots(priceSnapshots.stream().map(this::toAssetPriceVO).toList())
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
        BigDecimal quantity = scale4(request.getQuantity());
        BigDecimal avgCost = scale4(request.getAvgCost());
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
        BigDecimal quantity = scale4(request.getQuantity());
        BigDecimal avgCost = scale4(request.getAvgCost());
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
        quantity = scale4(quantity);
        price = scale4(price);
        fee = scale4(fee);
        Holding holding = holdingId == null ? findOrCreateHolding(userId, assetId) : findOwnedHolding(holdingId, userId);
        if (!Objects.equals(holding.getAssetId(), assetId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "交易资产与持仓资产不一致");
        }
        BigDecimal buyCost = quantity.multiply(price).add(fee).setScale(4, RoundingMode.HALF_UP);
        BigDecimal newQuantity = holding.getQuantity().add(quantity);
        BigDecimal newTotalCost = holding.getTotalCost().add(buyCost);
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
        quantity = scale4(quantity);
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
        BigDecimal newQuantity = holding.getQuantity().subtract(quantity);
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
        quantity = scale4(quantity);
        costAmount = scale4(costAmount);
        if (holding.getQuantity().compareTo(quantity) < 0 || holding.getTotalCost().compareTo(costAmount) < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "持仓不足，无法撤销该买入交易");
        }
        BigDecimal newQuantity = holding.getQuantity().subtract(quantity).setScale(4, RoundingMode.HALF_UP);
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
        quantity = scale4(quantity);
        costAmount = scale4(costAmount);
        BigDecimal newQuantity = holding.getQuantity().add(quantity).setScale(4, RoundingMode.HALF_UP);
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
     * 投资模块统一按四位小数参与持仓和成本计算，避免不同入口传入精度不一致。
     */
    private BigDecimal scale4(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(4, RoundingMode.HALF_UP);
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
        Map<Long, List<AssetPrice>> priceMap = priceHistoryMap(assetIds);
        return holdings.stream().map(holding -> toVO(holding, assetMap.get(holding.getAssetId()), priceMap.getOrDefault(holding.getAssetId(), List.of()))).toList();
    }

    /**
     * 转换单条持仓。
     */
    private HoldingVO toVO(Holding holding) {
        Asset asset = assetMapper.selectById(holding.getAssetId());
        List<AssetPrice> prices = priceHistoryMap(Set.of(holding.getAssetId())).getOrDefault(holding.getAssetId(), List.of());
        return toVO(holding, asset, prices);
    }

    /**
     * 计算市值、浮动盈亏和收益率；没有价格时用 avgCost 兜底。
     */
    private HoldingVO toVO(Holding holding, Asset asset, List<AssetPrice> prices) {
        // 最新价、市值和收益指标都从同一组同币种价格快照计算，避免展示和估值口径不一致。
        List<AssetPrice> matchedPrices = prices.stream()
                .filter(price -> priceMatchesAssetCurrency(asset, price))
                .sorted(Comparator.comparing(AssetPrice::getQuoteTime).reversed().thenComparing(AssetPrice::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        AssetPrice matchedPrice = matchedPrices.isEmpty() ? null : matchedPrices.get(0);
        AssetPrice previousPrice = previousPrice(matchedPrices, matchedPrice);
        AssetPrice beforePreviousPrice = beforePreviousPrice(matchedPrices);
        BigDecimal latestPrice = matchedPrice == null ? holding.getAvgCost() : matchedPrice.getPrice();
        BigDecimal previous = previousPrice == null ? null : previousPrice.getPrice();
        BigDecimal beforePrevious = beforePreviousPrice == null ? null : beforePreviousPrice.getPrice();
        BigDecimal marketValue = holding.getQuantity().multiply(latestPrice).setScale(4, RoundingMode.HALF_UP);
        BigDecimal profit = marketValue.subtract(holding.getTotalCost());
        BigDecimal profitRate = holding.getTotalCost().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : profit.multiply(BigDecimal.valueOf(100)).divide(holding.getTotalCost(), 4, RoundingMode.HALF_UP);
        BigDecimal todayProfit = priceDiffProfit(holding.getQuantity(), latestPrice, previous);
        BigDecimal todayChangeRate = changeRate(latestPrice, previous);
        BigDecimal yesterdayProfit = priceDiffProfit(holding.getQuantity(), previous, beforePrevious);
        BigDecimal yesterdayChangeRate = changeRate(previous, beforePrevious);
        BigDecimal breakEvenRate = matchedPrice == null ? null : breakEvenRate(holding.getAvgCost(), latestPrice);
        return HoldingVO.builder()
                .id(holding.getId())
                .assetId(holding.getAssetId())
                .assetName(asset == null ? null : asset.getName())
                .symbol(asset == null ? null : asset.getSymbol())
                .assetType(asset == null ? null : asset.getType())
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
                .previousPriceTime(previousPrice == null ? null : previousPrice.getQuoteTime())
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
     * 计算单个持仓详情汇总，撤销交易保留明细展示但不参与收益统计。
     */
    private HoldingDetailSummaryVO detailSummary(HoldingVO holding, List<InvestmentTransaction> transactions) {
        BigDecimal totalBuyAmount = BigDecimal.ZERO;
        BigDecimal totalSellAmount = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal realizedProfit = BigDecimal.ZERO;
        int buyCount = 0;
        int sellCount = 0;
        java.time.LocalDateTime firstBuyDateTime = null;
        java.time.LocalDateTime lastTradeTime = null;
        for (InvestmentTransaction transaction : transactions) {
            if ("REVOKED".equals(transaction.getStatus())) {
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
                .quantity(transaction.getQuantity())
                .price(transaction.getPrice())
                .amount(transaction.getAmount())
                .fee(transaction.getFee())
                .costAmount(transaction.getCostAmount())
                .realizedProfit(transaction.getRealizedProfit())
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
                .source(price.getSource())
                .quoteTime(price.getQuoteTime())
                .build();
    }

    /**
     * 批量读取价格快照，后续在内存中按币种和日期挑选最新、昨日、前日价格。
     */
    private Map<Long, List<AssetPrice>> priceHistoryMap(Set<Long> assetIds) {
        if (assetIds.isEmpty()) {
            return Map.of();
        }
        return assetPriceMapper.selectList(new LambdaQueryWrapper<AssetPrice>()
                        .in(AssetPrice::getAssetId, assetIds)
                        .orderByDesc(AssetPrice::getQuoteTime)
                        .orderByDesc(AssetPrice::getCreatedAt))
                .stream()
                .collect(Collectors.groupingBy(AssetPrice::getAssetId));
    }

    /**
     * 昨日价格优先取昨天最后一条；缺失时取最新价格之前最近的不同日期价格。
     */
    private AssetPrice previousPrice(List<AssetPrice> prices, AssetPrice latestPrice) {
        if (latestPrice == null) {
            return null;
        }
        LocalDate yesterday = LocalDate.now().minusDays(1);
        AssetPrice exactYesterday = lastPriceOnDate(prices, yesterday);
        if (exactYesterday != null) {
            return exactYesterday;
        }
        LocalDate latestDate = latestPrice.getQuoteTime().toLocalDate();
        return prices.stream()
                .filter(price -> price.getQuoteTime().toLocalDate().isBefore(latestDate))
                .findFirst()
                .orElse(null);
    }

    /**
     * 前日价格只取前天最后一条；没有则返回 null，让前端展示暂无。
     */
    private AssetPrice beforePreviousPrice(List<AssetPrice> prices) {
        return lastPriceOnDate(prices, LocalDate.now().minusDays(2));
    }

    /**
     * 获取指定日期最后一条价格。
     */
    private AssetPrice lastPriceOnDate(List<AssetPrice> prices, LocalDate date) {
        return prices.stream()
                .filter(price -> price.getQuoteTime().toLocalDate().equals(date))
                .findFirst()
                .orElse(null);
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
                .eq(Asset::getSymbol, symbol)
                .last("LIMIT 1"));
        if (exists != null) {
            return exists;
        }
        Asset asset = new Asset();
        asset.setSymbol(symbol);
        asset.setName(request.getAssetName().trim());
        asset.setType(type);
        asset.setCurrency(request.getCurrency());
        asset.setQuoteSource(request.getQuoteSource());
        asset.setQuoteKey(StringUtils.hasText(request.getQuoteKey()) ? request.getQuoteKey().trim() : symbol);
        asset.setStatus(1);
        asset.setDeleted(0);
        assetMapper.insert(asset);
        return asset;
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
