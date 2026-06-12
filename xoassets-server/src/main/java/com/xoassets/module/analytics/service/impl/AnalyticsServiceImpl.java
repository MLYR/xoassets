package com.xoassets.module.analytics.service.impl;

import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.analytics.service.AnalyticsService;
import com.xoassets.module.analytics.vo.AnalyticsKpiVO;
import com.xoassets.module.analytics.vo.AnalyticsOverviewVO;
import com.xoassets.module.analytics.vo.InvestmentAnalyticsVO;
import com.xoassets.module.budget.service.BudgetService;
import com.xoassets.module.budget.vo.BudgetSummaryVO;
import com.xoassets.module.investment.service.HoldingService;
import com.xoassets.module.investment.vo.HoldingVO;
import com.xoassets.module.investment.vo.InvestmentCalendarDayProfitVO;
import com.xoassets.module.investment.vo.InvestmentModuleAssetVO;
import com.xoassets.module.investment.vo.InvestmentOverviewVO;
import com.xoassets.module.investment.vo.InvestmentTrendVO;
import com.xoassets.module.snapshot.service.SnapshotService;
import com.xoassets.module.snapshot.vo.AssetSnapshotVO;
import com.xoassets.module.statistics.service.StatisticsService;
import com.xoassets.module.statistics.vo.AssetDistributionVO;
import com.xoassets.module.statistics.vo.ExpenseCategoryVO;
import com.xoassets.module.statistics.vo.IncomeExpenseTrendVO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 数据分析聚合服务实现：只编排现有业务服务，不重复实现统计 SQL。
 */
@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    /**
     * 资产快照服务。
     */
    private final SnapshotService snapshotService;
    /**
     * 基础统计服务。
     */
    private final StatisticsService statisticsService;
    /**
     * 预算服务。
     */
    private final BudgetService budgetService;
    /**
     * 投资持仓服务。
     */
    private final HoldingService holdingService;

    /**
     * 注入业务依赖。
     */
    public AnalyticsServiceImpl(
            SnapshotService snapshotService,
            StatisticsService statisticsService,
            BudgetService budgetService,
            HoldingService holdingService) {
        this.snapshotService = snapshotService;
        this.statisticsService = statisticsService;
        this.budgetService = budgetService;
        this.holdingService = holdingService;
    }

    /**
     * 查询数据分析聚合结果；用户隔离由下游服务按当前登录用户执行。
     */
    @Override
    public AnalyticsOverviewVO overview(
            LocalDate startDate,
            LocalDate endDate,
            YearMonth startMonth,
            YearMonth endMonth,
            YearMonth selectedMonth,
            String investmentModule,
            String investmentPeriod) {
        // 聚合层主动读取登录用户，避免在未建立用户上下文时误调用分析聚合接口。
        LoginUserContext.getUserId();
        YearMonth targetMonth = selectedMonth == null ? YearMonth.now() : selectedMonth;
        String targetModule = investmentModule == null || investmentModule.isBlank() ? "ALL" : investmentModule;
        String targetInvestmentPeriod = investmentPeriod == null || investmentPeriod.isBlank() ? "MONTH" : investmentPeriod;

        List<AssetSnapshotVO> assetTrend = safeList(snapshotService.trend(startDate, endDate));
        List<IncomeExpenseTrendVO> incomeExpenseTrend = safeList(statisticsService.incomeExpenseTrend(startMonth, endMonth));
        List<ExpenseCategoryVO> expenseCategories = safeList(statisticsService.expenseCategory(targetMonth));
        List<AssetDistributionVO> assetDistribution = safeList(statisticsService.assetDistribution());
        BudgetSummaryVO budgetSummary = safeBudgetSummary(budgetService.summary(targetMonth.toString()), targetMonth);
        InvestmentOverviewVO investmentOverview = holdingService.overview();
        InvestmentTrendVO investmentTrend = holdingService.trend(targetModule, targetInvestmentPeriod, startDate, endDate);
        List<InvestmentCalendarDayProfitVO> dailyProfitCalendar = safeList(holdingService.dailyProfitCalendar(targetMonth));
        List<HoldingVO> holdings = safeList(holdingService.list(targetModule));

        return AnalyticsOverviewVO.builder()
                .kpi(buildKpi(assetTrend, incomeExpenseTrend, budgetSummary, investmentOverview))
                .assetTrend(assetTrend)
                .incomeExpenseTrend(incomeExpenseTrend)
                .expenseCategories(expenseCategories)
                .assetDistribution(assetDistribution)
                .budgetSummary(budgetSummary)
                .investment(buildInvestment(investmentOverview, investmentTrend, latestSnapshot(assetTrend), dailyProfitCalendar, holdings))
                .build();
    }

    /**
     * 生成分析页 KPI，缺失金额统一按 0 返回，今日收益可用性仍保留在投资对象中。
     */
    private AnalyticsKpiVO buildKpi(
            List<AssetSnapshotVO> assetTrend,
            List<IncomeExpenseTrendVO> incomeExpenseTrend,
            BudgetSummaryVO budgetSummary,
            InvestmentOverviewVO investmentOverview) {
        AssetSnapshotVO latestSnapshot = assetTrend.isEmpty() ? null : assetTrend.get(assetTrend.size() - 1);
        BigDecimal periodIncome = incomeExpenseTrend.stream()
                .map(IncomeExpenseTrendVO::getIncome)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal periodExpense = incomeExpenseTrend.stream()
                .map(IncomeExpenseTrendVO::getExpense)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal periodBalance = incomeExpenseTrend.stream()
                .map(IncomeExpenseTrendVO::getBalance)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AnalyticsKpiVO.builder()
                .netAsset(latestSnapshot == null ? BigDecimal.ZERO : zeroIfNull(latestSnapshot.getNetAsset()))
                .totalAsset(latestSnapshot == null ? BigDecimal.ZERO : zeroIfNull(latestSnapshot.getTotalAsset()))
                .periodIncome(periodIncome)
                .periodExpense(periodExpense)
                .periodBalance(periodBalance)
                // 分析页资产趋势来自资产快照，KPI 投资资产也优先同源，避免漏掉待确认基金在途资产。
                .investmentAsset(latestSnapshot == null ? investmentOverviewAmount(investmentOverview) : zeroIfNull(latestSnapshot.getInvestmentAsset()))
                .investmentProfit(investmentOverview == null ? BigDecimal.ZERO : zeroIfNull(investmentOverview.getHoldingProfit()))
                .budgetRemaining(budgetSummary == null ? BigDecimal.ZERO : zeroIfNull(budgetSummary.getTotalRemaining()))
                .build();
    }

    /**
     * 组装投资分析对象，保留今日收益可用性，避免前端把不可用收益当作 0。
     */
    private InvestmentAnalyticsVO buildInvestment(
            InvestmentOverviewVO overview,
            InvestmentTrendVO trend,
            AssetSnapshotVO latestSnapshot,
            List<InvestmentCalendarDayProfitVO> dailyProfitCalendar,
            List<HoldingVO> holdings) {
        List<InvestmentModuleAssetVO> moduleAssets = overview == null ? Collections.emptyList() : safeList(overview.getModuleAssets());
        return InvestmentAnalyticsVO.builder()
                // 投资分析顶部资产额与资产趋势同源，待确认申购作为在途投资资产展示。
                .totalInvestmentAsset(latestSnapshot == null ? investmentOverviewAmount(overview) : zeroIfNull(latestSnapshot.getInvestmentAsset()))
                .holdingProfit(overview == null ? BigDecimal.ZERO : zeroIfNull(overview.getHoldingProfit()))
                .holdingProfitRate(overview == null ? BigDecimal.ZERO : zeroIfNull(overview.getHoldingProfitRate()))
                .todayProfit(overview == null ? null : overview.getTodayProfit())
                .todayProfitAvailable(overview == null ? false : overview.getTodayProfitAvailable())
                .todayProfitStatusLabel(overview == null ? "暂无投资总览数据" : overview.getTodayProfitStatusLabel())
                .yesterdayProfit(overview == null ? null : overview.getYesterdayProfit())
                .moduleAssets(moduleAssets)
                .trend(trend == null ? emptyTrend() : trend)
                .dailyProfitCalendar(dailyProfitCalendar)
                .holdings(holdings)
                .build();
    }

    /**
     * 构造空预算汇总，保证聚合接口不返回 null 明细列表。
     */
    private BudgetSummaryVO safeBudgetSummary(BudgetSummaryVO summary, YearMonth month) {
        if (summary != null) {
            if (summary.getItems() == null) {
                summary.setItems(Collections.emptyList());
            }
            return summary;
        }
        return BudgetSummaryVO.builder()
                .month(month.toString())
                .totalBudget(BigDecimal.ZERO)
                .totalUsed(BigDecimal.ZERO)
                .totalRemaining(BigDecimal.ZERO)
                .usageRate(BigDecimal.ZERO)
                .usageStatus("NORMAL")
                .usageStatusLabel("正常")
                .items(Collections.emptyList())
                .build();
    }

    /**
     * 构造空投资趋势，保证前端无需处理 null points。
     */
    private InvestmentTrendVO emptyTrend() {
        return InvestmentTrendVO.builder()
                .module("ALL")
                .period("MONTH")
                .points(Collections.emptyList())
                .build();
    }

    /**
     * null 列表统一转为空列表。
     */
    private <T> List<T> safeList(List<T> items) {
        return items == null ? Collections.emptyList() : items;
    }

    /**
     * 取资产趋势最后一个快照点，作为分析页资产 KPI 的同源基准。
     */
    private AssetSnapshotVO latestSnapshot(List<AssetSnapshotVO> assetTrend) {
        return assetTrend == null || assetTrend.isEmpty() ? null : assetTrend.get(assetTrend.size() - 1);
    }

    /**
     * 缺少资产快照时退回投资总览当前持仓资产额，保证历史数据为空时页面仍可展示。
     */
    private BigDecimal investmentOverviewAmount(InvestmentOverviewVO investmentOverview) {
        return investmentOverview == null ? BigDecimal.ZERO : zeroIfNull(investmentOverview.getTotalInvestmentAsset());
    }

    /**
     * 金额 null 统一按 0 参与 KPI 聚合。
     */
    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
