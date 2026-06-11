package com.xoassets.module.investment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.module.investment.dto.ManualQuoteRequest;
import com.xoassets.module.investment.provider.QuoteFetchResult;
import com.xoassets.module.investment.provider.QuoteProvider;
import com.xoassets.module.investment.service.AssetService;
import com.xoassets.module.investment.service.InvestmentHoldingDailyProfitService;
import com.xoassets.module.investment.service.QuoteRawSnapshot;
import com.xoassets.module.investment.service.QuoteRawSnapshotService;
import com.xoassets.module.investment.service.QuoteService;
import com.xoassets.module.investment.vo.AssetPriceVO;
import com.xoassets.persistence.entity.Asset;
import com.xoassets.persistence.entity.AssetPriceCurrent;
import com.xoassets.persistence.entity.AssetPriceDaily;
import com.xoassets.persistence.entity.MarketCalendar;
import com.xoassets.persistence.mapper.AssetPriceCurrentMapper;
import com.xoassets.persistence.mapper.AssetPriceDailyMapper;
import com.xoassets.persistence.mapper.MarketCalendarMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 行情价格服务实现：当前价落 MySQL current，原始快照短期落 Redis。
 */
@Slf4j
@Service
public class QuoteServiceImpl implements QuoteService {

    /**
     * 股票行情刷新开始时间。
     */
    private static final LocalTime STOCK_REFRESH_START = LocalTime.of(9, 30);
    /**
     * 股票行情刷新结束时间。
     */
    private static final LocalTime STOCK_REFRESH_END = LocalTime.of(15, 0);
    /**
     * 基金资产类型常量。
     */
    private static final String ASSET_TYPE_FUND = "FUND";
    /**
     * 股票资产类型常量。
     */
    private static final String ASSET_TYPE_STOCK = "STOCK";
    /**
     * 虚拟货币资产类型常量。
     */
    private static final String ASSET_TYPE_CRYPTO = "CRYPTO";
    /**
     * 交易日历优先级排序SQL。
     */
    private static final String CALENDAR_PRIORITY_SQL = "order by case source when 'MANUAL' then 3 when 'EXCHANGE_ANNOUNCEMENT' then 2 when 'SYSTEM_WEEKDAY' then 1 else 0 end desc, id desc limit 1";

    /**
     * 当前价格数据访问组件。
     */
    private final AssetPriceCurrentMapper assetPriceCurrentMapper;
    /**
     * 日级价格数据访问组件。
     */
    private final AssetPriceDailyMapper assetPriceDailyMapper;
    /**
     * 交易日历数据访问组件。
     */
    private final MarketCalendarMapper marketCalendarMapper;
    /**
     * Redis行情快照服务。
     */
    private final QuoteRawSnapshotService quoteRawSnapshotService;
    /**
     * 资产服务。
     */
    private final AssetService assetService;
    /**
     * 持仓每日收益服务。
     */
    private final InvestmentHoldingDailyProfitService holdingDailyProfitService;
    /**
     * 行情提供方列表。
     */
    private final List<QuoteProvider> quoteProviders;

    /**
     * 注入业务依赖。
     */
    public QuoteServiceImpl(
            AssetPriceCurrentMapper assetPriceCurrentMapper,
            AssetPriceDailyMapper assetPriceDailyMapper,
            MarketCalendarMapper marketCalendarMapper,
            QuoteRawSnapshotService quoteRawSnapshotService,
            AssetService assetService,
            InvestmentHoldingDailyProfitService holdingDailyProfitService,
            List<QuoteProvider> quoteProviders) {
        this.assetPriceCurrentMapper = assetPriceCurrentMapper;
        this.assetPriceDailyMapper = assetPriceDailyMapper;
        this.marketCalendarMapper = marketCalendarMapper;
        this.quoteRawSnapshotService = quoteRawSnapshotService;
        this.assetService = assetService;
        this.holdingDailyProfitService = holdingDailyProfitService;
        this.quoteProviders = quoteProviders;
    }

    /**
     * 手动录入价格只写 current/daily；旧价格快照表已退役，避免删表后仍访问旧审计表。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AssetPriceVO manualQuote(ManualQuoteRequest request) {
        Asset asset = assetService.findAsset(request.getAssetId());
        AssetPriceCurrent price = new AssetPriceCurrent();
        price.setAssetId(asset.getId());
        // 行情价格按 8 位入库；持仓市值再按业务金额口径收敛到 4 位。
        price.setPrice(request.getPrice().setScale(8, RoundingMode.HALF_UP));
        price.setCurrency(StringUtils.hasText(request.getCurrency()) ? request.getCurrency() : asset.getCurrency());
        price.setSource("MANUAL");
        price.setQuoteTime(request.getQuoteTime() == null ? LocalDateTime.now() : request.getQuoteTime());
        price.setMarketStatus("MANUAL");
        price.setRawJson(null);
        upsertCurrent(price);
        upsertSinglePointDailyPrice(asset, price);
        refreshHoldingDailyProfit(asset, price);
        return toVO(price);
    }

    /**
     * 用户手动刷新必须绕过 TTL 和交易时段缓存，尽量拿到最新可用收盘价。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AssetPriceVO refreshQuote(Long assetId) {
        return refreshQuoteInternal(assetId, true);
    }

    /**
     * 批量刷新行情，单个资产失败时保留旧价格，避免影响整批持仓刷新。
     */
    @Override
    public List<AssetPriceVO> refreshQuotes(Collection<Long> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) {
            return List.of();
        }
        List<AssetPriceVO> results = new ArrayList<>();
        for (Long assetId : assetIds.stream().distinct().toList()) {
            try {
                AssetPriceVO refreshed = refreshQuote(assetId);
                if (refreshed != null) {
                    results.add(refreshed);
                }
            } catch (Exception exception) {
                log.warn("批量刷新行情失败 assetId={}", assetId, exception);
                AssetPriceCurrent latestPrice = latestPrice(assetId);
                if (latestPrice != null) {
                    results.add(toVO(latestPrice));
                }
            }
        }
        return results;
    }

    /**
     * 刷新过期行情；手动价格永不过期，失败时不删除旧价格。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AssetPriceVO refreshQuoteIfStale(Long assetId) {
        return refreshQuoteInternal(assetId, false);
    }

    /**
     * force=true 用于用户主动刷新；force=false 用于定时任务和后台按需刷新。
     */
    private AssetPriceVO refreshQuoteInternal(Long assetId, boolean force) {
        Asset asset = assetService.findAsset(assetId);
        AssetPriceCurrent latestPrice = latestPrice(assetId);
        if (!force && isOutsideStockRefreshWindow(asset)) {
            // 股票行情只在 09:30-15:00 之间主动刷新；非交易时段直接复用最近快照，避免无意义写入。
            if (latestPrice != null) {
                return toVO(latestPrice);
            }
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "股票行情仅在交易时段刷新");
        }
        if (!force && isFresh(asset, latestPrice)) {
            return toVO(latestPrice);
        }
        QuoteProvider provider = quoteProviders.stream()
                .filter(item -> item.supports(asset))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_ERROR, "当前资产暂不支持自动刷新行情"));
        QuoteFetchResult result;
        try {
            result = provider.fetch(asset);
        } catch (Exception exception) {
            // 第三方行情失败时保留旧价格；没有旧价格才把错误返回给前端，引导用户手动录价。
            log.warn("资产行情刷新失败，保留最近价格 assetId={}", assetId, exception);
            if (latestPrice != null) {
                return toVO(latestPrice);
            }
            throw exception;
        }
        AssetPriceCurrent price = toCurrent(asset.getId(), result);
        upsertFundDailyPrice(asset, price);
        if (sameOrOlderQuote(latestPrice, price)) {
            // 第三方在非交易日或 QDII 延迟时会重复返回同一净值日期，不能反复更新 current.updated_at 误导为今日新价。
            // 但持仓每日收益表可能仍落后于 current，重复行情也要触发一次收益重建。
            refreshHoldingDailyProfit(asset, latestPrice);
            return toVO(latestPrice);
        }
        upsertCurrent(price);
        appendRawSnapshot(asset, price);
        refreshHoldingDailyProfit(asset, price);
        return toVO(price);
    }

    /**
     * 行情更新成功后立即刷新受影响持仓的当日收益，避免收益日历等到晚间快照任务才有数据。
     */
    private void refreshHoldingDailyProfit(Asset asset, AssetPriceCurrent price) {
        if (asset == null || price == null || price.getQuoteTime() == null) {
            return;
        }
        holdingDailyProfitService.rebuildForAsset(asset.getId(), price.getQuoteTime().toLocalDate());
    }

    /**
     * 批量查询当前价格；旧价格快照表已退役，不再做旧表兜底。
     */
    @Override
    public Map<Long, AssetPriceCurrent> latestPriceMap(Collection<Long> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) {
            return Map.of();
        }
        // 公共批量入口先过滤空 ID，避免历史脏持仓或调用方漏校验导致 selectBatchIds 异常。
        Set<Long> cleanAssetIds = assetIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (cleanAssetIds.isEmpty()) {
            return Map.of();
        }
        return assetPriceCurrentMapper.selectBatchIds(cleanAssetIds).stream()
                .collect(Collectors.toMap(AssetPriceCurrent::getAssetId, price -> price, (left, right) -> left));
    }

    /**
     * 查询单个资产最近价格，供缓存判断和手动资产兜底使用。
     */
    private AssetPriceCurrent latestPrice(Long assetId) {
        return assetPriceCurrentMapper.selectById(assetId);
    }

    /**
     * 按资产类型判断最近价格是否仍可复用。
     */
    private boolean isFresh(Asset asset, AssetPriceCurrent price) {
        if (price == null || price.getQuoteTime() == null) {
            return false;
        }
        if ("MANUAL".equals(asset.getQuoteSource())) {
            return true;
        }
        Duration ttl = switch (asset.getType()) {
            case "CRYPTO" -> Duration.ofMinutes(15);
            case "STOCK" -> Duration.ofMinutes(15);
            case "FUND" -> Duration.ofDays(1);
            default -> Duration.ZERO;
        };
        return ttl.isZero() || !price.getQuoteTime().isBefore(LocalDateTime.now().minus(ttl));
    }

    /**
     * 股票只在开盘日 09:30-15:00 之间拉取第三方行情，其他时间保留最近快照。
     */
    private boolean isOutsideStockRefreshWindow(Asset asset) {
        if (!ASSET_TYPE_STOCK.equals(asset.getType())) {
            return false;
        }
        if (!isTradingDay()) {
            return true;
        }
        LocalTime now = LocalTime.now();
        return now.isBefore(STOCK_REFRESH_START) || now.isAfter(STOCK_REFRESH_END);
    }

    /**
     * 股票开盘日优先以市场日历为准；本地缺少日历时回退到工作日判断。
     */
    private boolean isTradingDay() {
        LocalDateTime now = LocalDateTime.now();
        MarketCalendar calendar = marketCalendarMapper.selectOne(new LambdaQueryWrapper<MarketCalendar>()
                .eq(MarketCalendar::getMarket, "A_SHARE")
                .eq(MarketCalendar::getTradeDate, now.toLocalDate())
                // 交易所公告修正优先于系统周末规则，避免节假日误触发行情刷新。
                .last(CALENDAR_PRIORITY_SQL));
        if (calendar != null) {
            return Boolean.TRUE.equals(calendar.getTradingDay());
        }
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    /**
     * 当前价每个资产只保留一条，用于持仓估值和首页投资资产。
     */
    private void upsertCurrent(AssetPriceCurrent current) {
        if (assetPriceCurrentMapper.selectById(current.getAssetId()) == null) {
            assetPriceCurrentMapper.insert(current);
            return;
        }
        assetPriceCurrentMapper.updateById(current);
    }

    /**
     * 基金净值本身就是日级收盘价，刷新成功后直接沉淀到 daily，避免等夜间聚合任务导致确认和收益基准滞后。
     */
    private void upsertFundDailyPrice(Asset asset, AssetPriceCurrent price) {
        if (!ASSET_TYPE_FUND.equals(asset.getType())) {
            return;
        }
        upsertSinglePointDailyPrice(asset, price);
    }

    /**
     * 手动价没有 Redis intraday 明细，直接按单点日价沉淀到 daily，保证删掉旧表后历史曲线仍有来源。
     */
    private void upsertSinglePointDailyPrice(Asset asset, AssetPriceCurrent price) {
        if (asset == null || price == null || price.getQuoteTime() == null || price.getPrice() == null) {
            return;
        }
        AssetPriceDaily daily = toDailyPrice(price);
        AssetPriceDaily exists = assetPriceDailyMapper.selectOne(new LambdaQueryWrapper<AssetPriceDaily>()
                .eq(AssetPriceDaily::getAssetId, daily.getAssetId())
                .eq(AssetPriceDaily::getTradeDate, daily.getTradeDate())
                // 日级净值只 upsert 当前有效记录，不复用逻辑删除历史行。
                .eq(AssetPriceDaily::getDeleted, 0)
                .last("limit 1"));
        if (exists == null) {
            assetPriceDailyMapper.insert(daily);
            return;
        }
        if (sameDailyPayload(exists, daily)) {
            return;
        }
        daily.setId(exists.getId());
        assetPriceDailyMapper.update(daily, new LambdaUpdateWrapper<AssetPriceDaily>()
                .eq(AssetPriceDaily::getId, exists.getId())
                .eq(AssetPriceDaily::getDeleted, 0));
    }

    /**
     * 转换为日级价格实体。
     */
    private AssetPriceDaily toDailyPrice(AssetPriceCurrent price) {
        AssetPriceDaily daily = new AssetPriceDaily();
        daily.setAssetId(price.getAssetId());
        daily.setTradeDate(price.getQuoteTime().toLocalDate());
        daily.setOpenPrice(price.getPrice().setScale(8, RoundingMode.HALF_UP));
        daily.setClosePrice(price.getPrice().setScale(8, RoundingMode.HALF_UP));
        daily.setHighPrice(price.getPrice().setScale(8, RoundingMode.HALF_UP));
        daily.setLowPrice(price.getPrice().setScale(8, RoundingMode.HALF_UP));
        daily.setPreviousClose(price.getPreviousClose());
        daily.setChangeAmount(price.getChangeAmount());
        daily.setChangePercent(price.getChangePercent());
        daily.setCurrency(price.getCurrency());
        daily.setSource(price.getSource());
        daily.setDeleted(0);
        return daily;
    }

    /**
     * 判断行情是否未更新。
     */
    private boolean sameOrOlderQuote(AssetPriceCurrent latestPrice, AssetPriceCurrent fetchedPrice) {
        if (latestPrice == null || fetchedPrice == null || latestPrice.getQuoteTime() == null || fetchedPrice.getQuoteTime() == null) {
            return false;
        }
        return !fetchedPrice.getQuoteTime().isAfter(latestPrice.getQuoteTime()) && samePricePayload(latestPrice, fetchedPrice);
    }

    /**
     * 判断当前价格数据是否相同。
     */
    private boolean samePricePayload(AssetPriceCurrent left, AssetPriceCurrent right) {
        return sameAmount(left.getPrice(), right.getPrice())
                && sameText(left.getCurrency(), right.getCurrency())
                && sameAmount(left.getPreviousClose(), right.getPreviousClose())
                && sameAmount(left.getChangeAmount(), right.getChangeAmount())
                && sameAmount(left.getChangePercent(), right.getChangePercent())
                && sameText(left.getSource(), right.getSource())
                && sameText(left.getMarketStatus(), right.getMarketStatus());
    }

    /**
     * 判断日级价格数据是否相同。
     */
    private boolean sameDailyPayload(AssetPriceDaily left, AssetPriceDaily right) {
        return sameAmount(left.getOpenPrice(), right.getOpenPrice())
                && sameAmount(left.getClosePrice(), right.getClosePrice())
                && sameAmount(left.getHighPrice(), right.getHighPrice())
                && sameAmount(left.getLowPrice(), right.getLowPrice())
                && sameAmount(left.getPreviousClose(), right.getPreviousClose())
                && sameAmount(left.getChangeAmount(), right.getChangeAmount())
                && sameAmount(left.getChangePercent(), right.getChangePercent())
                && sameText(left.getCurrency(), right.getCurrency())
                && sameText(left.getSource(), right.getSource());
    }

    /**
     * 判断金额是否相同。
     */
    private boolean sameAmount(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.compareTo(right) == 0;
    }

    /**
     * 判断文本是否相同。
     */
    private boolean sameText(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    /**
     * Redis 只保存股票和虚拟货币短期原始快照，基金净值直接沉淀到日级价格表。
     */
    private void appendRawSnapshot(Asset asset, AssetPriceCurrent price) {
        if (asset == null || price == null || !(ASSET_TYPE_STOCK.equals(asset.getType()) || ASSET_TYPE_CRYPTO.equals(asset.getType()))) {
            return;
        }
        quoteRawSnapshotService.append(toRawSnapshot(price));
    }

    /**
     * 转换为当前价格实体。
     */
    private AssetPriceCurrent toCurrent(Long assetId, QuoteFetchResult result) {
        AssetPriceCurrent current = new AssetPriceCurrent();
        current.setAssetId(assetId);
        current.setPrice(result.price().setScale(8, RoundingMode.HALF_UP));
        current.setCurrency(result.currency());
        current.setPreviousClose(result.previousClose() == null ? null : result.previousClose().setScale(8, RoundingMode.HALF_UP));
        current.setChangeAmount(result.changeAmount() == null ? null : result.changeAmount().setScale(8, RoundingMode.HALF_UP));
        current.setChangePercent(result.changePercent() == null ? null : result.changePercent().setScale(4, RoundingMode.HALF_UP));
        current.setSource(result.source());
        current.setQuoteTime(result.quoteTime());
        current.setMarketStatus(result.marketStatus());
        current.setRawJson(result.rawJson());
        return current;
    }

    /**
     * 转换为Redis行情快照。
     */
    private QuoteRawSnapshot toRawSnapshot(AssetPriceCurrent price) {
        return new QuoteRawSnapshot(
                price.getAssetId(),
                price.getPrice(),
                price.getCurrency(),
                price.getPreviousClose(),
                price.getChangeAmount(),
                price.getChangePercent(),
                price.getSource(),
                price.getQuoteTime(),
                price.getMarketStatus());
    }

    /**
     * 转换价格快照展示对象。
     */
    private AssetPriceVO toVO(AssetPriceCurrent price) {
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
}
