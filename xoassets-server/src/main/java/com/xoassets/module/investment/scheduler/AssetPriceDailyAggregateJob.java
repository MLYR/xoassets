package com.xoassets.module.investment.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xoassets.module.investment.service.QuoteRawSnapshot;
import com.xoassets.module.investment.service.QuoteRawSnapshotService;
import com.xoassets.persistence.entity.Asset;
import com.xoassets.persistence.entity.AssetPriceCurrent;
import com.xoassets.persistence.entity.AssetPriceDaily;
import com.xoassets.persistence.entity.Holding;
import com.xoassets.persistence.mapper.AssetMapper;
import com.xoassets.persistence.mapper.AssetPriceCurrentMapper;
import com.xoassets.persistence.mapper.AssetPriceDailyMapper;
import com.xoassets.persistence.mapper.HoldingMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资产日级价格汇总任务，将 Redis 短期原始快照沉淀为 MySQL 长期权威日线。
 */
@Slf4j
@Component
public class AssetPriceDailyAggregateJob {

    /**
     * 股票资产类型常量。
     */
    private static final String ASSET_TYPE_STOCK = "STOCK";
    /**
     * 虚拟货币资产类型常量。
     */
    private static final String ASSET_TYPE_CRYPTO = "CRYPTO";
    /**
     * 近期数据修复天数。
     */
    private static final int RECENT_REPAIR_DAYS = 4;

    /**
     * 持仓数据访问组件。
     */
    private final HoldingMapper holdingMapper;
    /**
     * 资产数据访问组件。
     */
    private final AssetMapper assetMapper;
    /**
     * 当前价格数据访问组件。
     */
    private final AssetPriceCurrentMapper assetPriceCurrentMapper;
    /**
     * 日级价格数据访问组件。
     */
    private final AssetPriceDailyMapper assetPriceDailyMapper;
    /**
     * Redis行情快照服务。
     */
    private final QuoteRawSnapshotService quoteRawSnapshotService;

    /**
     * 注入定时任务依赖。
     */
    public AssetPriceDailyAggregateJob(
            HoldingMapper holdingMapper,
            AssetMapper assetMapper,
            AssetPriceCurrentMapper assetPriceCurrentMapper,
            AssetPriceDailyMapper assetPriceDailyMapper,
            QuoteRawSnapshotService quoteRawSnapshotService) {
        this.holdingMapper = holdingMapper;
        this.assetMapper = assetMapper;
        this.assetPriceCurrentMapper = assetPriceCurrentMapper;
        this.assetPriceDailyMapper = assetPriceDailyMapper;
        this.quoteRawSnapshotService = quoteRawSnapshotService;
    }

    /**
     * 每天补跑最近 4 个自然日，周一晚间仍能覆盖上周五交易日，避免周末打断补数。
     */
    @Scheduled(cron = "${xoassets.quotes.daily-aggregate-cron:0 25,55 19-22 * * ?}")
    public void aggregateRecentDays() {
        for (int daysAgo = RECENT_REPAIR_DAYS - 1; daysAgo >= 0; daysAgo--) {
            try {
                aggregate(LocalDate.now().minusDays(daysAgo));
            } catch (Exception exception) {
                log.warn("资产日级价格汇总失败 date={}", LocalDate.now().minusDays(daysAgo), exception);
            }
        }
    }

    /**
     * 23:25 再补一轮，给 23:30 投资快照和 23:50 总资产快照提供晚间最终价。
     */
    @Scheduled(cron = "${xoassets.quotes.daily-aggregate-late-cron:0 25 23 * * ?}")
    public void aggregateLateRecentDays() {
        aggregateRecentDays();
    }

    /**
     * 支持指定日期重跑；收益计算依赖日级表，不能直接依赖 Redis。
     */
    @Transactional(rollbackFor = Exception.class)
    public void aggregate(LocalDate tradeDate) {
        Set<Long> assetIds = activeHoldingAssetIds();
        for (Long assetId : assetIds) {
            upsertDailyPrice(assetId, tradeDate);
        }
    }

    /**
     * 新增或更新日级价格。
     */
    private void upsertDailyPrice(Long assetId, LocalDate tradeDate) {
        List<QuoteRawSnapshot> snapshots = quoteRawSnapshotService.listByDate(assetId, tradeDate);
        AssetPriceDaily daily = snapshots.isEmpty()
                ? fallbackFromCurrent(assetId, tradeDate)
                : aggregateSnapshots(assetId, tradeDate, snapshots);
        if (daily == null) {
            return;
        }
        AssetPriceDaily previous = previousDaily(assetId, tradeDate);
        if (previous != null) {
            daily.setPreviousClose(scale8(previous.getClosePrice()));
            daily.setChangeAmount(scale8(daily.getClosePrice().subtract(previous.getClosePrice())));
            daily.setChangePercent(changePercent(daily.getClosePrice(), previous.getClosePrice()));
        }
        upsert(daily);
    }

    /**
     * 聚合Redis行情快照。
     */
    private AssetPriceDaily aggregateSnapshots(Long assetId, LocalDate tradeDate, List<QuoteRawSnapshot> snapshots) {
        List<QuoteRawSnapshot> sorted = snapshots.stream()
                .filter(item -> item.price() != null)
                .sorted(Comparator.comparing(QuoteRawSnapshot::quoteTime))
                .toList();
        if (sorted.isEmpty()) {
            return null;
        }
        AssetPriceDaily daily = new AssetPriceDaily();
        daily.setAssetId(assetId);
        daily.setTradeDate(tradeDate);
        daily.setOpenPrice(scale8(sorted.get(0).price()));
        daily.setClosePrice(scale8(sorted.get(sorted.size() - 1).price()));
        daily.setHighPrice(scale8(sorted.stream().map(QuoteRawSnapshot::price).max(BigDecimal::compareTo).orElse(sorted.get(0).price())));
        daily.setLowPrice(scale8(sorted.stream().map(QuoteRawSnapshot::price).min(BigDecimal::compareTo).orElse(sorted.get(0).price())));
        daily.setCurrency(sorted.get(sorted.size() - 1).currency());
        daily.setSource(sorted.get(sorted.size() - 1).source());
        daily.setDeleted(0);
        return daily;
    }

    /**
     * Redis 缺失时用 current 兜底，保证日级数据不断档，但 open/high/low 只能等于当前价。
     */
    private AssetPriceDaily fallbackFromCurrent(Long assetId, LocalDate tradeDate) {
        AssetPriceCurrent current = assetPriceCurrentMapper.selectById(assetId);
        if (current == null || current.getPrice() == null) {
            return null;
        }
        if (current.getQuoteTime() == null || !current.getQuoteTime().toLocalDate().equals(tradeDate)) {
            // 不能用今天的 current 反向补昨天/前天，否则最近两个交易日收盘价会被写成同一个值，昨日收益恒为 0。
            return null;
        }
        AssetPriceDaily daily = new AssetPriceDaily();
        daily.setAssetId(assetId);
        daily.setTradeDate(tradeDate);
        daily.setOpenPrice(scale8(current.getPrice()));
        daily.setClosePrice(scale8(current.getPrice()));
        daily.setHighPrice(scale8(current.getPrice()));
        daily.setLowPrice(scale8(current.getPrice()));
        daily.setCurrency(current.getCurrency());
        daily.setSource(current.getSource());
        daily.setDeleted(0);
        return daily;
    }

    /**
     * 查询上一交易日日级价格。
     */
    private AssetPriceDaily previousDaily(Long assetId, LocalDate tradeDate) {
        return assetPriceDailyMapper.selectOne(new LambdaQueryWrapper<AssetPriceDaily>()
                .eq(AssetPriceDaily::getAssetId, assetId)
                .lt(AssetPriceDaily::getTradeDate, tradeDate)
                .orderByDesc(AssetPriceDaily::getTradeDate)
                .last("LIMIT 1"));
    }

    /**
     * 新增或更新记录。
     */
    private void upsert(AssetPriceDaily daily) {
        AssetPriceDaily exists = assetPriceDailyMapper.selectOne(new LambdaQueryWrapper<AssetPriceDaily>()
                .eq(AssetPriceDaily::getAssetId, daily.getAssetId())
                .eq(AssetPriceDaily::getTradeDate, daily.getTradeDate())
                // 与 uk_asset_trade_date(asset_id, trade_date, deleted) 保持一致，只更新有效日线。
                .eq(AssetPriceDaily::getDeleted, 0)
                .last("LIMIT 1"));
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
     * 查询当前仍在持仓中的资产ID。
     */
    private Set<Long> activeHoldingAssetIds() {
        Set<Long> assetIds = holdingMapper.selectList(new LambdaQueryWrapper<Holding>()
                        .eq(Holding::getStatus, 1)
                        .gt(Holding::getQuantity, 0))
                .stream()
                .map(Holding::getAssetId)
                .collect(Collectors.toSet());
        if (assetIds.isEmpty()) {
            return Set.of();
        }
        // Redis 原始快照只承载股票和虚拟货币；基金净值由基金刷新直接写 current/daily，不参与 Redis 聚合。
        return assetMapper.selectBatchIds(assetIds).stream()
                .filter(asset -> ASSET_TYPE_STOCK.equals(asset.getType()) || ASSET_TYPE_CRYPTO.equals(asset.getType()))
                .map(Asset::getId)
                .collect(Collectors.toSet());
    }

    /**
     * 按价格精度保留八位小数。
     */
    private BigDecimal scale8(BigDecimal value) {
        return value == null ? null : value.setScale(8, RoundingMode.HALF_UP);
    }

    /**
     * 计算较上一交易日涨跌幅。
     */
    private BigDecimal changePercent(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null || previous.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return current.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous, 4, RoundingMode.HALF_UP);
    }
}
