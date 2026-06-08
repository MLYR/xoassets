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

    private BigDecimal totalAssets;
    private BigDecimal netAssets;
    private BigDecimal todayIncome;
    private BigDecimal todayExpense;
    private BigDecimal yesterdayIncome;
    private BigDecimal yesterdayExpense;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpense;
    private BigDecimal todayBalance;
    private BigDecimal monthlyBalance;
    private BigDecimal todayBalanceRateByIncome;
    private BigDecimal todayBalanceRateByExpense;
    private BigDecimal monthlyBalanceRateByIncome;
    private BigDecimal monthlyBalanceRateByExpense;
    private BigDecimal investmentMarketValue;
    private BigDecimal investmentFloatingProfit;
    private BigDecimal investmentTotalProfit;
    private BigDecimal investmentTodayProfit;
    private BigDecimal budgetUsageRate;
    private BigDecimal assetTrendRate;
    private BigDecimal incomeTrendRate;
    private BigDecimal expenseTrendRate;
    private BigDecimal balanceTrendRate;
}
