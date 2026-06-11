package com.xoassets.module.analytics.vo;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * 数据分析页核心 KPI。
 */
@Data
@Builder
public class AnalyticsKpiVO {

    /**
     * 最新净资产。
     */
    private BigDecimal netAsset;
    /**
     * 最新总资产。
     */
    private BigDecimal totalAsset;
    /**
     * 本期收入。
     */
    private BigDecimal periodIncome;
    /**
     * 本期支出。
     */
    private BigDecimal periodExpense;
    /**
     * 本期结余。
     */
    private BigDecimal periodBalance;
    /**
     * 投资资产。
     */
    private BigDecimal investmentAsset;
    /**
     * 投资持有收益。
     */
    private BigDecimal investmentProfit;
    /**
     * 预算剩余。
     */
    private BigDecimal budgetRemaining;
}
