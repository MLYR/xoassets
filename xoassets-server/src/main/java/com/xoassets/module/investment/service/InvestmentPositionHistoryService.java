package com.xoassets.module.investment.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * 投资历史头寸服务，用交易流水重建历史持仓和净入金。
 */
public interface InvestmentPositionHistoryService {

    /**
     * 重建某用户截至指定日期日终的持仓状态。
     */
    Map<Long, InvestmentPositionState> positionsAt(Long userId, LocalDate date);

    /**
     * 查询某持仓在指定日期日终的数量。
     */
    BigDecimal quantityAt(Long userId, Long holdingId, Long assetId, LocalDate date);

    /**
     * 统计指定日期区间内投资资产净入金；买入为正，卖出为负。
     */
    BigDecimal netInflow(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 投资日快照补跑用户集合，覆盖当前有持仓、近期有交易和已有快照的用户。
     */
    Set<Long> snapshotUserIds(LocalDate startDate, LocalDate endDate);
}
