package com.xoassets.module.dashboard.service;

import com.xoassets.module.dashboard.vo.DashboardOverviewVO;
import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * 首页服务接口：提供首页概览和统计复用的月度收支聚合。
 */
public interface DashboardService {

    /**
     * 查询首页概览指标。
     */
    DashboardOverviewVO overview(YearMonth month);

    /**
     * 统计指定月份收入。
     */
    BigDecimal sumIncome(Long userId, YearMonth month);

    /**
     * 统计指定月份支出。
     */
    BigDecimal sumExpense(Long userId, YearMonth month);
}
