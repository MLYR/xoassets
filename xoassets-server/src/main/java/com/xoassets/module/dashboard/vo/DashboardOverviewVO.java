package com.xoassets.module.dashboard.vo;

import com.xoassets.module.investment.vo.InvestmentTransactionVO;
import com.xoassets.module.transaction.vo.TransactionVO;
import java.math.BigDecimal;
import java.util.List;
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
    private BigDecimal todayExpense;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpense;
    private BigDecimal monthlyBalance;
    private BigDecimal investmentMarketValue;
    private BigDecimal investmentFloatingProfit;
    private BigDecimal budgetUsageRate;
    private BigDecimal assetTrendRate;
    private BigDecimal incomeTrendRate;
    private BigDecimal expenseTrendRate;
    private BigDecimal balanceTrendRate;
    private List<TransactionVO> recentTransactions;
    private List<InvestmentTransactionVO> recentInvestmentTransactions;
}
