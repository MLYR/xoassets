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

    private static final String STATUS_NORMAL = "NORMAL";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String TYPE_BUY = "BUY";
    private static final String TYPE_SELL = "SELL";
    private static final String INPUT_MODE_AMOUNT_NAV = "AMOUNT_NAV";

    private final HoldingMapper holdingMapper;
    private final InvestmentTransactionMapper transactionMapper;
    private final InvestmentDailySnapshotMapper dailySnapshotMapper;

    public InvestmentPositionHistoryServiceImpl(
            HoldingMapper holdingMapper,
            InvestmentTransactionMapper transactionMapper,
            InvestmentDailySnapshotMapper dailySnapshotMapper) {
        this.holdingMapper = holdingMapper;
        this.transactionMapper = transactionMapper;
        this.dailySnapshotMapper = dailySnapshotMapper;
    }

    @Override
    public Map<Long, InvestmentPositionState> positionsAt(Long userId, LocalDate date) {
        LocalDateTime end = date.atTime(LocalTime.MAX);
        Map<Long, PositionAccumulator> positions = new LinkedHashMap<>();
        transactionMapper.selectList(new LambdaQueryWrapper<InvestmentTransaction>()
                        .eq(InvestmentTransaction::getUserId, userId)
                        .le(InvestmentTransaction::getTransactionTime, end)
                        .orderByAsc(InvestmentTransaction::getTransactionTime)
                        .orderByAsc(InvestmentTransaction::getId))
                .stream()
                .filter(this::isEffective)
                .filter(transaction -> !effectiveDate(transaction).isAfter(date))
                .forEach(transaction -> applyTransaction(positions, transaction));
        addManualHoldingsWithoutTransactions(userId, date, positions);
        return positions.entrySet().stream()
                .filter(entry -> entry.getValue().quantity.compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().toState(entry.getKey()),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

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

    @Override
    public BigDecimal netInflow(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate == null ? endDate : startDate;
        LocalDate end = endDate == null ? start : endDate;
        return transactionMapper.selectList(new LambdaQueryWrapper<InvestmentTransaction>()
                        .eq(InvestmentTransaction::getUserId, userId)
                        .le(InvestmentTransaction::getTransactionTime, end.atTime(LocalTime.MAX))
                        .orderByAsc(InvestmentTransaction::getTransactionTime))
                .stream()
                .filter(this::isEffective)
                .filter(transaction -> !effectiveDate(transaction).isBefore(start) && !effectiveDate(transaction).isAfter(end))
                .map(this::netInflowAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

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
                .filter(this::isEffective)
                .filter(transaction -> !effectiveDate(transaction).isBefore(startDate) && !effectiveDate(transaction).isAfter(endDate))
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

    private void addManualHoldingsWithoutTransactions(Long userId, LocalDate date, Map<Long, PositionAccumulator> positions) {
        Set<Long> transactionHoldingIds = positions.keySet();
        holdingMapper.selectList(new LambdaQueryWrapper<Holding>()
                        .eq(Holding::getUserId, userId)
                        .eq(Holding::getStatus, 1)
                        .le(Holding::getCreatedAt, date.atTime(LocalTime.MAX)))
                .stream()
                .filter(holding -> !transactionHoldingIds.contains(holding.getId()))
                // 手工初始化持仓没有历史流水，只能按创建时的当前状态作为历史起点。
                .forEach(holding -> {
                    PositionAccumulator state = new PositionAccumulator(holding.getAssetId());
                    state.quantity = scaleQuantity(holding.getQuantity());
                    state.totalCost = scale4(holding.getTotalCost());
                    positions.put(holding.getId(), state);
                });
    }

    private boolean isEffective(InvestmentTransaction transaction) {
        return STATUS_NORMAL.equals(transaction.getStatus()) || STATUS_CONFIRMED.equals(transaction.getStatus());
    }

    private LocalDate effectiveDate(InvestmentTransaction transaction) {
        if (INPUT_MODE_AMOUNT_NAV.equals(transaction.getInputMode()) && transaction.getConfirmedDate() != null) {
            return transaction.getConfirmedDate();
        }
        if (transaction.getTradeDate() != null) {
            return transaction.getTradeDate();
        }
        return transaction.getTransactionTime().toLocalDate();
    }

    private BigDecimal netInflowAmount(InvestmentTransaction transaction) {
        if (TYPE_BUY.equals(transaction.getType())) {
            return costAmount(transaction);
        }
        return scale4(transaction.getAmount()).subtract(scale4(transaction.getFee())).negate().setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal costAmount(InvestmentTransaction transaction) {
        if (transaction.getCostAmount() != null) {
            return scale4(transaction.getCostAmount());
        }
        if (transaction.getTradeAmount() != null) {
            return scale4(transaction.getTradeAmount());
        }
        return scale4(transaction.getAmount()).add(scale4(transaction.getFee())).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal scale4(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleQuantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(10, RoundingMode.HALF_UP) : value.setScale(10, RoundingMode.HALF_UP);
    }

    private static class PositionAccumulator {
        private Long assetId;
        private BigDecimal quantity = BigDecimal.ZERO.setScale(10, RoundingMode.HALF_UP);
        private BigDecimal totalCost = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        private PositionAccumulator(Long assetId) {
            this.assetId = assetId;
        }

        private InvestmentPositionState toState(Long holdingId) {
            return new InvestmentPositionState(holdingId, assetId, quantity, totalCost);
        }
    }
}
