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
import com.xoassets.module.investment.service.InvestmentPositionHistoryService;
import com.xoassets.module.investment.service.InvestmentPositionState;
import com.xoassets.module.investment.service.QuoteService;
import com.xoassets.module.investment.vo.AssetPriceVO;
import com.xoassets.module.investment.vo.HoldingChartPointVO;
import com.xoassets.module.investment.vo.HoldingDetailSummaryVO;
import com.xoassets.module.investment.vo.HoldingDetailVO;
import com.xoassets.module.investment.vo.HoldingSummaryVO;
import com.xoassets.module.investment.vo.HoldingVO;
import com.xoassets.module.investment.vo.InvestmentCalendarDayProfitVO;
import com.xoassets.module.investment.vo.InvestmentModuleAssetVO;
import com.xoassets.module.investment.vo.InvestmentOverviewVO;
import com.xoassets.module.investment.vo.InvestmentTrendPointVO;
import com.xoassets.module.investment.vo.InvestmentTrendVO;
import com.xoassets.module.investment.vo.InvestmentTransactionVO;
import com.xoassets.persistence.entity.Account;
import com.xoassets.persistence.entity.Asset;
import com.xoassets.persistence.entity.AssetPriceCurrent;
import com.xoassets.persistence.entity.AssetPriceDaily;
import com.xoassets.persistence.entity.Holding;
import com.xoassets.persistence.entity.InvestmentDailySnapshot;
import com.xoassets.persistence.entity.InvestmentTransaction;
import com.xoassets.persistence.entity.MarketCalendar;
import com.xoassets.persistence.mapper.AccountMapper;
import com.xoassets.persistence.mapper.AssetMapper;
import com.xoassets.persistence.mapper.AssetPriceCurrentMapper;
import com.xoassets.persistence.mapper.AssetPriceDailyMapper;
import com.xoassets.persistence.mapper.HoldingMapper;
import com.xoassets.persistence.mapper.InvestmentDailySnapshotMapper;
import com.xoassets.persistence.mapper.InvestmentTransactionMapper;
import com.xoassets.persistence.mapper.MarketCalendarMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
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
    private static final String ASSET_TYPE_STOCK = "STOCK";
    private static final String ASSET_TYPE_CRYPTO = "CRYPTO";
    private static final String MODULE_ALL = "ALL";
    private static final String PROFIT_MODE_TODAY = "TODAY";
    private static final String ASSET_SUB_TYPE_ETF = "ETF";
    private static final String VALUATION_REALTIME_PRICE = "REALTIME_PRICE";
    private static final String VALUATION_END_OF_DAY_NAV = "END_OF_DAY_NAV";
    private static final String VALUATION_MONEY_FUND_YIELD = "MONEY_FUND_YIELD";
    private static final String TRADE_VENUE_EXCHANGE = "EXCHANGE";
    private static final String TRADE_VENUE_OTC = "OTC";
    private static final String TRADE_VENUE_CRYPTO_EXCHANGE = "CRYPTO_EXCHANGE";
    private static final String PRICE_STATUS_NORMAL = "NORMAL";
    private static final String PRICE_STATUS_TODAY_PRICE_NOT_AVAILABLE = "TODAY_PRICE_NOT_AVAILABLE";
    private static final String PRICE_STATUS_MARKET_CLOSED = "MARKET_CLOSED";
    private static final String CALENDAR_PRIORITY_SQL = "order by case source when 'MANUAL' then 3 when 'EXCHANGE_ANNOUNCEMENT' then 2 when 'SYSTEM_WEEKDAY' then 1 else 0 end desc, id desc limit 1";
    private static final String NEXT_TRADING_DATE_SQL = "order by trade_date asc, case source when 'MANUAL' then 3 when 'EXCHANGE_ANNOUNCEMENT' then 2 when 'SYSTEM_WEEKDAY' then 1 else 0 end desc, id desc limit 370";

    private final HoldingMapper holdingMapper;
    private final AssetMapper assetMapper;
    private final AssetPriceCurrentMapper assetPriceCurrentMapper;
    private final AssetPriceDailyMapper assetPriceDailyMapper;
    private final InvestmentDailySnapshotMapper investmentDailySnapshotMapper;
    private final InvestmentTransactionMapper investmentTransactionMapper;
    private final MarketCalendarMapper marketCalendarMapper;
    private final AccountMapper accountMapper;
    private final AssetService assetService;
    private final InvestmentPositionHistoryService positionHistoryService;
    private final QuoteService quoteService;

    public HoldingServiceImpl(
            HoldingMapper holdingMapper,
            AssetMapper assetMapper,
            AssetPriceCurrentMapper assetPriceCurrentMapper,
            AssetPriceDailyMapper assetPriceDailyMapper,
            InvestmentDailySnapshotMapper investmentDailySnapshotMapper,
            InvestmentTransactionMapper investmentTransactionMapper,
            MarketCalendarMapper marketCalendarMapper,
            AccountMapper accountMapper,
            AssetService assetService,
            InvestmentPositionHistoryService positionHistoryService,
            QuoteService quoteService) {
        this.holdingMapper = holdingMapper;
        this.assetMapper = assetMapper;
        this.assetPriceCurrentMapper = assetPriceCurrentMapper;
        this.assetPriceDailyMapper = assetPriceDailyMapper;
        this.investmentDailySnapshotMapper = investmentDailySnapshotMapper;
        this.investmentTransactionMapper = investmentTransactionMapper;
        this.marketCalendarMapper = marketCalendarMapper;
        this.accountMapper = accountMapper;
        this.assetService = assetService;
        this.positionHistoryService = positionHistoryService;
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
     * 按基金 / 股票 / 虚拟货币模块筛选持仓，模块判断由资产类型和派生子类型共同决定。
     */
    @Override
    public List<HoldingVO> list(String module) {
        String normalizedModule = normalizeModule(module);
        if (MODULE_ALL.equals(normalizedModule)) {
            return list();
        }
        return list().stream()
                .filter(item -> normalizedModule.equals(moduleOf(item.getAssetType())))
                .toList();
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
        boolean todayProfitAvailable = holdings.stream().anyMatch(this::hasTodayProfit);
        // 今日收益必须尊重单项持仓的“今日价格有效性”，无今日有效价时返回 null 供前端显示 --。
        BigDecimal todayProfit = todayProfitAvailable ? holdings.stream()
                .filter(this::hasTodayProfit)
                .map(item -> nullToZero(item.getTodayProfit()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP) : null;
        BigDecimal todayProfitBase = holdings.stream()
                .filter(item -> Boolean.TRUE.equals(item.getTodayPriceAvailable()) && item.getTodayProfitBase() != null)
                .map(HoldingVO::getTodayProfitBase)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayProfitRate = todayProfitBase.compareTo(BigDecimal.ZERO) <= 0 ? null : rate(todayProfit, todayProfitBase);
        BigDecimal yesterdayProfit = holdings.stream().map(item -> nullToZero(item.getYesterdayProfit())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal yesterdayProfitBase = holdings.stream()
                .filter(item -> item.getYesterdayProfitBase() != null)
                .map(HoldingVO::getYesterdayProfitBase)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal yesterdayProfitRate = yesterdayProfitBase.compareTo(BigDecimal.ZERO) <= 0 ? null : rate(yesterdayProfit, yesterdayProfitBase);
        BigDecimal floatingProfit = holdings.stream().map(HoldingVO::getFloatingProfit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal floatingProfitRate = totalCost.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : floatingProfit.multiply(BigDecimal.valueOf(100)).divide(totalCost, 4, RoundingMode.HALF_UP);
        InvestmentDailySnapshot lastMonthEndSnapshot = lastMonthEndSnapshot(userId, LocalDate.now());
        BigDecimal monthNetInflow = positionHistoryService.netInflow(userId, LocalDate.now().withDayOfMonth(1), LocalDate.now());
        monthNetInflow = monthNetInflow == null ? BigDecimal.ZERO : monthNetInflow;
        BigDecimal lastMonthProfit = lastMonthEndSnapshot == null ? null : totalMarketValue.subtract(scale4(lastMonthEndSnapshot.getMarketValue())).subtract(monthNetInflow).setScale(4, RoundingMode.HALF_UP);
        BigDecimal lastMonthProfitRate = lastMonthEndSnapshot == null ? null : rate(lastMonthProfit, scale4(lastMonthEndSnapshot.getMarketValue()));
        return HoldingSummaryVO.builder()
                .totalMarketValue(totalMarketValue.setScale(4, RoundingMode.HALF_UP))
                .totalCost(totalCost.setScale(4, RoundingMode.HALF_UP))
                .todayProfitAvailable(todayProfitAvailable)
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
     * 投资总览按今日有效价格动态汇总，基金晚间净值更新后自然进入今日收益。
     */
    @Override
    public InvestmentOverviewVO overview() {
        List<HoldingVO> holdings = list();
        BigDecimal totalMarketValue = holdings.stream().map(HoldingVO::getMarketValue).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalCost = holdings.stream().map(HoldingVO::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP);
        BigDecimal holdingProfit = holdings.stream().map(HoldingVO::getFloatingProfit).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP);
        boolean todayProfitAvailable = holdings.stream().anyMatch(this::hasTodayProfit);
        // 总览今日收益只有存在今日有效价格时才返回金额，避免基金净值未出或休市时把 0 当成收益。
        BigDecimal todayProfit = todayProfitAvailable ? holdings.stream()
                .filter(this::hasTodayProfit)
                .map(item -> nullToZero(item.getTodayProfitByCurrentQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP) : null;
        BigDecimal yesterdayProfit = holdings.stream()
                .map(item -> nullToZero(item.getYesterdayProfit()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
        return InvestmentOverviewVO.builder()
                .totalInvestmentAsset(totalMarketValue)
                .totalCost(totalCost)
                .holdingProfit(holdingProfit)
                .holdingProfitRate(totalCost.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO : rate(holdingProfit, totalCost))
                .todayProfitAvailable(todayProfitAvailable)
                .todayProfit(todayProfit)
                .todayProfitAssetScope("今日有效价资产")
                .todayProfitStatusLabel(todayProfitStatusLabel(holdings, todayProfitAvailable))
                .yesterdayProfit(yesterdayProfit)
                .yesterdayProfitAssetScope("上一交易日收益")
                .moduleAssets(moduleAssets(holdings, totalMarketValue))
                .build();
    }

    /**
     * 查询投资资产真实趋势，直接读取投资日快照，不用前端静态回推点位。
     */
    @Override
    public List<InvestmentTrendPointVO> trend(LocalDate startDate, LocalDate endDate) {
        Long userId = LoginUserContext.getUserId();
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate start = startDate == null ? end.minusDays(30) : startDate;
        return investmentDailySnapshotMapper.selectList(new LambdaQueryWrapper<InvestmentDailySnapshot>()
                        .eq(InvestmentDailySnapshot::getUserId, userId)
                        .between(InvestmentDailySnapshot::getSnapshotDate, start, end)
                        .orderByAsc(InvestmentDailySnapshot::getSnapshotDate))
                .stream()
                .map(item -> InvestmentTrendPointVO.builder()
                        .date(item.getSnapshotDate())
                        .marketValue(scale4(item.getMarketValue()))
                        .totalProfit(scale4(nullToZero(item.getRealizedProfit()).add(nullToZero(item.getFloatingProfit()))))
                        .assetAmount(scale4(item.getMarketValue()))
                        .holdingProfit(scale4(nullToZero(item.getRealizedProfit()).add(nullToZero(item.getFloatingProfit()))))
                        .build())
                .toList();
    }

    /**
     * 模块趋势：ALL 复用用户投资日快照；单模块用历史持仓份额和日级价格重建资产金额。
     */
    @Override
    public InvestmentTrendVO trend(String module, String period, LocalDate startDate, LocalDate endDate) {
        String normalizedModule = normalizeModule(module);
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate start = startDate == null ? end.minusDays(periodDays(period)) : startDate;
        List<InvestmentTrendPointVO> points = MODULE_ALL.equals(normalizedModule)
                ? trend(start, end)
                : moduleTrendPoints(normalizedModule, start, end);
        return InvestmentTrendVO.builder()
                .module(normalizedModule)
                .period(StringUtils.hasText(period) ? period.trim().toUpperCase() : "MONTH")
                .points(points)
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
        List<AssetPriceVO> priceSnapshots = latestPriceSnapshots(asset, holding.getAssetId());
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
                .priceSnapshots(priceSnapshots)
                .chartPoints(holdingChartPoints(userId, holdingVO, detailSummary, transactions, priceSnapshots))
                .profitCalendar(profitCalendar(id, YearMonth.now()))
                .build();
    }

    /**
     * 单持仓收益日历，按上一价格日的历史持仓份额计算当天价格差收益。
     */
    @Override
    public List<InvestmentCalendarDayProfitVO> profitCalendar(Long id, YearMonth month) {
        Long userId = LoginUserContext.getUserId();
        Holding holding = findOwnedHolding(id, userId);
        Asset asset = assetMapper.selectById(holding.getAssetId());
        YearMonth targetMonth = month == null ? YearMonth.now() : month;
        LocalDate start = targetMonth.atDay(1);
        LocalDate end = targetMonth.atEndOfMonth();
        AssetMeta assetMeta = deriveAssetMeta(asset);
        List<DailyPricePoint> prices = calendarPricePoints(asset, holding.getAssetId(), end);
        Map<LocalDate, CalendarProfitData> profitByDisplayDate = calendarProfitDataMap(userId, holding, assetMeta, prices, start, end);
        List<InvestmentCalendarDayProfitVO> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            CalendarProfitData profitData = profitByDisplayDate.get(date);
            boolean tradingDay = tradingDayForAsset(asset, date);
            boolean marketClosed = marketClosedForAsset(asset, date);
            result.add(InvestmentCalendarDayProfitVO.builder()
                    .date(date)
                    .profitAmount(profitData == null ? null : profitData.profit())
                    .profitRate(profitData == null ? null : profitData.profitRate())
                    .marketValue(profitData == null ? null : profitData.marketValue())
                    .price(profitData == null ? null : profitData.price())
                    .previousPrice(profitData == null ? null : profitData.previousPrice())
                    .hasPrice(profitData != null)
                    .tradingDay(tradingDay)
                    .marketClosed(marketClosed)
                    .statusLabel(marketClosed ? "休市" : profitData == null ? "无价格" : "有收益")
                    .priceLabel(priceLabel(assetMeta))
                    .build());
        }
        return result;
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
        holding.setVersion(0L);
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
        updateHoldingBalance(holding);
        return toVO(holding);
    }

    /**
     * 只有清仓后的持仓允许删除，避免用户误删仍有市值的资产。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long id) {
        Long userId = LoginUserContext.getUserId();
        Holding holding = findOwnedHolding(id, userId);
        if (holding.getQuantity() != null && holding.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "持仓未清仓，不允许删除");
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
        quantity = scaleQuantity(quantity);
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
        created.setVersion(0L);
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
        long version = holding.getVersion() == null ? 0L : holding.getVersion();
        holding.setVersion(version + 1);
        int updated = holdingMapper.update(holding, new LambdaUpdateWrapper<Holding>()
                .eq(Holding::getId, holding.getId())
                .eq(Holding::getUserId, holding.getUserId())
                .eq(Holding::getVersion, version));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "持仓更新冲突，请重试");
        }
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
     * 今日收益率缺少收益或分母时返回 null，前端才能和收益金额一起展示 --。
     */
    private BigDecimal nullableRate(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 4, RoundingMode.HALF_UP);
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
        AssetPriceCurrent matchedPrice = priceContext == null || !priceMatchesAssetCurrency(asset, priceContext.currentPrice()) ? null : priceContext.currentPrice();
        AssetPriceDaily previousDaily = priceContext == null || !priceMatchesAssetCurrency(asset, priceContext.previousDaily()) ? null : priceContext.previousDaily();
        AssetPriceDaily beforePreviousDaily = priceContext == null || !priceMatchesAssetCurrency(asset, priceContext.beforePreviousDaily()) ? null : priceContext.beforePreviousDaily();
        BigDecimal latestPrice = matchedPrice == null ? holding.getAvgCost() : matchedPrice.getPrice();
        // 今日收益按“当前价 - 最近交易日日收盘价”计算，不能用自然日 yesterday 推断交易日。
        BigDecimal previous = matchedPrice != null && matchedPrice.getPreviousClose() != null
                ? matchedPrice.getPreviousClose()
                : previousDaily == null ? null : previousDaily.getClosePrice();
        BigDecimal yesterdayPrevious = yesterdayPreviousPrice(previousDaily);
        BigDecimal beforePrevious = yesterdayBeforePreviousPrice(beforePreviousDaily);
        LocalDate previousDate = previousPriceDate(matchedPrice, previousDaily);
        LocalDate beforePreviousDate = beforePreviousPriceDate(previousDaily, beforePreviousDaily);
        BigDecimal todayBaselineQuantity = previousDate == null ? holding.getQuantity() : positionHistoryService.quantityAt(holding.getUserId(), holding.getId(), holding.getAssetId(), previousDate);
        if (todayBaselineQuantity == null) {
            todayBaselineQuantity = holding.getQuantity();
        }
        BigDecimal yesterdayBaselineQuantity = beforePreviousDate == null ? null : positionHistoryService.quantityAt(holding.getUserId(), holding.getId(), holding.getAssetId(), beforePreviousDate);
        BigDecimal marketValue = holding.getQuantity().multiply(latestPrice).setScale(4, RoundingMode.HALF_UP);
        BigDecimal profit = marketValue.subtract(holding.getTotalCost());
        BigDecimal profitRate = holding.getTotalCost().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : profit.multiply(BigDecimal.valueOf(100)).divide(holding.getTotalCost(), 4, RoundingMode.HALF_UP);
        LocalDate priceDate = matchedPrice == null || matchedPrice.getQuoteTime() == null ? null : matchedPrice.getQuoteTime().toLocalDate();
        boolean todayPriceAvailable = todayPriceAvailable(asset, priceDate);
        // 今日收益同时返回当前份额和上一日份额两个口径，便于后续确认最终展示方案。
        BigDecimal todayProfitByCurrentQuantity = todayPriceAvailable ? priceDiffProfit(holding.getQuantity(), latestPrice, previous) : null;
        BigDecimal todayProfitBaseByCurrentQuantity = todayPriceAvailable && previous != null ? holding.getQuantity().multiply(previous).setScale(4, RoundingMode.HALF_UP) : null;
        BigDecimal todayProfitByPreviousSnapshotQuantity = todayPriceAvailable ? priceDiffProfit(todayBaselineQuantity, latestPrice, previous) : null;
        BigDecimal todayProfitBaseByPreviousSnapshotQuantity = todayPriceAvailable && previous != null ? todayBaselineQuantity.multiply(previous).setScale(4, RoundingMode.HALF_UP) : null;
        BigDecimal todayProfit = todayProfitByCurrentQuantity;
        BigDecimal todayProfitBase = todayProfitBaseByCurrentQuantity;
        BigDecimal todayChangeRate = todayPriceAvailable ? changeRate(latestPrice, previous) : null;
        // 昨日收益使用上上交易日日终持仓数量，避免把昨日或今日新增份额套进历史涨跌。
        BigDecimal yesterdayProfit = yesterdayBaselineQuantity == null ? null : priceDiffProfit(yesterdayBaselineQuantity, yesterdayPrevious, beforePrevious);
        BigDecimal yesterdayProfitBase = beforePrevious == null || yesterdayBaselineQuantity == null ? null : yesterdayBaselineQuantity.multiply(beforePrevious).setScale(4, RoundingMode.HALF_UP);
        BigDecimal yesterdayChangeRate = changeRate(yesterdayPrevious, beforePrevious);
        BigDecimal breakEvenRate = matchedPrice == null ? null : breakEvenRate(holding.getAvgCost(), latestPrice);
        AssetMeta assetMeta = deriveAssetMeta(asset);
        boolean marketClosedToday = marketClosedForAsset(asset, LocalDate.now());
        String priceStatus = marketClosedToday ? PRICE_STATUS_MARKET_CLOSED : todayPriceAvailable ? PRICE_STATUS_NORMAL : PRICE_STATUS_TODAY_PRICE_NOT_AVAILABLE;
        return HoldingVO.builder()
                .id(holding.getId())
                .assetId(holding.getAssetId())
                .assetName(asset == null ? null : asset.getName())
                .symbol(asset == null ? null : asset.getSymbol())
                .assetType(asset == null ? null : asset.getType())
                .assetSubType(assetMeta.assetSubType())
                .profitDisplayMode(assetMeta.profitDisplayMode())
                .valuationMode(assetMeta.valuationMode())
                .tradeVenue(assetMeta.tradeVenue())
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
                .previousPriceTime(previousDaily == null ? null : previousDaily.getTradeDate().atStartOfDay())
                .priceDate(priceDate)
                .todayPriceAvailable(todayPriceAvailable)
                .todayProfitAvailable(todayPriceAvailable && todayProfit != null)
                .priceStatus(priceStatus)
                .latestPriceSource(matchedPrice == null ? null : matchedPrice.getSource())
                .marketStatus(matchedPrice == null ? null : matchedPrice.getMarketStatus())
                .primaryProfitLabel("今日收益")
                .primaryProfitAmount(todayProfit)
                .secondaryProfitLabel("持有收益")
                .secondaryProfitAmount(profit)
                .priceLabel(priceLabel(assetMeta))
                .marketValue(marketValue)
                .todayProfit(todayProfit)
                .todayProfitBase(todayProfitBase)
                .todayChangeRate(todayChangeRate)
                .todayProfitByCurrentQuantity(todayProfitByCurrentQuantity)
                .todayProfitRateByCurrentQuantity(nullableRate(todayProfitByCurrentQuantity, todayProfitBaseByCurrentQuantity))
                .todayProfitByPreviousSnapshotQuantity(todayProfitByPreviousSnapshotQuantity)
                .todayProfitRateByPreviousSnapshotQuantity(nullableRate(todayProfitByPreviousSnapshotQuantity, todayProfitBaseByPreviousSnapshotQuantity))
                .yesterdayProfit(yesterdayProfit)
                .yesterdayProfitBase(yesterdayProfitBase)
                .yesterdayChangeRate(yesterdayChangeRate)
                .floatingProfit(profit)
                .floatingProfitRate(profitRate)
                .breakEvenRate(breakEvenRate)
                .remark(holding.getRemark())
                .status(holding.getStatus())
                .build();
    }

    /**
     * 汇总总览里的三大模块卡片，模块收益使用各模块自己的主收益口径。
     */
    private List<InvestmentModuleAssetVO> moduleAssets(List<HoldingVO> holdings, BigDecimal totalMarketValue) {
        return List.of(moduleAsset("FUND", "基金", holdings, totalMarketValue),
                moduleAsset("STOCK", "股票", holdings, totalMarketValue),
                moduleAsset("CRYPTO", "虚拟货币", holdings, totalMarketValue));
    }

    /**
     * 收益日历只读取日级价格表；旧价格快照表退役后，历史缺口直接展示为空。
     */
    private List<DailyPricePoint> calendarPricePoints(Asset asset, Long assetId, LocalDate end) {
        Map<LocalDate, DailyPricePoint> priceMap = new LinkedHashMap<>();
        List<AssetPriceDaily> dailyPrices = assetPriceDailyMapper.selectList(new LambdaQueryWrapper<AssetPriceDaily>()
                .eq(AssetPriceDaily::getAssetId, assetId)
                .le(AssetPriceDaily::getTradeDate, end)
                .orderByAsc(AssetPriceDaily::getTradeDate));
        (dailyPrices == null ? List.<AssetPriceDaily>of() : dailyPrices)
                .stream()
                .filter(price -> priceMatchesAssetCurrency(asset, price))
                .forEach(price -> priceMap.put(price.getTradeDate(), new DailyPricePoint(price.getTradeDate(), price.getClosePrice(), price.getPreviousClose())));
        return priceMap.values().stream()
                .sorted(Comparator.comparing(DailyPricePoint::tradeDate))
                .toList();
    }

    /**
     * 净值型基金的收益展示日是净值日后的下一交易日，和用户截图中的收益明细日期保持一致。
     */
    private Map<LocalDate, CalendarProfitData> calendarProfitDataMap(Long userId, Holding holding, AssetMeta assetMeta, List<DailyPricePoint> prices, LocalDate start, LocalDate end) {
        Map<LocalDate, CalendarProfitData> result = new HashMap<>();
        DailyPricePoint previous = null;
        boolean manualOnly = effectiveTransactionCount(userId, holding.getId()) == 0;
        for (DailyPricePoint price : prices) {
            BigDecimal previousPrice = previous == null ? price.previousClose() : previous.closePrice();
            LocalDate previousPriceDate = previous == null ? price.tradeDate().minusDays(1) : previous.tradeDate();
            if (previousPrice != null) {
                LocalDate displayDate = calendarDisplayDate(assetMeta, price.tradeDate());
                if (!displayDate.isBefore(start) && !displayDate.isAfter(end)) {
                    BigDecimal quantity = calendarQuantity(userId, holding, previousPriceDate, manualOnly);
                    BigDecimal profit = priceDiffProfit(quantity, price.closePrice(), previousPrice);
                    BigDecimal baseAmount = quantity.multiply(previousPrice).setScale(4, RoundingMode.HALF_UP);
                    result.put(displayDate, new CalendarProfitData(
                            profit,
                            profit == null || baseAmount.compareTo(BigDecimal.ZERO) <= 0 ? null : rate(profit, baseAmount),
                            quantity.multiply(price.closePrice()).setScale(4, RoundingMode.HALF_UP),
                            price.closePrice(),
                            previousPrice));
                }
            }
            previous = price;
        }
        return result;
    }

    private BigDecimal calendarQuantity(Long userId, Holding holding, LocalDate previousPriceDate, boolean manualOnly) {
        BigDecimal quantity = positionHistoryService.quantityAt(userId, holding.getId(), holding.getAssetId(), previousPriceDate);
        // 手工初始化持仓的 created_at 是录入时间，不等于真实买入时间；无有效交易流水时用当前份额对齐用户截图历史收益。
        if (manualOnly && quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return scaleQuantity(holding.getQuantity());
        }
        return quantity;
    }

    private long effectiveTransactionCount(Long userId, Long holdingId) {
        return investmentTransactionMapper.selectCount(new LambdaQueryWrapper<InvestmentTransaction>()
                .eq(InvestmentTransaction::getUserId, userId)
                .eq(InvestmentTransaction::getHoldingId, holdingId)
                .in(InvestmentTransaction::getStatus, List.of("NORMAL", "CONFIRMED")));
    }

    private LocalDate calendarDisplayDate(AssetMeta assetMeta, LocalDate priceDate) {
        if (VALUATION_END_OF_DAY_NAV.equals(assetMeta.valuationMode()) || VALUATION_MONEY_FUND_YIELD.equals(assetMeta.valuationMode())) {
            return nextTradingDate(priceDate);
        }
        return priceDate;
    }

    private LocalDate nextTradingDate(LocalDate date) {
        List<MarketCalendar> calendars = marketCalendarMapper.selectList(new LambdaQueryWrapper<MarketCalendar>()
                        .eq(MarketCalendar::getMarket, "A_SHARE")
                        .gt(MarketCalendar::getTradeDate, date)
                        // 先按日期、再按日历来源优先级排序；同日只看最高优先级记录，避免节假日被系统工作日重复行穿透。
                        .last(NEXT_TRADING_DATE_SQL));
        if (calendars != null) {
            LocalDate checkedDate = null;
            for (MarketCalendar calendar : calendars) {
                if (calendar.getTradeDate() == null || calendar.getTradeDate().equals(checkedDate)) {
                    continue;
                }
                checkedDate = calendar.getTradeDate();
                if (Boolean.TRUE.equals(calendar.getTradingDay())) {
                    return calendar.getTradeDate();
                }
            }
        }
        LocalDate next = date.plusDays(1);
        while (next.getDayOfWeek().getValue() >= 6) {
            next = next.plusDays(1);
        }
        return next;
    }

    private InvestmentModuleAssetVO moduleAsset(String module, String name, List<HoldingVO> holdings, BigDecimal totalMarketValue) {
        List<HoldingVO> moduleHoldings = holdings.stream()
                .filter(item -> module.equals(moduleOf(item.getAssetType())))
                .toList();
        BigDecimal assetAmount = moduleHoldings.stream().map(HoldingVO::getMarketValue).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalCost = moduleHoldings.stream().map(HoldingVO::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP);
        BigDecimal holdingProfit = moduleHoldings.stream().map(HoldingVO::getFloatingProfit).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP);
        boolean primaryProfitAvailable = moduleHoldings.stream().anyMatch(this::hasTodayProfit);
        // 模块今日收益遵守“未更新显示 --”，没有今日有效价时不返回 0。
        BigDecimal primaryProfitAmount = primaryProfitAvailable ? moduleHoldings.stream()
                .filter(this::hasTodayProfit)
                .map(item -> nullToZero(item.getTodayProfitByCurrentQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP) : null;
        return InvestmentModuleAssetVO.builder()
                .module(module)
                .name(name)
                .assetAmount(assetAmount)
                .assetRatio(totalMarketValue.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO : assetAmount.multiply(BigDecimal.valueOf(100)).divide(totalMarketValue, 4, RoundingMode.HALF_UP))
                .primaryProfitLabel(modulePrimaryProfitLabel(module))
                .primaryProfitAvailable(primaryProfitAvailable)
                .primaryProfitAmount(primaryProfitAmount)
                .primaryProfitStatusLabel(todayProfitStatusLabel(moduleHoldings, primaryProfitAvailable))
                .holdingProfit(holdingProfit)
                .holdingProfitRate(totalCost.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO : rate(holdingProfit, totalCost))
                .holdingCount(moduleHoldings.size())
                .build();
    }

    /**
     * 总览和模块卡片在今日收益为 -- 时也要说明原因，休市优先于普通价格未更新。
     */
    private String todayProfitStatusLabel(List<HoldingVO> holdings, boolean todayProfitAvailable) {
        if (todayProfitAvailable) {
            return "今日有效价资产";
        }
        if (holdings == null || holdings.isEmpty()) {
            return "暂无持仓";
        }
        boolean hasMarketClosed = holdings.stream().anyMatch(item -> PRICE_STATUS_MARKET_CLOSED.equals(item.getPriceStatus()));
        if (hasMarketClosed) {
            return "今日休市";
        }
        boolean onlyFund = holdings.stream().allMatch(item -> ASSET_TYPE_FUND.equals(item.getAssetType()));
        return onlyFund ? "今日净值未更新" : "今日价格未更新";
    }

    private boolean hasTodayProfit(HoldingVO holding) {
        return holding != null && Boolean.TRUE.equals(holding.getTodayPriceAvailable()) && holding.getTodayProfitByCurrentQuantity() != null;
    }

    /**
     * 单模块趋势用历史头寸和日级价格重建，不使用当前持仓数量倒推历史。
     */
    private List<InvestmentTrendPointVO> moduleTrendPoints(String module, LocalDate start, LocalDate end) {
        Long userId = LoginUserContext.getUserId();
        List<Holding> allHoldings = holdingMapper.selectList(new LambdaQueryWrapper<Holding>()
                .eq(Holding::getUserId, userId)
                .eq(Holding::getStatus, 1));
        if (allHoldings.isEmpty()) {
            return List.of();
        }
        Set<Long> assetIds = allHoldings.stream().map(Holding::getAssetId).collect(Collectors.toSet());
        Map<Long, Asset> assetMap = assetMapper.selectBatchIds(assetIds).stream().collect(Collectors.toMap(Asset::getId, asset -> asset));
        List<Holding> moduleHoldings = allHoldings.stream()
                .filter(holding -> module.equals(moduleOf(assetMap.get(holding.getAssetId()) == null ? null : assetMap.get(holding.getAssetId()).getType())))
                .toList();
        if (moduleHoldings.isEmpty()) {
            return List.of();
        }
        Set<Long> moduleAssetIds = moduleHoldings.stream().map(Holding::getAssetId).collect(Collectors.toSet());
        Map<Long, List<AssetPriceDaily>> priceMap = dailyPricesUntil(moduleAssetIds, end);
        List<InvestmentTrendPointVO> points = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            Map<Long, InvestmentPositionState> positions = positionHistoryService.positionsAt(userId, date);
            BigDecimal assetAmount = BigDecimal.ZERO;
            BigDecimal totalCost = BigDecimal.ZERO;
            for (Holding holding : moduleHoldings) {
                InvestmentPositionState state = positions.get(holding.getId());
                if (state == null || !Objects.equals(state.assetId(), holding.getAssetId())) {
                    continue;
                }
                Asset asset = assetMap.get(holding.getAssetId());
                BigDecimal price = closePriceOnOrBefore(priceMap.getOrDefault(holding.getAssetId(), List.of()), date);
                if (asset == null || price == null) {
                    continue;
                }
                assetAmount = assetAmount.add(state.quantity().multiply(price).setScale(4, RoundingMode.HALF_UP));
                totalCost = totalCost.add(scale4(state.totalCost()));
            }
            BigDecimal holdingProfit = assetAmount.subtract(totalCost).setScale(4, RoundingMode.HALF_UP);
            points.add(InvestmentTrendPointVO.builder()
                    .date(date)
                    .marketValue(assetAmount.setScale(4, RoundingMode.HALF_UP))
                    .totalProfit(holdingProfit)
                    .assetAmount(assetAmount.setScale(4, RoundingMode.HALF_UP))
                    .holdingProfit(holdingProfit)
                    .primaryProfitLabel(modulePrimaryProfitLabel(module))
                    // 趋势点只负责市值和持有收益；没有逐日收益归因时返回 null，避免把缺失收益冒充为 0。
                    .primaryProfitAmount(null)
                    .build());
        }
        return points;
    }

    /**
     * 读取模块趋势需要的日级价格，包含 start 之前价格以便找到指定日可用收盘价。
     */
    private Map<Long, List<AssetPriceDaily>> dailyPricesUntil(Set<Long> assetIds, LocalDate end) {
        if (assetIds.isEmpty()) {
            return Map.of();
        }
        List<AssetPriceDaily> rows = assetPriceDailyMapper.selectList(new LambdaQueryWrapper<AssetPriceDaily>()
                .in(AssetPriceDaily::getAssetId, assetIds)
                .le(AssetPriceDaily::getTradeDate, end)
                .orderByAsc(AssetPriceDaily::getTradeDate));
        return (rows == null ? List.<AssetPriceDaily>of() : rows)
                .stream()
                .collect(Collectors.groupingBy(AssetPriceDaily::getAssetId, LinkedHashMap::new, Collectors.toList()));
    }

    private BigDecimal closePriceOnOrBefore(List<AssetPriceDaily> prices, LocalDate date) {
        BigDecimal result = null;
        for (AssetPriceDaily price : prices) {
            if (price.getTradeDate().isAfter(date)) {
                break;
            }
            result = price.getClosePrice();
        }
        return result;
    }

    private Map<LocalDate, LocalDate> previousPriceDateMap(List<AssetPriceDaily> prices) {
        Map<LocalDate, LocalDate> result = new HashMap<>();
        AssetPriceDaily previous = null;
        for (AssetPriceDaily price : prices) {
            if (previous != null) {
                result.put(price.getTradeDate(), previous.getTradeDate());
            }
            previous = price;
        }
        return result;
    }

    private Map<LocalDate, BigDecimal> previousPriceMap(List<AssetPriceDaily> prices) {
        Map<LocalDate, BigDecimal> result = new HashMap<>();
        AssetPriceDaily previous = null;
        for (AssetPriceDaily price : prices) {
            if (previous != null) {
                result.put(price.getTradeDate(), previous.getClosePrice());
            }
            previous = price;
        }
        return result;
    }

    /**
     * 资产展示元数据优先用稳定字段派生，后续若落库字段补齐也不影响前端契约。
     */
    private AssetMeta deriveAssetMeta(Asset asset) {
        if (asset == null) {
            return new AssetMeta("OTHER", "HOLDING", VALUATION_REALTIME_PRICE, TRADE_VENUE_EXCHANGE);
        }
        String type = asset.getType();
        String name = asset.getName() == null ? "" : asset.getName().toUpperCase();
        String symbol = asset.getSymbol() == null ? "" : asset.getSymbol().toUpperCase();
        String market = asset.getMarket() == null ? "" : asset.getMarket().toUpperCase();
        boolean etf = name.contains("ETF") || symbol.contains("ETF");
        if (ASSET_TYPE_CRYPTO.equals(type)) {
            return new AssetMeta("CRYPTO_SPOT", PROFIT_MODE_TODAY, VALUATION_REALTIME_PRICE, TRADE_VENUE_CRYPTO_EXCHANGE);
        }
        if (ASSET_TYPE_STOCK.equals(type)) {
            String subType = etf ? ASSET_SUB_TYPE_ETF : market.contains("HK") ? "HK_STOCK" : market.contains("US") ? "US_STOCK" : "CN_STOCK";
            return new AssetMeta(subType, PROFIT_MODE_TODAY, VALUATION_REALTIME_PRICE, TRADE_VENUE_EXCHANGE);
        }
        if (ASSET_TYPE_FUND.equals(type)) {
            if (etf) {
                return new AssetMeta(ASSET_SUB_TYPE_ETF, PROFIT_MODE_TODAY, VALUATION_REALTIME_PRICE, TRADE_VENUE_EXCHANGE);
            }
            if (name.contains("QDII")) {
                return new AssetMeta("QDII_FUND", PROFIT_MODE_TODAY, VALUATION_END_OF_DAY_NAV, TRADE_VENUE_OTC);
            }
            if (name.contains("货币")) {
                return new AssetMeta("MONEY_FUND", PROFIT_MODE_TODAY, VALUATION_MONEY_FUND_YIELD, TRADE_VENUE_OTC);
            }
            if (name.contains("债")) {
                return new AssetMeta("BOND_FUND", PROFIT_MODE_TODAY, VALUATION_END_OF_DAY_NAV, TRADE_VENUE_OTC);
            }
            return new AssetMeta("OTC_FUND", PROFIT_MODE_TODAY, VALUATION_END_OF_DAY_NAV, TRADE_VENUE_OTC);
        }
        return new AssetMeta("OTHER", "HOLDING", VALUATION_REALTIME_PRICE, TRADE_VENUE_EXCHANGE);
    }

    private String priceLabel(AssetMeta assetMeta) {
        if (VALUATION_END_OF_DAY_NAV.equals(assetMeta.valuationMode()) || VALUATION_MONEY_FUND_YIELD.equals(assetMeta.valuationMode())) {
            return "最新净值";
        }
        return "当前价";
    }

    private String modulePrimaryProfitLabel(String module) {
        if ("CRYPTO".equals(module)) {
            return "今日收益";
        }
        return "今日收益";
    }

    private String normalizeModule(String module) {
        if (!StringUtils.hasText(module)) {
            return MODULE_ALL;
        }
        String normalized = module.trim().toUpperCase();
        return List.of(MODULE_ALL, ASSET_TYPE_FUND, ASSET_TYPE_STOCK, ASSET_TYPE_CRYPTO).contains(normalized) ? normalized : MODULE_ALL;
    }

    private String moduleOf(String assetType) {
        if (ASSET_TYPE_FUND.equals(assetType)) {
            return ASSET_TYPE_FUND;
        }
        if (ASSET_TYPE_STOCK.equals(assetType)) {
            return ASSET_TYPE_STOCK;
        }
        if (ASSET_TYPE_CRYPTO.equals(assetType)) {
            return ASSET_TYPE_CRYPTO;
        }
        return "OTHER";
    }

    private int periodDays(String period) {
        if (!StringUtils.hasText(period)) {
            return 30;
        }
        return switch (period.trim().toUpperCase()) {
            case "WEEK" -> 7;
            case "QUARTER" -> 90;
            case "YEAR" -> 365;
            default -> 30;
        };
    }

    private record AssetMeta(String assetSubType, String profitDisplayMode, String valuationMode, String tradeVenue) {
    }

    private record DailyPricePoint(LocalDate tradeDate, BigDecimal closePrice, BigDecimal previousClose) {
    }

    private record CalendarProfitData(BigDecimal profit, BigDecimal profitRate, BigDecimal marketValue, BigDecimal price, BigDecimal previousPrice) {
    }

    /**
     * 查询详情页最近 30 条价格点；旧快照表退役后，曲线由 current + daily 共同提供。
     */
    private List<AssetPriceVO> latestPriceSnapshots(Asset asset, Long assetId) {
        List<AssetPriceVO> snapshots = new ArrayList<>();
        AssetPriceCurrent current = assetPriceCurrentMapper.selectById(assetId);
        LocalDate currentDate = null;
        if (priceMatchesAssetCurrency(asset, current)) {
            snapshots.add(toAssetPriceVO(current));
            currentDate = current.getQuoteTime() == null ? null : current.getQuoteTime().toLocalDate();
        }
        List<AssetPriceDaily> dailyPrices = assetPriceDailyMapper.selectList(new LambdaQueryWrapper<AssetPriceDaily>()
                .eq(AssetPriceDaily::getAssetId, assetId)
                .orderByDesc(AssetPriceDaily::getTradeDate)
                .last("limit 30"));
        for (AssetPriceDaily daily : dailyPrices == null ? List.<AssetPriceDaily>of() : dailyPrices) {
            if (!priceMatchesAssetCurrency(asset, daily) || Objects.equals(currentDate, daily.getTradeDate())) {
                continue;
            }
            snapshots.add(toAssetPriceVO(daily));
        }
        return snapshots.stream()
                .filter(price -> price.getQuoteTime() != null)
                .sorted(Comparator.comparing(AssetPriceVO::getQuoteTime).reversed())
                .limit(30)
                .toList();
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
     * 生成持仓详情金额曲线：每个价格日期都重建当日日终头寸，避免用当前份额倒推历史市值。
     */
    private List<HoldingChartPointVO> holdingChartPoints(Long userId, HoldingVO holding, HoldingDetailSummaryVO summary, List<InvestmentTransaction> transactions, List<AssetPriceVO> priceSnapshots) {
        List<AssetPriceVO> points = priceSnapshots == null ? List.of() : priceSnapshots.stream()
                .filter(price -> price.getPrice() != null)
                .sorted(java.util.Comparator.comparing(AssetPriceVO::getQuoteTime))
                .toList();
        if (points.isEmpty()) {
            BigDecimal assetAmount = scale4(holding.getMarketValue());
            return List.of(HoldingChartPointVO.builder()
                    .quoteTime(holding.getLatestPriceTime() == null ? java.time.LocalDateTime.now() : holding.getLatestPriceTime())
                    .totalAssetAmount(assetAmount)
                    .totalProfitAmount(scale4(summary.getTotalProfit()))
                    .build());
        }
        return points.stream()
                .map(price -> {
                    LocalDate pointDate = price.getQuoteTime().toLocalDate();
                    InvestmentPositionState state = positionHistoryService.positionsAt(userId, pointDate).get(holding.getId());
                    BigDecimal quantity = state == null ? BigDecimal.ZERO : nullToZero(state.quantity());
                    BigDecimal totalCost = state == null ? BigDecimal.ZERO : scale4(state.totalCost());
                    BigDecimal assetAmount = quantity.multiply(price.getPrice()).setScale(4, RoundingMode.HALF_UP);
                    BigDecimal profitAmount = realizedProfitAt(transactions, pointDate).add(assetAmount.subtract(totalCost)).setScale(4, RoundingMode.HALF_UP);
                    return HoldingChartPointVO.builder()
                            .quoteTime(price.getQuoteTime())
                            .totalAssetAmount(assetAmount)
                            .totalProfitAmount(profitAmount)
                            .build();
                })
                .toList();
    }

    /**
     * 详情趋势中的累计已实现收益只统计截至价格日期已经发生的有效卖出。
     */
    private BigDecimal realizedProfitAt(List<InvestmentTransaction> transactions, LocalDate date) {
        if (transactions == null || date == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return transactions.stream()
                .filter(this::isEffectiveInvestmentTransaction)
                .filter(transaction -> "SELL".equals(transaction.getType()))
                .filter(transaction -> !effectiveTransactionDate(transaction).isAfter(date))
                .map(InvestmentTransaction::getRealizedProfit)
                .map(this::scale4)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private LocalDate effectiveTransactionDate(InvestmentTransaction transaction) {
        if ("AMOUNT_NAV".equals(transaction.getInputMode()) && transaction.getConfirmedDate() != null) {
            return transaction.getConfirmedDate();
        }
        if (transaction.getTradeDate() != null) {
            return transaction.getTradeDate();
        }
        return transaction.getTransactionTime() == null ? LocalDate.MAX : transaction.getTransactionTime().toLocalDate();
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
     * 转换当前价格供前端绘制轻量价格趋势。
     */
    private AssetPriceVO toAssetPriceVO(AssetPriceCurrent price) {
        return AssetPriceVO.builder()
                .id(null)
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
     * 转换日级价格供前端绘制轻量价格趋势。
     */
    private AssetPriceVO toAssetPriceVO(AssetPriceDaily price) {
        return AssetPriceVO.builder()
                .id(price.getId())
                .assetId(price.getAssetId())
                .price(price.getClosePrice())
                .currency(price.getCurrency())
                .previousClose(price.getPreviousClose())
                .changeAmount(price.getChangeAmount())
                .changePercent(price.getChangePercent())
                .source(price.getSource())
                .quoteTime(price.getTradeDate() == null ? null : price.getTradeDate().atStartOfDay())
                .marketStatus("DAILY")
                .build();
    }

    /**
     * 批量读取 current + 最近两个交易日 daily，核心收益计算不再依赖旧原始快照表。
     */
    private Map<Long, HoldingPriceContext> priceContextMap(Set<Long> assetIds) {
        if (assetIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, AssetPriceCurrent> currentPriceMap = quoteService.latestPriceMap(assetIds);
        Map<Long, AssetPriceCurrent> safeCurrentPriceMap = currentPriceMap == null ? Map.of() : currentPriceMap;
        List<AssetPriceDaily> dailyRows = assetPriceDailyMapper.selectList(new LambdaQueryWrapper<AssetPriceDaily>()
                        .in(AssetPriceDaily::getAssetId, assetIds)
                        .lt(AssetPriceDaily::getTradeDate, LocalDate.now())
                        .orderByDesc(AssetPriceDaily::getTradeDate));
        Map<Long, List<AssetPriceDaily>> dailyPriceMap = (dailyRows == null ? List.<AssetPriceDaily>of() : dailyRows)
                .stream()
                .collect(Collectors.groupingBy(AssetPriceDaily::getAssetId));
        return assetIds.stream().collect(Collectors.toMap(assetId -> assetId, assetId -> {
            List<AssetPriceDaily> prices = dailyPriceMap.getOrDefault(assetId, List.of());
            return new HoldingPriceContext(
                    safeCurrentPriceMap.get(assetId),
                    prices.isEmpty() ? null : prices.get(0),
                    prices.size() < 2 ? null : prices.get(1));
        }));
    }

    /**
     * 读取单个资产的价格上下文。
     */
    private HoldingPriceContext priceContext(Long assetId) {
        return priceContextMap(Set.of(assetId)).get(assetId);
    }

    /**
     * 昨日收益使用最近交易日收盘价；日级价缺失时返回 null，由前端展示暂无。
     */
    private BigDecimal yesterdayPreviousPrice(AssetPriceDaily previousDaily) {
        return previousDaily == null ? null : previousDaily.getClosePrice();
    }

    private BigDecimal yesterdayBeforePreviousPrice(AssetPriceDaily beforePreviousDaily) {
        return beforePreviousDaily == null ? null : beforePreviousDaily.getClosePrice();
    }

    private LocalDate previousPriceDate(AssetPriceCurrent matchedPrice, AssetPriceDaily previousDaily) {
        if (previousDaily != null) {
            return previousDaily.getTradeDate();
        }
        return matchedPrice == null || matchedPrice.getQuoteTime() == null ? null : matchedPrice.getQuoteTime().toLocalDate().minusDays(1);
    }

    private LocalDate beforePreviousPriceDate(AssetPriceDaily previousDaily, AssetPriceDaily beforePreviousDaily) {
        if (beforePreviousDaily != null) {
            return beforePreviousDaily.getTradeDate();
        }
        return previousDaily == null ? null : previousDaily.getTradeDate().minusDays(1);
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
     * 今日收益只允许使用今天有效价；CRYPTO 虽然不休市，也不能用昨天残留的当前价冒充今日价格。
     */
    private boolean todayPriceAvailable(Asset asset, LocalDate priceDate) {
        if (asset == null || priceDate == null) {
            return false;
        }
        if (marketClosedForAsset(asset, LocalDate.now())) {
            return false;
        }
        if (ASSET_TYPE_FUND.equals(asset.getType()) || ASSET_TYPE_STOCK.equals(asset.getType())) {
            return LocalDate.now().equals(priceDate);
        }
        return LocalDate.now().equals(priceDate);
    }

    /**
     * 基金和股票按市场日历展示休市；未配置对应市场日历时退回周末规则，避免跨市场节假日误伤。
     */
    private boolean marketClosedForAsset(Asset asset, LocalDate date) {
        return usesCalendarForClosedDisplay(asset) && !tradingDayForAsset(asset, date);
    }

    private boolean tradingDayForAsset(Asset asset, LocalDate date) {
        if (!usesCalendarForClosedDisplay(asset) || date == null) {
            return true;
        }
        String marketCalendar = calendarMarketForAsset(asset);
        if (StringUtils.hasText(marketCalendar)) {
            MarketCalendar calendar = marketCalendarMapper.selectOne(new LambdaQueryWrapper<MarketCalendar>()
                    .eq(MarketCalendar::getMarket, marketCalendar)
                    .eq(MarketCalendar::getTradeDate, date)
                    // 收益日历和今日收益状态必须优先使用人工/交易所休市修正，不能被系统工作日兜底覆盖。
                    .last(CALENDAR_PRIORITY_SQL));
            if (calendar != null) {
                return Boolean.TRUE.equals(calendar.getTradingDay());
            }
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    private boolean usesCalendarForClosedDisplay(Asset asset) {
        return asset != null && (ASSET_TYPE_FUND.equals(asset.getType()) || ASSET_TYPE_STOCK.equals(asset.getType()));
    }

    private String calendarMarketForAsset(Asset asset) {
        if (asset == null) {
            return null;
        }
        if (ASSET_TYPE_FUND.equals(asset.getType())) {
            return "A_SHARE";
        }
        if (!ASSET_TYPE_STOCK.equals(asset.getType())) {
            return null;
        }
        String market = asset.getMarket();
        if ("SH".equalsIgnoreCase(market) || "SZ".equalsIgnoreCase(market) || "BJ".equalsIgnoreCase(market) || "CN".equalsIgnoreCase(market)) {
            return "A_SHARE";
        }
        return StringUtils.hasText(market) ? market.toUpperCase() : null;
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
     * 资产识别结果带回的当前价写入 current/daily；旧价格快照表退役后不再保存审计快照。
     */
    private void saveLookupPriceSnapshot(HoldingRequest request, Asset asset) {
        if (request.getLatestPrice() == null || request.getLatestPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        AssetPriceCurrent price = new AssetPriceCurrent();
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
        upsertCurrentPrice(price);
        upsertLookupDailyPrice(price);
    }

    /**
     * 自动识别带回的初始价同步写 current，保证新增持仓后估值优先使用当前价表。
     */
    private void upsertCurrentPrice(AssetPriceCurrent current) {
        if (assetPriceCurrentMapper.selectById(current.getAssetId()) == null) {
            assetPriceCurrentMapper.insert(current);
            return;
        }
        assetPriceCurrentMapper.updateById(current);
    }

    /**
     * 识别价没有 Redis intraday 明细，按单点日价写 daily，支撑删表后的详情曲线和快照估值。
     */
    private void upsertLookupDailyPrice(AssetPriceCurrent price) {
        if (price.getQuoteTime() == null || price.getPrice() == null) {
            return;
        }
        AssetPriceDaily daily = new AssetPriceDaily();
        daily.setAssetId(price.getAssetId());
        daily.setTradeDate(price.getQuoteTime().toLocalDate());
        daily.setOpenPrice(price.getPrice());
        daily.setClosePrice(price.getPrice());
        daily.setHighPrice(price.getPrice());
        daily.setLowPrice(price.getPrice());
        daily.setPreviousClose(price.getPreviousClose());
        daily.setChangeAmount(price.getChangeAmount());
        daily.setChangePercent(price.getChangePercent());
        daily.setCurrency(price.getCurrency());
        daily.setSource(price.getSource());
        daily.setDeleted(0);
        AssetPriceDaily exists = assetPriceDailyMapper.selectOne(new LambdaQueryWrapper<AssetPriceDaily>()
                .eq(AssetPriceDaily::getAssetId, daily.getAssetId())
                .eq(AssetPriceDaily::getTradeDate, daily.getTradeDate())
                // 初始日级价格只更新有效记录，避免唯一索引中的 deleted 维度和查询口径不一致。
                .eq(AssetPriceDaily::getDeleted, 0)
                .last("limit 1"));
        if (exists == null) {
            assetPriceDailyMapper.insert(daily);
            return;
        }
        daily.setId(exists.getId());
        assetPriceDailyMapper.update(daily, new LambdaUpdateWrapper<AssetPriceDaily>()
                .eq(AssetPriceDaily::getId, exists.getId())
                .eq(AssetPriceDaily::getDeleted, 0));
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
    private boolean priceMatchesAssetCurrency(Asset asset, AssetPriceCurrent price) {
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

    private record HoldingPriceContext(AssetPriceCurrent currentPrice, AssetPriceDaily previousDaily, AssetPriceDaily beforePreviousDaily) {
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
