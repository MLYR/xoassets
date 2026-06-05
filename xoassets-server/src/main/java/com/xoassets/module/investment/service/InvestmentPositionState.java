package com.xoassets.module.investment.service;

import java.math.BigDecimal;

/**
 * 指定日期重建出来的投资持仓状态。
 */
public record InvestmentPositionState(
        Long holdingId,
        Long assetId,
        BigDecimal quantity,
        BigDecimal totalCost) {
}
