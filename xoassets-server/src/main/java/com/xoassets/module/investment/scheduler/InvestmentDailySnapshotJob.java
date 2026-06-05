package com.xoassets.module.investment.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xoassets.module.investment.service.InvestmentPositionHistoryService;
import com.xoassets.module.investment.service.InvestmentPositionState;
import com.xoassets.persistence.entity.AssetPriceCurrent;
import com.xoassets.persistence.entity.AssetPriceDaily;
import com.xoassets.persistence.entity.InvestmentDailySnapshot;
import com.xoassets.persistence.entity.InvestmentTransaction;
import com.xoassets.persistence.mapper.AssetPriceCurrentMapper;
import com.xoassets.persistence.mapper.AssetPriceDailyMapper;
import com.xoassets.persistence.mapper.InvestmentDailySnapshotMapper;
import com.xoassets.persistence.mapper.InvestmentTransactionMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户投资日快照任务，长期保存用户维度收益对比基准。
 */
@Slf4j
@Component
public class InvestmentDailySnapshotJob {

    private final InvestmentPositionHistoryService positionHistoryService;
    private final AssetPriceCurrentMapper assetPriceCurrentMapper;
    private final AssetPriceDailyMapper assetPriceDailyMapper;
    private final InvestmentTransactionMapper investmentTransactionMapper;
    private final InvestmentDailySnapshotMapper investmentDailySnapshotMapper;

    public InvestmentDailySnapshotJob(
            InvestmentPositionHistoryService positionHistoryService,
            AssetPriceCurrentMapper assetPriceCurrentMapper,
            AssetPriceDailyMapper assetPriceDailyMapper,
            InvestmentTransactionMapper investmentTransactionMapper,
            InvestmentDailySnapshotMapper investmentDailySnapshotMapper) {
        this.positionHistoryService = positionHistoryService;
        this.assetPriceCurrentMapper = assetPriceCurrentMapper;
        this.assetPriceDailyMapper = assetPriceDailyMapper;
        this.investmentTransactionMapper = investmentTransactionMapper;
        this.investmentDailySnapshotMapper = investmentDailySnapshotMapper;
    }

    /**
     * 在日级价格汇总后执行，失败会在下一次补跑最近 3 天时被覆盖更新。
     */
    @Scheduled(cron = "${xoassets.quotes.investment-snapshot-cron:0 45 23 * * ?}")
    public void snapshotRecentDays() {
        for (int daysAgo = 2; daysAgo >= 0; daysAgo--) {
            try {
                snapshot(LocalDate.now().minusDays(daysAgo));
            } catch (Exception exception) {
                log.warn("用户投资日快照生成失败 date={}", LocalDate.now().minusDays(daysAgo), exception);
            }
        }
    }

    /**
     * 支持指定日期重跑；较昨日和较上月都以该表为权威基准。
     */
    @Transactional(rollbackFor = Exception.class)
    public void snapshot(LocalDate snapshotDate) {
        for (Long userId : positionHistoryService.snapshotUserIds(snapshotDate, LocalDate.now())) {
            upsert(buildSnapshot(userId, snapshotDate));
        }
    }

    private InvestmentDailySnapshot buildSnapshot(Long userId, LocalDate snapshotDate) {
        Map<Long, InvestmentPositionState> positions = positionHistoryService.positionsAt(userId, snapshotDate);
        BigDecimal marketValue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        for (InvestmentPositionState position : positions.values()) {
            BigDecimal price = resolvePrice(position.assetId(), snapshotDate);
            if (price == null) {
                price = position.quantity().compareTo(BigDecimal.ZERO) <= 0
                        ? BigDecimal.ZERO
                        : position.totalCost().divide(position.quantity(), 8, RoundingMode.HALF_UP);
            }
            marketValue = marketValue.add(position.quantity().multiply(price));
            totalCost = totalCost.add(position.totalCost());
        }
        marketValue = scale4(marketValue);
        totalCost = scale4(totalCost);
        BigDecimal floatingProfit = marketValue.subtract(totalCost).setScale(4, RoundingMode.HALF_UP);
        BigDecimal floatingProfitRate = rate(floatingProfit, totalCost);
        BigDecimal realizedProfit = realizedProfit(userId, snapshotDate);
        BigDecimal netInflow = positionHistoryService.netInflow(userId, snapshotDate, snapshotDate);
        InvestmentDailySnapshot previous = previousSnapshot(userId, snapshotDate);
        BigDecimal dailyProfit = previous == null ? null : marketValue.subtract(previous.getMarketValue()).subtract(netInflow).setScale(4, RoundingMode.HALF_UP);
        BigDecimal dailyProfitRate = previous == null ? null : rate(dailyProfit, previous.getMarketValue());
        InvestmentDailySnapshot snapshot = new InvestmentDailySnapshot();
        snapshot.setUserId(userId);
        snapshot.setSnapshotDate(snapshotDate);
        snapshot.setMarketValue(marketValue);
        snapshot.setTotalCost(totalCost);
        snapshot.setFloatingProfit(floatingProfit);
        snapshot.setFloatingProfitRate(floatingProfitRate);
        snapshot.setRealizedProfit(realizedProfit);
        snapshot.setDailyProfit(dailyProfit);
        snapshot.setDailyProfitRate(dailyProfitRate);
        snapshot.setNetInflow(netInflow);
        snapshot.setDeleted(0);
        return snapshot;
    }

    private BigDecimal resolvePrice(Long assetId, LocalDate snapshotDate) {
        AssetPriceDaily daily = assetPriceDailyMapper.selectOne(new LambdaQueryWrapper<AssetPriceDaily>()
                .eq(AssetPriceDaily::getAssetId, assetId)
                .le(AssetPriceDaily::getTradeDate, snapshotDate)
                .orderByDesc(AssetPriceDaily::getTradeDate)
                .last("LIMIT 1"));
        if (daily != null) {
            return daily.getClosePrice();
        }
        AssetPriceCurrent current = assetPriceCurrentMapper.selectById(assetId);
        if (current == null || current.getQuoteTime() == null || current.getQuoteTime().toLocalDate().isAfter(snapshotDate)) {
            return null;
        }
        return current.getPrice();
    }

    private BigDecimal realizedProfit(Long userId, LocalDate snapshotDate) {
        LocalDateTime end = snapshotDate.atTime(LocalTime.MAX);
        return investmentTransactionMapper.selectList(new LambdaQueryWrapper<InvestmentTransaction>()
                        .eq(InvestmentTransaction::getUserId, userId)
                        .eq(InvestmentTransaction::getType, "SELL")
                        .in(InvestmentTransaction::getStatus, "NORMAL", "CONFIRMED")
                        .le(InvestmentTransaction::getTransactionTime, end))
                .stream()
                .filter(transaction -> effectiveDate(transaction).atTime(LocalTime.MAX).compareTo(end) <= 0)
                .map(InvestmentTransaction::getRealizedProfit)
                .map(this::scale4)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private LocalDate effectiveDate(InvestmentTransaction transaction) {
        if ("AMOUNT_NAV".equals(transaction.getInputMode()) && transaction.getConfirmedDate() != null) {
            return transaction.getConfirmedDate();
        }
        if (transaction.getTradeDate() != null) {
            return transaction.getTradeDate();
        }
        return transaction.getTransactionTime().toLocalDate();
    }

    private InvestmentDailySnapshot previousSnapshot(Long userId, LocalDate snapshotDate) {
        return investmentDailySnapshotMapper.selectOne(new LambdaQueryWrapper<InvestmentDailySnapshot>()
                .eq(InvestmentDailySnapshot::getUserId, userId)
                .lt(InvestmentDailySnapshot::getSnapshotDate, snapshotDate)
                .orderByDesc(InvestmentDailySnapshot::getSnapshotDate)
                .last("LIMIT 1"));
    }

    private void upsert(InvestmentDailySnapshot snapshot) {
        InvestmentDailySnapshot exists = investmentDailySnapshotMapper.selectOne(new LambdaQueryWrapper<InvestmentDailySnapshot>()
                .eq(InvestmentDailySnapshot::getUserId, snapshot.getUserId())
                .eq(InvestmentDailySnapshot::getSnapshotDate, snapshot.getSnapshotDate())
                .last("LIMIT 1"));
        if (exists == null) {
            investmentDailySnapshotMapper.insert(snapshot);
            return;
        }
        snapshot.setId(exists.getId());
        investmentDailySnapshotMapper.update(snapshot, new LambdaUpdateWrapper<InvestmentDailySnapshot>()
                .eq(InvestmentDailySnapshot::getId, exists.getId()));
    }

    private BigDecimal scale4(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 4, RoundingMode.HALF_UP);
    }
}
