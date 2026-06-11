package com.xoassets.module.analytics.vo;

import com.xoassets.module.budget.vo.BudgetSummaryVO;
import com.xoassets.module.snapshot.vo.AssetSnapshotVO;
import com.xoassets.module.statistics.vo.AssetDistributionVO;
import com.xoassets.module.statistics.vo.ExpenseCategoryVO;
import com.xoassets.module.statistics.vo.IncomeExpenseTrendVO;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 数据分析页聚合返回对象。
 */
@Data
@Builder
public class AnalyticsOverviewVO {

    /**
     * 核心 KPI。
     */
    private AnalyticsKpiVO kpi;
    /**
     * 资产快照趋势。
     */
    private List<AssetSnapshotVO> assetTrend;
    /**
     * 收支趋势。
     */
    private List<IncomeExpenseTrendVO> incomeExpenseTrend;
    /**
     * 支出分类。
     */
    private List<ExpenseCategoryVO> expenseCategories;
    /**
     * 当前资产分布。
     */
    private List<AssetDistributionVO> assetDistribution;
    /**
     * 预算汇总。
     */
    private BudgetSummaryVO budgetSummary;
    /**
     * 投资分析。
     */
    private InvestmentAnalyticsVO investment;
}
