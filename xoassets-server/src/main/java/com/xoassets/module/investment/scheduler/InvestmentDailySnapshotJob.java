package com.xoassets.module.investment.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
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

    private static final String TYPE_BUY = "BUY";
    private static final String INPUT_MODE_AMOUNT_NAV = "AMOUNT_NAV";
    private static final String STATUS_PENDING_CONFIRM = "PENDING_CONFIRM";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final int RECENT_REPAIR_DAYS = 4;

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
     * 在日级价格汇总后执行，补跑最近 4 个自然日，周一可覆盖上周五交易日。
     */
    @Scheduled(cron = "${xoassets.quotes.investment-snapshot-cron:0 30 19 * * ?}")
    public void snapshotRecentDays() {
        snapshotRecentDaysSafely();
    }

    /**
     * 20:00 到 23:30 每半小时继续 upsert，等待晚间净值逐步入库。
     */
    @Scheduled(cron = "${xoassets.quotes.investment-snapshot-followup-cron:0 0,30 20-23 * * ?}")
    public void snapshotRecentDaysFollowup() {
        snapshotRecentDaysSafely();
    }

    private void snapshotRecentDaysSafely() {
        for (int daysAgo = RECENT_REPAIR_DAYS - 1; daysAgo >= 0; daysAgo--) {
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
            snapshotForUser(userId, snapshotDate);
        }
    }

    /**
     * 手动重建单个用户指定日期投资日快照，供本地对账修复使用。
     */
    @Transactional(rollbackFor = Exception.class)
    public void snapshotForUser(Long userId, LocalDate snapshotDate) {
        LocalDate targetDate = snapshotDate == null ? LocalDate.now() : snapshotDate;
        if (targetDate.isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "不能生成未来日期投资快照");
        }
        upsert(buildSnapshot(userId, targetDate));
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
        BigDecimal inTransitAmount = inTransitFundBuyAmount(userId, snapshotDate);
        // 基金金额买入从实际申购到确认日前已扣资金账户，日快照按在途投资资产处理，收益计算再用净流入剔除本金影响。
        marketValue = marketValue.add(inTransitAmount);
        totalCost = totalCost.add(inTransitAmount);
        marketValue = scale4(marketValue);
        totalCost = scale4(totalCost);
        BigDecimal floatingProfit = marketValue.subtract(totalCost).setScale(4, RoundingMode.HALF_UP);
        BigDecimal floatingProfitRate = rate(floatingProfit, totalCost);
        BigDecimal realizedProfit = realizedProfit(userId, snapshotDate);
        BigDecimal netInflow = positionHistoryService.netInflow(userId, snapshotDate, snapshotDate)
                .add(inTransitAmount)
                .setScale(4, RoundingMode.HALF_UP);
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
            // 投资快照补跑要使用已沉淀的权威日级价格；缺价时由调用方按成本价兜底。
            return null;
        }
        return current.getPrice();
    }

    private BigDecimal inTransitFundBuyAmount(Long userId, LocalDate snapshotDate) {
        LocalDateTime end = snapshotDate.atTime(LocalTime.MAX);
        return investmentTransactionMapper.selectList(new LambdaQueryWrapper<InvestmentTransaction>()
                        .eq(InvestmentTransaction::getUserId, userId)
                        .eq(InvestmentTransaction::getType, TYPE_BUY)
                        .eq(InvestmentTransaction::getInputMode, INPUT_MODE_AMOUNT_NAV)
                        .in(InvestmentTransaction::getStatus, STATUS_PENDING_CONFIRM, STATUS_CONFIRMED)
                        .le(InvestmentTransaction::getTransactionTime, end))
                .stream()
                .filter(transaction -> STATUS_PENDING_CONFIRM.equals(transaction.getStatus())
                        || (transaction.getConfirmedDate() != null && transaction.getConfirmedDate().isAfter(snapshotDate)))
                .map(InvestmentTransaction::getTradeAmount)
                .map(this::scale4)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
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
                // 与 uk_user_snapshot_date(user_id, snapshot_date, deleted) 保持一致，避免误复活逻辑删除快照。
                .eq(InvestmentDailySnapshot::getDeleted, 0)
                .last("LIMIT 1"));
        if (exists == null) {
            investmentDailySnapshotMapper.insert(snapshot);
            return;
        }
        snapshot.setId(exists.getId());
        investmentDailySnapshotMapper.update(snapshot, new LambdaUpdateWrapper<InvestmentDailySnapshot>()
                .eq(InvestmentDailySnapshot::getId, exists.getId())
                .eq(InvestmentDailySnapshot::getDeleted, 0));
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
