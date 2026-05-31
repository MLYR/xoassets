package com.xoassets.module.statistics.controller;

import com.xoassets.common.api.Result;
import com.xoassets.module.budget.vo.BudgetSummaryVO;
import com.xoassets.module.statistics.service.StatisticsService;
import com.xoassets.module.statistics.vo.AssetDistributionVO;
import com.xoassets.module.statistics.vo.AssetTrendPointVO;
import com.xoassets.module.statistics.vo.ExpenseCategoryVO;
import com.xoassets.module.statistics.vo.IncomeExpenseTrendVO;
import com.xoassets.module.statistics.vo.InvestmentProfitTrendVO;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 基础图表统计接口。
 */
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * 查询资产趋势，不传日期时默认最近 30 天。
     */
    @GetMapping("/asset-trend")
    public Result<List<AssetTrendPointVO>> assetTrend(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(statisticsService.assetTrend(startDate, endDate));
    }

    /**
     * 查询净资产趋势，不传日期时默认最近 30 天。
     */
    @GetMapping("/net-assets-trend")
    public Result<List<AssetTrendPointVO>> netAssetsTrend(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(statisticsService.netAssetsTrend(startDate, endDate));
    }

    /**
     * 查询月度支出分类占比。
     */
    @GetMapping("/expense-category")
    public Result<List<ExpenseCategoryVO>> expenseCategory(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return Result.success(statisticsService.expenseCategory(month));
    }

    /**
     * 查询收入支出月度趋势，不传区间时默认最近 6 个月。
     */
    @GetMapping("/income-expense-trend")
    public Result<List<IncomeExpenseTrendVO>> incomeExpenseTrend(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth startMonth,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth endMonth) {
        return Result.success(statisticsService.incomeExpenseTrend(startMonth, endMonth));
    }

    /**
     * 查询账户和投资资产分布。
     */
    @GetMapping("/asset-distribution")
    public Result<List<AssetDistributionVO>> assetDistribution() {
        return Result.success(statisticsService.assetDistribution());
    }

    /**
     * 查询投资盈亏趋势，不传区间时默认最近 6 个月。
     */
    @GetMapping("/investment-profit-trend")
    public Result<List<InvestmentProfitTrendVO>> investmentProfitTrend(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth startMonth,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth endMonth) {
        return Result.success(statisticsService.investmentProfitTrend(startMonth, endMonth));
    }

    /**
     * 查询预算使用进度。
     */
    @GetMapping("/budget-progress")
    public Result<BudgetSummaryVO> budgetProgress(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return Result.success(statisticsService.budgetProgress(month));
    }
}
