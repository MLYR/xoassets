package com.xoassets.module.investment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xoassets.module.investment.service.InvestmentPositionHistoryService;
import com.xoassets.module.investment.service.InvestmentPositionState;
import com.xoassets.persistence.entity.Holding;
import com.xoassets.persistence.entity.InvestmentDailySnapshot;
import com.xoassets.persistence.entity.InvestmentTransaction;
import com.xoassets.persistence.mapper.HoldingMapper;
import com.xoassets.persistence.mapper.InvestmentDailySnapshotMapper;
import com.xoassets.persistence.mapper.InvestmentTransactionMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/**
 * 基于投资交易流水重建历史头寸，避免用当前持仓倒推历史。
 */
@Service
public class InvestmentPositionHistoryServiceImpl implements InvestmentPositionHistoryService {

    /**
     * 正常状态常量。
     */
    private static final String STATUS_NORMAL = "NORMAL";
    /**
     * 已确认状态常量。
     */
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    /**
     * 待确认状态常量。
     */
    private static final String STATUS_PENDING_CONFIRM = "PENDING_CONFIRM";
    /**
     * 买入类型常量。
     */
    private static final String TYPE_BUY = "BUY";
    /**
     * 卖出类型常量。
     */
    private static final String TYPE_SELL = "SELL";
    /**
     * 金额净值录入模式常量。
     */
    private static final String INPUT_MODE_AMOUNT_NAV = "AMOUNT_NAV";

    /**
     * 持仓数据访问组件。
     */
    private final HoldingMapper holdingMapper;
    /**
     * 流水数据访问组件。
     */
    private final InvestmentTransactionMapper transactionMapper;
    /**
     * 数据访问组件。
     */
    private final InvestmentDailySnapshotMapper dailySnapshotMapper;

    /**
     * 注入业务依赖。
     */
    public InvestmentPositionHistoryServiceImpl(
            HoldingMapper holdingMapper,
            InvestmentTransactionMapper transactionMapper,
            InvestmentDailySnapshotMapper dailySnapshotMapper) {
        this.holdingMapper = holdingMapper;
        this.transactionMapper = transactionMapper;
        this.dailySnapshotMapper = dailySnapshotMapper;
    }

    /**
     * 查询指定日期持仓状态。
     */
    @Override
    public Map<Long, InvestmentPositionState> positionsAt(Long userId, LocalDate date) {
        LocalDateTime end = date.atTime(LocalTime.MAX);
        Map<Long, PositionAccumulator> positions = new LinkedHashMap<>();
        addManualBaseHoldings(userId, date, positions);
        transactionMapper.selectList(new LambdaQueryWrapper<InvestmentTransaction>()
                        .eq(InvestmentTransaction::getUserId, userId)
                        .le(InvestmentTransaction::getTransactionTime, end)
                        .orderByAsc(InvestmentTransaction::getTransactionTime)
                        .orderByAsc(InvestmentTransaction::getId))
                .stream()
                .filter(this::isEffective)
                .filter(transaction -> !effectiveDate(transaction).isAfter(date))
                .forEach(transaction -> applyTransaction(positions, transaction));
        return positions.entrySet().stream()
                .filter(entry -> entry.getValue().quantity.compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().toState(entry.getKey()),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    /**
     * 查询指定日期持仓数量。
     */
    @Override
    public BigDecimal quantityAt(Long userId, Long holdingId, Long assetId, LocalDate date) {
        if (date == null) {
            return BigDecimal.ZERO.setScale(10, RoundingMode.HALF_UP);
        }
        InvestmentPositionState state = positionsAt(userId, date).get(holdingId);
        if (state == null || !Objects.equals(state.assetId(), assetId)) {
            return BigDecimal.ZERO.setScale(10, RoundingMode.HALF_UP);
        }
        return state.quantity().setScale(10, RoundingMode.HALF_UP);
    }

    /**
     * 统计投资净流入。
     */
    @Override
    public BigDecimal netInflow(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate == null ? endDate : startDate;
        LocalDate end = endDate == null ? start : endDate;
        return transactionMapper.selectList(new LambdaQueryWrapper<InvestmentTransaction>()
                        .eq(InvestmentTransaction::getUserId, userId)
                        .le(InvestmentTransaction::getTransactionTime, end.atTime(LocalTime.MAX))
                        .orderByAsc(InvestmentTransaction::getTransactionTime))
                .stream()
                .filter(this::isCashFlowEffective)
                // 净入金按现金实际进出投资资产的日期统计；基金份额确认日只影响持仓生效，不应重复算入本金。
                .filter(transaction -> inRange(cashFlowDate(transaction), start, end))
                .map(this::netInflowAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 查询需要生成快照的用户。
     */
    @Override
    public Set<Long> snapshotUserIds(LocalDate startDate, LocalDate endDate) {
        Set<Long> holdingUsers = holdingMapper.selectList(new LambdaQueryWrapper<Holding>()
                        .select(Holding::getUserId)
                        .eq(Holding::getStatus, 1))
                .stream()
                .map(Holding::getUserId)
                .collect(Collectors.toSet());
        Set<Long> transactionUsers = transactionMapper.selectList(new LambdaQueryWrapper<InvestmentTransaction>()
                        .le(InvestmentTransaction::getTransactionTime, endDate.atTime(LocalTime.MAX)))
                .stream()
                .filter(transaction -> (isEffective(transaction) && inRange(effectiveDate(transaction), startDate, endDate))
                        || (isCashFlowEffective(transaction) && inRange(cashFlowDate(transaction), startDate, endDate)))
                .map(InvestmentTransaction::getUserId)
                .collect(Collectors.toSet());
        Set<Long> snapshotUsers = dailySnapshotMapper.selectList(new LambdaQueryWrapper<InvestmentDailySnapshot>()
                        .select(InvestmentDailySnapshot::getUserId)
                        .between(InvestmentDailySnapshot::getSnapshotDate, startDate, endDate))
                .stream()
                .map(InvestmentDailySnapshot::getUserId)
                .collect(Collectors.toSet());
        return Stream.of(holdingUsers, transactionUsers, snapshotUsers).flatMap(Set::stream).collect(Collectors.toSet());
    }

    /**
     * 将交易应用到持仓状态。
     */
    private void applyTransaction(Map<Long, PositionAccumulator> positions, InvestmentTransaction transaction) {
        PositionAccumulator state = positions.computeIfAbsent(transaction.getHoldingId(), key -> new PositionAccumulator(transaction.getAssetId()));
        if (TYPE_BUY.equals(transaction.getType())) {
            BigDecimal quantity = scaleQuantity(transaction.getQuantity());
            BigDecimal cost = costAmount(transaction);
            state.quantity = state.quantity.add(quantity).setScale(10, RoundingMode.HALF_UP);
            state.totalCost = state.totalCost.add(cost).setScale(4, RoundingMode.HALF_UP);
            state.assetId = transaction.getAssetId();
            return;
        }
        if (TYPE_SELL.equals(transaction.getType())) {
            BigDecimal quantity = scaleQuantity(transaction.getQuantity());
            BigDecimal cost = costAmount(transaction);
            state.quantity = state.quantity.subtract(quantity).max(BigDecimal.ZERO).setScale(10, RoundingMode.HALF_UP);
            state.totalCost = state.totalCost.subtract(cost).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
        }
    }

    /**
     * 补充没有交易流水的手工基础持仓。
     */
    private void addManualBaseHoldings(Long userId, LocalDate date, Map<Long, PositionAccumulator> positions) {
        var holdings = holdingMapper.selectList(new LambdaQueryWrapper<Holding>()
                        .eq(Holding::getUserId, userId)
                        .eq(Holding::getStatus, 1)
                        .le(Holding::getCreatedAt, date.atTime(LocalTime.MAX)));
        if (holdings == null || holdings.isEmpty()) {
            return;
        }
        Set<Long> holdingIds = holdings.stream().map(Holding::getId).collect(Collectors.toSet());
        Map<Long, PositionAccumulator> transactionDeltas = allTransactionDeltas(userId, holdingIds);
        holdings.forEach(holding -> {
            PositionAccumulator delta = transactionDeltas.getOrDefault(holding.getId(), new PositionAccumulator(holding.getAssetId()));
            BigDecimal baseQuantity = scaleQuantity(holding.getQuantity()).subtract(delta.quantity).max(BigDecimal.ZERO).setScale(10, RoundingMode.HALF_UP);
            BigDecimal baseCost = scale4(holding.getTotalCost()).subtract(delta.totalCost).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
            if (baseQuantity.compareTo(BigDecimal.ZERO) <= 0 && baseCost.compareTo(BigDecimal.ZERO) <= 0) {
                return;
            }
            PositionAccumulator state = positions.computeIfAbsent(holding.getId(), key -> new PositionAccumulator(holding.getAssetId()));
            // 有后续交易的手工底仓也必须作为历史起点，否则追加买入确认后会把原始底仓从快照里排除。
            state.quantity = state.quantity.add(baseQuantity).setScale(10, RoundingMode.HALF_UP);
            state.totalCost = state.totalCost.add(baseCost).setScale(4, RoundingMode.HALF_UP);
            state.assetId = holding.getAssetId();
        });
    }

    /**
     * 汇总全部交易的持仓变化。
     */
    private Map<Long, PositionAccumulator> allTransactionDeltas(Long userId, Set<Long> holdingIds) {
        if (holdingIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, PositionAccumulator> deltas = new LinkedHashMap<>();
        transactionMapper.selectList(new LambdaQueryWrapper<InvestmentTransaction>()
                        .eq(InvestmentTransaction::getUserId, userId)
                        .in(InvestmentTransaction::getHoldingId, holdingIds)
                        .orderByAsc(InvestmentTransaction::getTransactionTime)
                        .orderByAsc(InvestmentTransaction::getId))
                .stream()
                .filter(this::isEffective)
                .forEach(transaction -> applyTransactionDelta(deltas, transaction));
        return deltas;
    }

    /**
     * 汇总交易净变化时保留卖出的负数量，用于从当前持仓反推出手工底仓。
     */
    private void applyTransactionDelta(Map<Long, PositionAccumulator> positions, InvestmentTransaction transaction) {
        PositionAccumulator state = positions.computeIfAbsent(transaction.getHoldingId(), key -> new PositionAccumulator(transaction.getAssetId()));
        BigDecimal quantity = scaleQuantity(transaction.getQuantity());
        BigDecimal cost = costAmount(transaction);
        if (TYPE_BUY.equals(transaction.getType())) {
            state.quantity = state.quantity.add(quantity).setScale(10, RoundingMode.HALF_UP);
            state.totalCost = state.totalCost.add(cost).setScale(4, RoundingMode.HALF_UP);
            state.assetId = transaction.getAssetId();
            return;
        }
        if (TYPE_SELL.equals(transaction.getType())) {
            // 这里不能 max(0)：清仓后的手工底仓正是 current - (-sellQuantity) 反推出来的。
            state.quantity = state.quantity.subtract(quantity).setScale(10, RoundingMode.HALF_UP);
            state.totalCost = state.totalCost.subtract(cost).setScale(4, RoundingMode.HALF_UP);
            state.assetId = transaction.getAssetId();
        }
    }

    /**
     * 判断交易是否参与统计。
     */
    private boolean isEffective(InvestmentTransaction transaction) {
        return STATUS_NORMAL.equals(transaction.getStatus()) || STATUS_CONFIRMED.equals(transaction.getStatus());
    }

    /**
     * 判断交易现金流是否参与投资净入金；待确认基金申购已扣资金账户，但份额不能提前生效。
     */
    private boolean isCashFlowEffective(InvestmentTransaction transaction) {
        return STATUS_NORMAL.equals(transaction.getStatus())
                || STATUS_CONFIRMED.equals(transaction.getStatus())
                || STATUS_PENDING_CONFIRM.equals(transaction.getStatus());
    }

    /**
     * 计算交易生效日期。
     */
    private LocalDate effectiveDate(InvestmentTransaction transaction) {
        if (INPUT_MODE_AMOUNT_NAV.equals(transaction.getInputMode()) && transaction.getConfirmedDate() != null) {
            return transaction.getConfirmedDate();
        }
        if (transaction.getTradeDate() != null) {
            return transaction.getTradeDate();
        }
        return transaction.getTransactionTime().toLocalDate();
    }

    /**
     * 计算投资现金流日期；基金金额申购在下单日已扣款，不能等确认日才算净入金。
     */
    private LocalDate cashFlowDate(InvestmentTransaction transaction) {
        if (transaction.getTradeDate() != null) {
            return transaction.getTradeDate();
        }
        return transaction.getTransactionTime().toLocalDate();
    }

    /**
     * 判断日期是否落在闭区间内。
     */
    private boolean inRange(LocalDate date, LocalDate start, LocalDate end) {
        return date != null && !date.isBefore(start) && !date.isAfter(end);
    }

    /**
     * 计算单笔交易净流入。
     */
    private BigDecimal netInflowAmount(InvestmentTransaction transaction) {
        if (TYPE_BUY.equals(transaction.getType())) {
            return costAmount(transaction);
        }
        return scale4(transaction.getAmount()).subtract(scale4(transaction.getFee())).negate().setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 计算交易成本金额。
     */
    private BigDecimal costAmount(InvestmentTransaction transaction) {
        if (transaction.getCostAmount() != null) {
            return scale4(transaction.getCostAmount());
        }
        if (transaction.getTradeAmount() != null) {
            return scale4(transaction.getTradeAmount());
        }
        return scale4(transaction.getAmount()).add(scale4(transaction.getFee())).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 按金额精度保留四位小数。
     */
    private BigDecimal scale4(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : value.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 按持仓数量精度处理。
     */
    private BigDecimal scaleQuantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(10, RoundingMode.HALF_UP) : value.setScale(10, RoundingMode.HALF_UP);
    }

    private static class PositionAccumulator {
        /**
         * 资产ID。
         */
        private Long assetId;
        /**
         * 持仓数量。
         */
        private BigDecimal quantity = BigDecimal.ZERO.setScale(10, RoundingMode.HALF_UP);
        /**
         * 总成本。
         */
        private BigDecimal totalCost = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        /**
         * 持仓历史计算累加器。
         */
        private PositionAccumulator(Long assetId) {
            this.assetId = assetId;
        }

        /**
         * 转换业务对象。
         */
        private InvestmentPositionState toState(Long holdingId) {
            return new InvestmentPositionState(holdingId, assetId, quantity, totalCost);
        }
    }
}
