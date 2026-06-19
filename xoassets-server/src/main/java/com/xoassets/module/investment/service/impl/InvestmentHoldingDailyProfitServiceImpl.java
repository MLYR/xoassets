package com.xoassets.module.investment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.module.exchange.service.ExchangeRateService;
import com.xoassets.module.investment.service.InvestmentHoldingDailyProfitService;
import com.xoassets.module.investment.service.InvestmentPositionHistoryService;
import com.xoassets.module.investment.vo.InvestmentCalendarDayProfitVO;
import com.xoassets.persistence.entity.Asset;
import com.xoassets.persistence.entity.AssetPriceCurrent;
import com.xoassets.persistence.entity.AssetPriceDaily;
import com.xoassets.persistence.entity.Holding;
import com.xoassets.persistence.entity.InvestmentDailySnapshot;
import com.xoassets.persistence.entity.InvestmentHoldingDailyProfit;
import com.xoassets.persistence.entity.InvestmentTransaction;
import com.xoassets.persistence.entity.MarketCalendar;
import com.xoassets.persistence.mapper.AssetMapper;
import com.xoassets.persistence.mapper.AssetPriceCurrentMapper;
import com.xoassets.persistence.mapper.AssetPriceDailyMapper;
import com.xoassets.persistence.mapper.HoldingMapper;
import com.xoassets.persistence.mapper.InvestmentDailySnapshotMapper;
import com.xoassets.persistence.mapper.InvestmentHoldingDailyProfitMapper;
import com.xoassets.persistence.mapper.InvestmentTransactionMapper;
import com.xoassets.persistence.mapper.MarketCalendarMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
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
 * 持仓每日收益服务实现，收益日历、趋势每日收益和昨日收益统一从持久化结果读取。
 */
@Service
public class InvestmentHoldingDailyProfitServiceImpl implements InvestmentHoldingDailyProfitService {

    /**
     * 实时价格估值模式。
     */
    private static final String VALUATION_REALTIME_PRICE = "REALTIME_PRICE";
    /**
     * 日终净值估值模式。
     */
    private static final String VALUATION_END_OF_DAY_NAV = "END_OF_DAY_NAV";
    /**
     * 货币基金估值模式。
     */
    private static final String VALUATION_MONEY_FUND_YIELD = "MONEY_FUND_YIELD";
    /**
     * 全部模块。
     */
    private static final String MODULE_ALL = "ALL";
    /**
     * 正常状态。
     */
    private static final String STATUS_NORMAL = "NORMAL";
    /**
     * 缺价格状态。
     */
    private static final String STATUS_PRICE_MISSING = "PRICE_MISSING";
    /**
     * 清仓后状态。
     */
    private static final String STATUS_CLOSED_OUT = "CLOSED_OUT";
    /**
     * 交易日历优先级排序。
     */
    private static final String CALENDAR_PRIORITY_SQL = "order by case source when 'MANUAL' then 3 when 'EXCHANGE_ANNOUNCEMENT' then 2 when 'SYSTEM_WEEKDAY' then 1 else 0 end desc, id desc limit 1";
    /**
     * 下一交易日查询排序。
     */
    private static final String NEXT_TRADING_DATE_SQL = "order by trade_date asc, case source when 'MANUAL' then 3 when 'EXCHANGE_ANNOUNCEMENT' then 2 when 'SYSTEM_WEEKDAY' then 1 else 0 end desc, id desc limit 370";
    /**
     * 计算版本。
     */
    private static final int CALC_VERSION = 1;

    private final InvestmentHoldingDailyProfitMapper dailyProfitMapper;
    private final InvestmentDailySnapshotMapper dailySnapshotMapper;
    private final HoldingMapper holdingMapper;
    private final AssetMapper assetMapper;
    private final AssetPriceDailyMapper assetPriceDailyMapper;
    private final AssetPriceCurrentMapper assetPriceCurrentMapper;
    private final InvestmentTransactionMapper investmentTransactionMapper;
    private final MarketCalendarMapper marketCalendarMapper;
    private final InvestmentPositionHistoryService positionHistoryService;
    private final ExchangeRateService exchangeRateService;

    /**
     * 注入收益计算依赖。
     */
    public InvestmentHoldingDailyProfitServiceImpl(
            InvestmentHoldingDailyProfitMapper dailyProfitMapper,
            InvestmentDailySnapshotMapper dailySnapshotMapper,
            HoldingMapper holdingMapper,
            AssetMapper assetMapper,
            AssetPriceDailyMapper assetPriceDailyMapper,
            AssetPriceCurrentMapper assetPriceCurrentMapper,
            InvestmentTransactionMapper investmentTransactionMapper,
            MarketCalendarMapper marketCalendarMapper,
            InvestmentPositionHistoryService positionHistoryService,
            ExchangeRateService exchangeRateService) {
        this.dailyProfitMapper = dailyProfitMapper;
        this.dailySnapshotMapper = dailySnapshotMapper;
        this.holdingMapper = holdingMapper;
        this.assetMapper = assetMapper;
        this.assetPriceDailyMapper = assetPriceDailyMapper;
        this.assetPriceCurrentMapper = assetPriceCurrentMapper;
        this.investmentTransactionMapper = investmentTransactionMapper;
        this.marketCalendarMapper = marketCalendarMapper;
        this.positionHistoryService = positionHistoryService;
        this.exchangeRateService = exchangeRateService;
    }

    /**
     * 重建指定用户区间内所有持仓的每日收益。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void rebuildForUser(Long userId, LocalDate startDate, LocalDate endDate) {
        if (userId == null || startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return;
        }
        List<Holding> holdings = holdingMapper.selectList(new LambdaQueryWrapper<Holding>()
                .eq(Holding::getUserId, userId)
                .eq(Holding::getStatus, 1));
        if (holdings == null || holdings.isEmpty()) {
            syncSnapshotCalendarProfit(userId, startDate, endDate);
            return;
        }
        Set<Long> assetIds = holdings.stream().map(Holding::getAssetId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Asset> assetMap = assetIds.isEmpty()
                ? Map.of()
                : assetMapper.selectBatchIds(assetIds).stream().collect(Collectors.toMap(Asset::getId, asset -> asset));
        for (Holding holding : holdings) {
            Asset asset = assetMap.get(holding.getAssetId());
            if (asset == null) {
                continue;
            }
            rebuildHolding(userId, holding, asset, startDate, endDate);
        }
        syncSnapshotCalendarProfit(userId, startDate, endDate);
    }

    /**
     * 行情刷新后按资产重建受影响用户的展示日收益。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void rebuildForAsset(Long assetId, LocalDate priceDate) {
        if (assetId == null || priceDate == null) {
            return;
        }
        List<Holding> holdings = holdingMapper.selectList(new LambdaQueryWrapper<Holding>()
                .eq(Holding::getAssetId, assetId)
                .eq(Holding::getStatus, 1));
        if (holdings == null || holdings.isEmpty()) {
            return;
        }
        Asset asset = assetMapper.selectById(assetId);
        if (asset == null) {
            return;
        }
        LocalDate displayDate = calendarDisplayDate(deriveAssetMeta(asset), priceDate);
        for (Holding holding : holdings) {
            rebuildHolding(holding.getUserId(), holding, asset, displayDate, displayDate);
        }
        holdings.stream()
                .map(Holding::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(userId -> syncSnapshotCalendarProfit(userId, displayDate, displayDate));
    }

    /**
     * 当前月页面读取前兜底生成，避免日历只能等晚间快照任务才有今日收益。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void ensureCurrentMonthForUser(Long userId, YearMonth month) {
        YearMonth currentMonth = YearMonth.now();
        if (userId == null || month == null || !currentMonth.equals(month)) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate start = currentMonth.atDay(1);
        Long monthRows = dailyProfitMapper.selectCount(new LambdaQueryWrapper<InvestmentHoldingDailyProfit>()
                .eq(InvestmentHoldingDailyProfit::getUserId, userId)
                .between(InvestmentHoldingDailyProfit::getDisplayDate, start, today)
                .eq(InvestmentHoldingDailyProfit::getDeleted, 0));
        Long todayRows = dailyProfitMapper.selectCount(new LambdaQueryWrapper<InvestmentHoldingDailyProfit>()
                .eq(InvestmentHoldingDailyProfit::getUserId, userId)
                .eq(InvestmentHoldingDailyProfit::getDisplayDate, today)
                .eq(InvestmentHoldingDailyProfit::getDeleted, 0));
        if (monthRows == null || monthRows == 0L) {
            // 新增表或历史迁移后首次打开页面时，补齐当月至今天，避免日历整月空白。
            rebuildForUser(userId, start, today);
            return;
        }
        if (todayRows == null || todayRows == 0L) {
            rebuildForUser(userId, today, today);
        }
    }

    /**
     * 查询单持仓收益日历。
     */
    @Override
    public List<InvestmentCalendarDayProfitVO> holdingCalendar(Long userId, Long holdingId, YearMonth month) {
        Holding holding = holdingMapper.selectOne(new LambdaQueryWrapper<Holding>()
                .eq(Holding::getUserId, userId)
                .eq(Holding::getId, holdingId));
        if (holding == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "持仓不存在");
        }
        Asset asset = assetMapper.selectById(holding.getAssetId());
        YearMonth targetMonth = month == null ? YearMonth.now() : month;
        ensureCurrentMonthForUser(userId, targetMonth);
        LocalDate start = targetMonth.atDay(1);
        LocalDate end = targetMonth.atEndOfMonth();
        Map<LocalDate, InvestmentHoldingDailyProfit> rows = dailyProfitMapper.selectList(new LambdaQueryWrapper<InvestmentHoldingDailyProfit>()
                        .eq(InvestmentHoldingDailyProfit::getUserId, userId)
                        .eq(InvestmentHoldingDailyProfit::getHoldingId, holdingId)
                        .between(InvestmentHoldingDailyProfit::getDisplayDate, start, end)
                        .eq(InvestmentHoldingDailyProfit::getDeleted, 0)
                        .orderByAsc(InvestmentHoldingDailyProfit::getDisplayDate))
                .stream()
                .collect(Collectors.toMap(InvestmentHoldingDailyProfit::getDisplayDate, row -> row, (left, right) -> left, LinkedHashMap::new));
        List<InvestmentCalendarDayProfitVO> result = new ArrayList<>();
        AssetMeta assetMeta = deriveAssetMeta(asset);
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            InvestmentHoldingDailyProfit row = rows.get(date);
            boolean tradingDay = tradingDayForAsset(asset, date);
            boolean marketClosed = marketClosedForAsset(asset, date);
            result.add(InvestmentCalendarDayProfitVO.builder()
                    .date(date)
                    .profitAmount(row == null || marketClosed ? null : row.getProfitAmount())
                    .profitRate(row == null || marketClosed ? null : row.getProfitRate())
                    .marketValue(row == null || marketClosed ? null : row.getMarketValue())
                    .price(row == null || marketClosed ? null : row.getPrice())
                    .previousPrice(row == null || marketClosed ? null : row.getPreviousPrice())
                    .hasPrice(row != null && !marketClosed && STATUS_NORMAL.equals(row.getStatus()))
                    .tradingDay(tradingDay)
                    .marketClosed(marketClosed)
                    .statusLabel(marketClosed ? "休市" : row == null ? "无价格" : row.getStatusLabel())
                    .priceLabel(priceLabel(assetMeta))
                    .build());
        }
        return result;
    }

    /**
     * 查询全持仓收益日历。
     */
    @Override
    public List<InvestmentCalendarDayProfitVO> userCalendar(Long userId, YearMonth month) {
        YearMonth targetMonth = month == null ? YearMonth.now() : month;
        ensureCurrentMonthForUser(userId, targetMonth);
        LocalDate start = targetMonth.atDay(1);
        LocalDate end = targetMonth.atEndOfMonth();
        Map<LocalDate, DailyProfitSummary> rows = aggregateByDate(userId, start, end);
        List<Asset> activeAssets = activeCalendarAssets(userId);
        List<InvestmentCalendarDayProfitVO> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            DailyProfitSummary row = rows.get(date);
            boolean marketClosed = row == null && calendarClosedForAllAssets(activeAssets, date);
            result.add(InvestmentCalendarDayProfitVO.builder()
                    .date(date)
                    .profitAmount(row == null ? null : scale4(row.profit()))
                    .profitRate(row == null ? null : nullableRate(row.profit(), row.baseAmount()))
                    .hasPrice(row != null)
                    .tradingDay(!marketClosed)
                    .marketClosed(marketClosed)
                    .statusLabel(marketClosed ? "休市" : row == null ? "无价格" : "有收益")
                    .priceLabel("每日收益")
                    .build());
        }
        return result;
    }

    /**
     * 查询用户当前仍在统计范围内的投资资产，用于全持仓日历补齐休市状态。
     */
    private List<Asset> activeCalendarAssets(Long userId) {
        if (userId == null) {
            return List.of();
        }
        List<Holding> holdings = holdingMapper.selectList(new LambdaQueryWrapper<Holding>()
                .eq(Holding::getUserId, userId)
                .eq(Holding::getStatus, 1));
        if (holdings == null || holdings.isEmpty()) {
            return List.of();
        }
        Set<Long> assetIds = holdings.stream().map(Holding::getAssetId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (assetIds.isEmpty()) {
            return List.of();
        }
        return assetMapper.selectBatchIds(assetIds);
    }

    /**
     * 全持仓日历只有在所有当前统计资产都休市时，才展示为休市；含虚拟货币时不整体休市。
     */
    private boolean calendarClosedForAllAssets(List<Asset> assets, LocalDate date) {
        if (assets == null || assets.isEmpty()) {
            return false;
        }
        return assets.stream().allMatch(asset -> marketClosedForAsset(asset, date));
    }

    /**
     * 按展示日聚合全持仓收益。
     */
    @Override
    public Map<LocalDate, DailyProfitSummary> aggregateByDate(Long userId, LocalDate startDate, LocalDate endDate) {
        return aggregateByModuleAndDate(userId, startDate, endDate).getOrDefault(MODULE_ALL, Map.of());
    }

    /**
     * 按模块和展示日聚合持久化收益，供数据分析模块趋势复用同一收益口径。
     */
    @Override
    public Map<String, Map<LocalDate, DailyProfitSummary>> aggregateByModuleAndDate(Long userId, LocalDate startDate, LocalDate endDate) {
        return aggregatePersistedByModuleAndDate(userId, startDate, endDate);
    }

    /**
     * 查询最近收益日的 ALL 和模块汇总。
     */
    @Override
    public Map<String, DailyProfitSummary> latestByModuleBefore(Long userId, LocalDate date) {
        ensureCurrentMonthForUser(userId, YearMonth.now());
        Map<String, Map<LocalDate, DailyProfitSummary>> rows = aggregatePersistedByModuleAndDate(userId, date.minusDays(40), date.minusDays(1));
        Map<String, DailyProfitSummary> result = new HashMap<>();
        rows.forEach((module, dailyMap) -> dailyMap.entrySet().stream()
                .max(Map.Entry.comparingByKey())
                .ifPresent(entry -> result.put(module, entry.getValue())));
        return result;
    }

    /**
     * 查询单持仓最近收益日。
     */
    @Override
    public HoldingDailyProfitSummary latestHoldingBefore(Long userId, Long holdingId, LocalDate date) {
        ensureCurrentMonthForUser(userId, YearMonth.now());
        InvestmentHoldingDailyProfit row = dailyProfitMapper.selectOne(new LambdaQueryWrapper<InvestmentHoldingDailyProfit>()
                .eq(InvestmentHoldingDailyProfit::getUserId, userId)
                .eq(InvestmentHoldingDailyProfit::getHoldingId, holdingId)
                .lt(InvestmentHoldingDailyProfit::getDisplayDate, date)
                .eq(InvestmentHoldingDailyProfit::getStatus, STATUS_NORMAL)
                .eq(InvestmentHoldingDailyProfit::getDeleted, 0)
                .isNotNull(InvestmentHoldingDailyProfit::getProfitAmount)
                .orderByDesc(InvestmentHoldingDailyProfit::getDisplayDate)
                .last("limit 1"));
        return row == null ? null : new HoldingDailyProfitSummary(row.getProfitAmount(), row.getBaseAmount(), row.getPrice(), row.getPreviousPrice());
    }

    /**
     * 重建单持仓区间每日收益。
     */
    private void rebuildHolding(Long userId, Holding holding, Asset asset, LocalDate start, LocalDate end) {
        AssetMeta assetMeta = deriveAssetMeta(asset);
        List<DailyPricePoint> prices = calendarPricePoints(asset, holding.getAssetId(), end);
        DailyPricePoint previous = null;
        boolean manualOnly = effectiveTransactionCount(userId, holding.getId()) == 0;
        for (DailyPricePoint price : prices) {
            BigDecimal previousPrice = price.previousClose() != null ? price.previousClose() : previous == null ? null : previous.closePrice();
            LocalDate previousPriceDate = previous == null ? price.tradeDate().minusDays(1) : previous.tradeDate();
            if (previousPrice != null) {
                LocalDate displayDate = calendarDisplayDate(assetMeta, price.tradeDate());
                if (!displayDate.isBefore(start) && !displayDate.isAfter(end)) {
                    LocalDate quantityDate = calendarProfitBaseQuantityDate(assetMeta, previousPriceDate, displayDate);
                    LocalDate endQuantityDate = calendarProfitEndQuantityDate(assetMeta, price.tradeDate(), displayDate);
                    DailySegmentProfitData segmentProfit = dailySegmentProfit(userId, holding, assetMeta, quantityDate, endQuantityDate, price.closePrice(), previousPrice, manualOnly);
                    upsert(toRow(userId, holding, asset, assetMeta, displayDate, price, previousPriceDate, previousPrice, quantityDate, segmentProfit));
                }
            }
            previous = price;
        }
    }

    /**
     * 转换为持仓每日收益行。
     */
    private InvestmentHoldingDailyProfit toRow(Long userId, Holding holding, Asset asset, AssetMeta assetMeta, LocalDate displayDate,
                                               DailyPricePoint price, LocalDate previousPriceDate, BigDecimal previousPrice,
                                               LocalDate quantityDate, DailySegmentProfitData segmentProfit) {
        BigDecimal baseAmount = segmentProfit.baseAmount();
        BigDecimal profit = segmentProfit.profit();
        InvestmentHoldingDailyProfit row = new InvestmentHoldingDailyProfit();
        row.setUserId(userId);
        row.setHoldingId(holding.getId());
        row.setAssetId(holding.getAssetId());
        row.setAssetType(asset.getType());
        row.setModule(moduleOf(asset.getType()));
        row.setDisplayDate(displayDate);
        row.setPriceDate(price.tradeDate());
        row.setPreviousPriceDate(previousPriceDate);
        row.setQuantityDate(quantityDate);
        row.setQuantity(scaleQuantity(segmentProfit.endQuantity()));
        row.setPrice(scale8(price.closePrice()));
        row.setPreviousPrice(scale8(previousPrice));
        row.setProfitAmount(profit == null ? null : scale4(profit));
        row.setProfitRate(nullableRate(profit, baseAmount));
        row.setBaseAmount(baseAmount == null ? null : scale4(baseAmount));
        row.setMarketValue(scaleQuantity(segmentProfit.endQuantity()).multiply(price.closePrice()).setScale(4, RoundingMode.HALF_UP));
        row.setCurrency(StringUtils.hasText(asset.getCurrency()) ? asset.getCurrency() : "CNY");
        row.setStatus(segmentProfit.status());
        row.setStatusLabel(segmentProfit.statusLabel());
        row.setCalcVersion(CALC_VERSION);
        row.setDeleted(0);
        return row;
    }

    /**
     * 按模块和日期聚合持久化收益。
     */
    private Map<String, Map<LocalDate, DailyProfitSummary>> aggregatePersistedByModuleAndDate(Long userId, LocalDate start, LocalDate end) {
        List<InvestmentHoldingDailyProfit> rows = dailyProfitMapper.selectList(new LambdaQueryWrapper<InvestmentHoldingDailyProfit>()
                .eq(InvestmentHoldingDailyProfit::getUserId, userId)
                .between(InvestmentHoldingDailyProfit::getDisplayDate, start, end)
                .eq(InvestmentHoldingDailyProfit::getStatus, STATUS_NORMAL)
                .eq(InvestmentHoldingDailyProfit::getDeleted, 0)
                .isNotNull(InvestmentHoldingDailyProfit::getProfitAmount));
        Map<String, Map<LocalDate, BigDecimal>> profitByModule = new HashMap<>();
        Map<String, Map<LocalDate, BigDecimal>> baseAmountByModule = new HashMap<>();
        for (InvestmentHoldingDailyProfit row : rows == null ? List.<InvestmentHoldingDailyProfit>of() : rows) {
            BigDecimal cnyProfit = amountToCny(row.getProfitAmount(), row.getCurrency());
            BigDecimal cnyBase = amountToCny(nullToZero(row.getBaseAmount()), row.getCurrency());
            merge(profitByModule, baseAmountByModule, MODULE_ALL, row.getDisplayDate(), cnyProfit, cnyBase);
            merge(profitByModule, baseAmountByModule, row.getModule(), row.getDisplayDate(), cnyProfit, cnyBase);
        }
        Map<String, Map<LocalDate, DailyProfitSummary>> result = new HashMap<>();
        profitByModule.forEach((module, profitByDate) -> {
            Map<LocalDate, DailyProfitSummary> daily = new HashMap<>();
            profitByDate.forEach((date, profit) -> daily.put(date, new DailyProfitSummary(
                    scale4(profit),
                    scale4(baseAmountByModule.getOrDefault(module, Map.of()).get(date)))));
            result.put(module, daily);
        });
        return result;
    }

    /**
     * 同步已存在投资日快照的真实日历收益字段，避免趋势图继续读到旧聚合值。
     */
    private void syncSnapshotCalendarProfit(Long userId, LocalDate start, LocalDate end) {
        Map<LocalDate, DailyProfitSummary> dailyProfitMap = aggregateByDate(userId, start, end);
        List<InvestmentDailySnapshot> snapshots = dailySnapshotMapper.selectList(new LambdaQueryWrapper<InvestmentDailySnapshot>()
                .eq(InvestmentDailySnapshot::getUserId, userId)
                .between(InvestmentDailySnapshot::getSnapshotDate, start, end)
                .eq(InvestmentDailySnapshot::getDeleted, 0));
        for (InvestmentDailySnapshot snapshot : snapshots == null ? List.<InvestmentDailySnapshot>of() : snapshots) {
            DailyProfitSummary summary = dailyProfitMap.get(snapshot.getSnapshotDate());
            // 使用显式 set，确保没有收益行时也能清空旧快照里的日历收益字段。
            dailySnapshotMapper.update(null, new LambdaUpdateWrapper<InvestmentDailySnapshot>()
                    .eq(InvestmentDailySnapshot::getId, snapshot.getId())
                    .eq(InvestmentDailySnapshot::getDeleted, 0)
                    .set(InvestmentDailySnapshot::getCalendarProfit, summary == null ? null : scale4(summary.profit()))
                    .set(InvestmentDailySnapshot::getCalendarBaseAmount, summary == null ? null : scale4(summary.baseAmount()))
                    .set(InvestmentDailySnapshot::getCalendarProfitRate, summary == null ? null : nullableRate(summary.profit(), summary.baseAmount()))
                    .set(InvestmentDailySnapshot::getUpdatedAt, LocalDateTime.now()));
        }
    }

    /**
     * 合并聚合金额。
     */
    private void merge(Map<String, Map<LocalDate, BigDecimal>> profitByModule,
                       Map<String, Map<LocalDate, BigDecimal>> baseAmountByModule,
                       String module,
                       LocalDate date,
                       BigDecimal profit,
                       BigDecimal baseAmount) {
        profitByModule.computeIfAbsent(module, key -> new HashMap<>()).merge(date, profit, BigDecimal::add);
        baseAmountByModule.computeIfAbsent(module, key -> new HashMap<>()).merge(date, baseAmount, BigDecimal::add);
    }

    /**
     * 新增或更新每日收益行。
     */
    private void upsert(InvestmentHoldingDailyProfit row) {
        InvestmentHoldingDailyProfit exists = dailyProfitMapper.selectOne(new LambdaQueryWrapper<InvestmentHoldingDailyProfit>()
                .eq(InvestmentHoldingDailyProfit::getHoldingId, row.getHoldingId())
                .eq(InvestmentHoldingDailyProfit::getPriceDate, row.getPriceDate())
                .eq(InvestmentHoldingDailyProfit::getDeleted, 0)
                .last("limit 1"));
        if (exists == null) {
            dailyProfitMapper.insert(row);
            return;
        }
        // 显式 set 可空字段，避免清仓日收益重建时旧 profit_rate / market_value 残留。
        dailyProfitMapper.update(null, new LambdaUpdateWrapper<InvestmentHoldingDailyProfit>()
                .eq(InvestmentHoldingDailyProfit::getId, exists.getId())
                .eq(InvestmentHoldingDailyProfit::getDeleted, 0)
                .set(InvestmentHoldingDailyProfit::getUserId, row.getUserId())
                .set(InvestmentHoldingDailyProfit::getHoldingId, row.getHoldingId())
                .set(InvestmentHoldingDailyProfit::getAssetId, row.getAssetId())
                .set(InvestmentHoldingDailyProfit::getAssetType, row.getAssetType())
                .set(InvestmentHoldingDailyProfit::getModule, row.getModule())
                .set(InvestmentHoldingDailyProfit::getDisplayDate, row.getDisplayDate())
                .set(InvestmentHoldingDailyProfit::getPriceDate, row.getPriceDate())
                .set(InvestmentHoldingDailyProfit::getPreviousPriceDate, row.getPreviousPriceDate())
                .set(InvestmentHoldingDailyProfit::getQuantityDate, row.getQuantityDate())
                .set(InvestmentHoldingDailyProfit::getQuantity, row.getQuantity())
                .set(InvestmentHoldingDailyProfit::getPrice, row.getPrice())
                .set(InvestmentHoldingDailyProfit::getPreviousPrice, row.getPreviousPrice())
                .set(InvestmentHoldingDailyProfit::getProfitAmount, row.getProfitAmount())
                .set(InvestmentHoldingDailyProfit::getProfitRate, row.getProfitRate())
                .set(InvestmentHoldingDailyProfit::getBaseAmount, row.getBaseAmount())
                .set(InvestmentHoldingDailyProfit::getMarketValue, row.getMarketValue())
                .set(InvestmentHoldingDailyProfit::getCurrency, row.getCurrency())
                .set(InvestmentHoldingDailyProfit::getStatus, row.getStatus())
                .set(InvestmentHoldingDailyProfit::getStatusLabel, row.getStatusLabel())
                .set(InvestmentHoldingDailyProfit::getCalcVersion, row.getCalcVersion())
                .set(InvestmentHoldingDailyProfit::getUpdatedAt, LocalDateTime.now())
                .set(InvestmentHoldingDailyProfit::getDeleted, row.getDeleted()));
    }

    /**
     * 收益日历以日级价格为主，并合并当前价兜住当天聚合滞后。
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
        AssetPriceCurrent current = assetPriceCurrentMapper.selectById(assetId);
        if (priceMatchesAssetCurrency(asset, current) && current.getPrice() != null && current.getQuoteTime() != null && !current.getQuoteTime().toLocalDate().isAfter(end)) {
            DailyPricePoint existingPoint = priceMap.get(current.getQuoteTime().toLocalDate());
            BigDecimal previousClose = current.getPreviousClose() == null && existingPoint != null ? existingPoint.previousClose() : current.getPreviousClose();
            priceMap.put(current.getQuoteTime().toLocalDate(), new DailyPricePoint(current.getQuoteTime().toLocalDate(), current.getPrice(), previousClose));
        }
        return priceMap.values().stream()
                .sorted(Comparator.comparing(DailyPricePoint::tradeDate))
                .toList();
    }

    /**
     * 计算单日分段收益。
     */
    private DailySegmentProfitData dailySegmentProfit(Long userId, Holding holding, AssetMeta assetMeta, LocalDate previousPriceDate, LocalDate priceDate,
                                                      BigDecimal currentPrice, BigDecimal previousPrice, boolean manualOnly) {
        BigDecimal baseQuantity = calendarQuantity(userId, holding, previousPriceDate, manualOnly);
        BigDecimal endQuantity = calendarQuantity(userId, holding, priceDate, manualOnly);
        if (closedOutBeforePriceDate(userId, holding, priceDate, endQuantity)) {
            // 清仓日之后刷新行情只说明公共价格变了，不能继续用清仓前历史份额生成该持仓收益。
            return new DailySegmentProfitData(null, null, endQuantity, STATUS_CLOSED_OUT, "已清仓");
        }
        if (assetMeta == null || !VALUATION_REALTIME_PRICE.equals(assetMeta.valuationMode()) || currentPrice == null || previousPrice == null) {
            BigDecimal profit = priceDiffProfit(baseQuantity, currentPrice, previousPrice);
            return normalSegment(profit, baseQuantity.multiply(nullToZero(previousPrice)).setScale(4, RoundingMode.HALF_UP), endQuantity);
        }
        BigDecimal remainingOldQuantity = baseQuantity;
        BigDecimal profit = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal baseAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        List<IntradayLot> buyLots = new ArrayList<>();
        for (InvestmentTransaction transaction : transactionsOnDate(userId, holding.getId(), priceDate)) {
            BigDecimal quantity = scaleQuantity(transaction.getQuantity());
            BigDecimal transactionPrice = transactionPrice(transaction);
            if (quantity.compareTo(BigDecimal.ZERO) <= 0 || transactionPrice == null) {
                continue;
            }
            if ("BUY".equals(transaction.getType())) {
                buyLots.add(new IntradayLot(quantity, transactionPrice));
                continue;
            }
            if ("SELL".equals(transaction.getType())) {
                BigDecimal sellQuantity = quantity;
                BigDecimal oldSellQuantity = remainingOldQuantity.min(sellQuantity);
                if (oldSellQuantity.compareTo(BigDecimal.ZERO) > 0) {
                    profit = profit.add(oldSellQuantity.multiply(transactionPrice.subtract(previousPrice))).setScale(4, RoundingMode.HALF_UP);
                    baseAmount = baseAmount.add(oldSellQuantity.multiply(previousPrice)).setScale(4, RoundingMode.HALF_UP);
                    remainingOldQuantity = remainingOldQuantity.subtract(oldSellQuantity).setScale(10, RoundingMode.HALF_UP);
                    sellQuantity = sellQuantity.subtract(oldSellQuantity).setScale(10, RoundingMode.HALF_UP);
                }
                IntradayConsumeData consumeData = consumeIntradayBuyLots(buyLots, sellQuantity, transactionPrice);
                profit = profit.add(consumeData.profit()).setScale(4, RoundingMode.HALF_UP);
                baseAmount = baseAmount.add(consumeData.baseAmount()).setScale(4, RoundingMode.HALF_UP);
            }
        }
        profit = profit.add(remainingOldQuantity.multiply(currentPrice.subtract(previousPrice))).setScale(4, RoundingMode.HALF_UP);
        baseAmount = baseAmount.add(remainingOldQuantity.multiply(previousPrice)).setScale(4, RoundingMode.HALF_UP);
        for (IntradayLot lot : buyLots) {
            profit = profit.add(lot.quantity().multiply(currentPrice.subtract(lot.price()))).setScale(4, RoundingMode.HALF_UP);
            baseAmount = baseAmount.add(lot.quantity().multiply(lot.price())).setScale(4, RoundingMode.HALF_UP);
        }
        return normalSegment(profit, baseAmount, endQuantity);
    }

    /**
     * 构造正常收益段。
     */
    private DailySegmentProfitData normalSegment(BigDecimal profit, BigDecimal baseAmount, BigDecimal endQuantity) {
        return new DailySegmentProfitData(profit, baseAmount, endQuantity, STATUS_NORMAL, "有收益");
    }

    /**
     * 判断是否已经在价格日前清仓；清仓当天仍允许按卖出价计算当日收益。
     */
    private boolean closedOutBeforePriceDate(Long userId, Holding holding, LocalDate priceDate, BigDecimal endQuantity) {
        return priceDate != null
                && scaleQuantity(holding.getQuantity()).compareTo(BigDecimal.ZERO) <= 0
                && scaleQuantity(endQuantity).compareTo(BigDecimal.ZERO) <= 0
                && transactionsOnDate(userId, holding.getId(), priceDate).isEmpty();
    }

    /**
     * 查询指定生效日交易。
     */
    private List<InvestmentTransaction> transactionsOnDate(Long userId, Long holdingId, LocalDate date) {
        if (userId == null || holdingId == null || date == null) {
            return List.of();
        }
        List<InvestmentTransaction> transactions = investmentTransactionMapper.selectList(new LambdaQueryWrapper<InvestmentTransaction>()
                .eq(InvestmentTransaction::getUserId, userId)
                .eq(InvestmentTransaction::getHoldingId, holdingId)
                .orderByAsc(InvestmentTransaction::getTransactionTime)
                .orderByAsc(InvestmentTransaction::getId));
        return (transactions == null ? List.<InvestmentTransaction>of() : transactions).stream()
                .filter(this::isEffectiveInvestmentTransaction)
                .filter(transaction -> date.equals(effectiveTransactionDate(transaction)))
                .toList();
    }

    /**
     * 计算日历日期持仓数量。
     */
    private BigDecimal calendarQuantity(Long userId, Holding holding, LocalDate previousPriceDate, boolean manualOnly) {
        BigDecimal quantity = positionHistoryService.quantityAt(userId, holding.getId(), holding.getAssetId(), previousPriceDate);
        if (quantity == null) {
            quantity = BigDecimal.ZERO;
        }
        if (manualOnly && quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return scaleQuantity(holding.getQuantity());
        }
        return quantity;
    }

    /**
     * 扣减同日买入批次。
     */
    private IntradayConsumeData consumeIntradayBuyLots(List<IntradayLot> buyLots, BigDecimal sellQuantity, BigDecimal sellPrice) {
        BigDecimal profit = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal baseAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        for (int i = 0; i < buyLots.size() && sellQuantity.compareTo(BigDecimal.ZERO) > 0; i++) {
            IntradayLot lot = buyLots.get(i);
            BigDecimal matchedQuantity = lot.quantity().min(sellQuantity);
            if (matchedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            profit = profit.add(matchedQuantity.multiply(sellPrice.subtract(lot.price()))).setScale(4, RoundingMode.HALF_UP);
            baseAmount = baseAmount.add(matchedQuantity.multiply(lot.price())).setScale(4, RoundingMode.HALF_UP);
            buyLots.set(i, new IntradayLot(lot.quantity().subtract(matchedQuantity).setScale(10, RoundingMode.HALF_UP), lot.price()));
            sellQuantity = sellQuantity.subtract(matchedQuantity).setScale(10, RoundingMode.HALF_UP);
        }
        buyLots.removeIf(lot -> lot.quantity().compareTo(BigDecimal.ZERO) <= 0);
        return new IntradayConsumeData(profit, baseAmount);
    }

    /**
     * 判断交易是否有效。
     */
    private boolean isEffectiveInvestmentTransaction(InvestmentTransaction transaction) {
        return "NORMAL".equals(transaction.getStatus()) || "CONFIRMED".equals(transaction.getStatus());
    }

    /**
     * 计算交易生效日期。
     */
    private LocalDate effectiveTransactionDate(InvestmentTransaction transaction) {
        if ("AMOUNT_NAV".equals(transaction.getInputMode()) && transaction.getConfirmedDate() != null) {
            return transaction.getConfirmedDate();
        }
        if (transaction.getTradeDate() != null) {
            return transaction.getTradeDate();
        }
        return transaction.getTransactionTime().toLocalDate();
    }

    /**
     * 取成交价。
     */
    private BigDecimal transactionPrice(InvestmentTransaction transaction) {
        if (transaction.getPrice() != null) {
            return scale4(transaction.getPrice());
        }
        if (transaction.getTradePrice() != null) {
            return scale4(transaction.getTradePrice());
        }
        BigDecimal quantity = scaleQuantity(transaction.getQuantity());
        return quantity.compareTo(BigDecimal.ZERO) <= 0 || transaction.getAmount() == null
                ? null
                : scale4(transaction.getAmount()).divide(quantity, 4, RoundingMode.HALF_UP);
    }

    /**
     * 统计有效交易数量。
     */
    private long effectiveTransactionCount(Long userId, Long holdingId) {
        Long count = investmentTransactionMapper.selectCount(new LambdaQueryWrapper<InvestmentTransaction>()
                .eq(InvestmentTransaction::getUserId, userId)
                .eq(InvestmentTransaction::getHoldingId, holdingId)
                .in(InvestmentTransaction::getStatus, List.of("NORMAL", "CONFIRMED")));
        return count == null ? 0L : count;
    }

    /**
     * 收益日历展示日期。
     */
    private LocalDate calendarDisplayDate(AssetMeta assetMeta, LocalDate priceDate) {
        if (assetMeta != null && (VALUATION_END_OF_DAY_NAV.equals(assetMeta.valuationMode()) || VALUATION_MONEY_FUND_YIELD.equals(assetMeta.valuationMode()))) {
            return nextTradingDate(priceDate);
        }
        return priceDate;
    }

    /**
     * 收益基准数量日期。
     */
    private LocalDate calendarProfitBaseQuantityDate(AssetMeta assetMeta, LocalDate previousPriceDate, LocalDate displayDate) {
        if (assetMeta != null && (VALUATION_END_OF_DAY_NAV.equals(assetMeta.valuationMode()) || VALUATION_MONEY_FUND_YIELD.equals(assetMeta.valuationMode()))) {
            // 净值型基金的收益展示在净值日后的交易日；确认日新生效份额也应参与当天展示收益。
            return displayDate;
        }
        return previousPriceDate;
    }

    /**
     * 收益展示日持仓数量日期。
     */
    private LocalDate calendarProfitEndQuantityDate(AssetMeta assetMeta, LocalDate priceDate, LocalDate displayDate) {
        if (assetMeta != null && (VALUATION_END_OF_DAY_NAV.equals(assetMeta.valuationMode()) || VALUATION_MONEY_FUND_YIELD.equals(assetMeta.valuationMode()))) {
            // 与基准份额日期保持一致，避免确认日收益漏掉刚确认的基金份额。
            return displayDate;
        }
        return priceDate;
    }

    /**
     * 查询下一交易日。
     */
    private LocalDate nextTradingDate(LocalDate date) {
        List<MarketCalendar> calendars = marketCalendarMapper.selectList(new LambdaQueryWrapper<MarketCalendar>()
                .eq(MarketCalendar::getMarket, "A_SHARE")
                .gt(MarketCalendar::getTradeDate, date)
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

    /**
     * 判断资产在指定日期是否休市。
     */
    private boolean marketClosedForAsset(Asset asset, LocalDate date) {
        return asset != null && ("FUND".equals(asset.getType()) || "STOCK".equals(asset.getType())) && !tradingDayForAsset(asset, date);
    }

    /**
     * 判断资产交易日。
     */
    private boolean tradingDayForAsset(Asset asset, LocalDate date) {
        if (asset == null || date == null || "CRYPTO".equals(asset.getType())) {
            return true;
        }
        if (!"FUND".equals(asset.getType()) && !"STOCK".equals(asset.getType())) {
            return true;
        }
        String market = "FUND".equals(asset.getType()) || "CN_FUND".equals(asset.getMarket()) ? "A_SHARE" : asset.getMarket();
        MarketCalendar calendar = marketCalendarMapper.selectOne(new LambdaQueryWrapper<MarketCalendar>()
                .eq(MarketCalendar::getMarket, StringUtils.hasText(market) ? market : "A_SHARE")
                .eq(MarketCalendar::getTradeDate, date)
                .last(CALENDAR_PRIORITY_SQL));
        if (calendar != null) {
            return Boolean.TRUE.equals(calendar.getTradingDay());
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    /**
     * 推断资产估值模式。
     */
    private AssetMeta deriveAssetMeta(Asset asset) {
        if (asset == null) {
            return new AssetMeta("OTHER", VALUATION_REALTIME_PRICE);
        }
        if ("FUND".equals(asset.getType())) {
            return new AssetMeta("FUND", VALUATION_END_OF_DAY_NAV);
        }
        return new AssetMeta(asset.getType(), VALUATION_REALTIME_PRICE);
    }

    /**
     * 模块归类。
     */
    private String moduleOf(String assetType) {
        if ("FUND".equals(assetType) || "STOCK".equals(assetType) || "CRYPTO".equals(assetType)) {
            return assetType;
        }
        return "OTHER";
    }

    /**
     * 价格文案。
     */
    private String priceLabel(AssetMeta assetMeta) {
        return assetMeta != null && (VALUATION_END_OF_DAY_NAV.equals(assetMeta.valuationMode()) || VALUATION_MONEY_FUND_YIELD.equals(assetMeta.valuationMode()))
                ? "单位净值"
                : "价格";
    }

    /**
     * 价格差收益。
     */
    private BigDecimal priceDiffProfit(BigDecimal quantity, BigDecimal latestPrice, BigDecimal previousPrice) {
        if (latestPrice == null || previousPrice == null) {
            return null;
        }
        return quantity.multiply(latestPrice.subtract(previousPrice)).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 聚合展示统一使用人民币。
     */
    private BigDecimal amountToCny(BigDecimal amount, String currency) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        String normalizedCurrency = StringUtils.hasText(currency) ? currency.trim().toUpperCase() : "CNY";
        if ("CNY".equals(normalizedCurrency)) {
            return amount;
        }
        if ("USD".equals(normalizedCurrency)) {
            return amount.multiply(exchangeRateService.usdCny().getRate()).setScale(4, RoundingMode.HALF_UP);
        }
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "暂不支持 " + normalizedCurrency + " 聚合换算，请先使用 CNY 或 USD");
    }

    /**
     * 匹配日级价币种。
     */
    private boolean priceMatchesAssetCurrency(Asset asset, AssetPriceDaily price) {
        return asset == null || price == null || !StringUtils.hasText(price.getCurrency()) || price.getCurrency().equals(asset.getCurrency());
    }

    /**
     * 匹配当前价币种。
     */
    private boolean priceMatchesAssetCurrency(Asset asset, AssetPriceCurrent price) {
        return asset == null || price == null || !StringUtils.hasText(price.getCurrency()) || price.getCurrency().equals(asset.getCurrency());
    }

    private BigDecimal scale4(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal scale8(BigDecimal value) {
        return value == null ? null : value.setScale(8, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleQuantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(10, RoundingMode.HALF_UP);
    }

    private BigDecimal nullableRate(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record AssetMeta(String assetType, String valuationMode) {
    }

    private record DailyPricePoint(LocalDate tradeDate, BigDecimal closePrice, BigDecimal previousClose) {
    }

    private record CalendarProfitData(BigDecimal profit, BigDecimal profitRate, BigDecimal marketValue, BigDecimal price, BigDecimal previousPrice, BigDecimal baseAmount) {
    }

    private record DailySegmentProfitData(BigDecimal profit, BigDecimal baseAmount, BigDecimal endQuantity, String status, String statusLabel) {
    }

    private record IntradayLot(BigDecimal quantity, BigDecimal price) {
    }

    private record IntradayConsumeData(BigDecimal profit, BigDecimal baseAmount) {
    }
}
