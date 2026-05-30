package com.xoassets.module.statistics.service;

import com.xoassets.module.statistics.vo.AssetTrendPointVO;
import com.xoassets.module.statistics.vo.ExpenseCategoryVO;
import com.xoassets.module.statistics.vo.IncomeExpenseTrendVO;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 统计服务接口：提供第一期基础图表数据。
 */
public interface StatisticsService {

    /**
     * 查询资产趋势。
     */
    List<AssetTrendPointVO> assetTrend(LocalDate startDate, LocalDate endDate);

    /**
     * 查询支出分类占比。
     */
    List<ExpenseCategoryVO> expenseCategory(YearMonth month);

    /**
     * 查询月度收入支出趋势。
     */
    List<IncomeExpenseTrendVO> incomeExpenseTrend(YearMonth startMonth, YearMonth endMonth);
}
