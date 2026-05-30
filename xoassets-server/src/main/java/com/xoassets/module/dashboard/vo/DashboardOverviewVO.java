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
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpense;
    private BigDecimal monthlyBalance;
    private BigDecimal assetTrendRate;
    private BigDecimal incomeTrendRate;
    private BigDecimal expenseTrendRate;
    private BigDecimal balanceTrendRate;
}
