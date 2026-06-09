package com.xoassets.module.dashboard.vo;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * 首页概览指标返回对象。
 */
@Data
@Builder
public class DashboardOverviewVO {

    /**
     * 总资产。
     */
    private BigDecimal totalAssets;
    /**
     * 净资产。
     */
    private BigDecimal netAssets;
    /**
     * 今日收入。
     */
    private BigDecimal todayIncome;
    /**
     * 今日支出。
     */
    private BigDecimal todayExpense;
    /**
     * 昨日收入。
     */
    private BigDecimal yesterdayIncome;
    /**
     * 昨日支出。
     */
    private BigDecimal yesterdayExpense;
    /**
     * 当月收入。
     */
    private BigDecimal monthlyIncome;
    /**
     * 当月支出。
     */
    private BigDecimal monthlyExpense;
    /**
     * 今日结余。
     */
    private BigDecimal todayBalance;
    /**
     * 当月结余。
     */
    private BigDecimal monthlyBalance;
    /**
     * 今日结余率，按收入作分母。
     */
    private BigDecimal todayBalanceRateByIncome;
    /**
     * 今日结余率，按支出作分母。
     */
    private BigDecimal todayBalanceRateByExpense;
    /**
     * 当月结余率，按收入作分母。
     */
    private BigDecimal monthlyBalanceRateByIncome;
    /**
     * 当月结余率，按支出作分母。
     */
    private BigDecimal monthlyBalanceRateByExpense;
    /**
     * 投资资产市值。
     */
    private BigDecimal investmentMarketValue;
    /**
     * 投资浮动盈亏。
     */
    private BigDecimal investmentFloatingProfit;
    /**
     * 投资总收益。
     */
    private BigDecimal investmentTotalProfit;
    /**
     * 投资昨日收益。
     */
    private BigDecimal investmentYesterdayProfit;
    /**
     * 投资今日收益。
     */
    private BigDecimal investmentTodayProfit;
    /**
     * 预算使用率。
     */
    private BigDecimal budgetUsageRate;
    /**
     * 资产趋势率。
     */
    private BigDecimal assetTrendRate;
    /**
     * 收入趋势率。
     */
    private BigDecimal incomeTrendRate;
    /**
     * 支出趋势率。
     */
    private BigDecimal expenseTrendRate;
    /**
     * 结余趋势率。
     */
    private BigDecimal balanceTrendRate;
}
