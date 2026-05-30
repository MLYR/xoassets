package com.xoassets.module.dashboard.service;

import com.xoassets.module.dashboard.vo.DashboardOverviewVO;
import com.xoassets.module.transaction.vo.TransactionVO;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * 首页服务接口：提供首页概览、最近流水和统计复用的月度收支聚合。
 */
public interface DashboardService {

    /**
     * 查询首页概览指标。
     */
    DashboardOverviewVO overview(YearMonth month);

    /**
     * 查询最近流水。
     */
    List<TransactionVO> recentTransactions(int limit);

    /**
     * 统计指定月份收入。
     */
    BigDecimal sumIncome(Long userId, YearMonth month);

    /**
     * 统计指定月份支出。
     */
    BigDecimal sumExpense(Long userId, YearMonth month);
}
