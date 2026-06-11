package com.xoassets.module.analytics.service;

import com.xoassets.module.analytics.vo.AnalyticsOverviewVO;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 数据分析聚合服务接口。
 */
public interface AnalyticsService {

    /**
     * 查询数据分析页聚合总览。
     */
    AnalyticsOverviewVO overview(
            LocalDate startDate,
            LocalDate endDate,
            YearMonth startMonth,
            YearMonth endMonth,
            YearMonth selectedMonth,
            String investmentModule,
            String investmentPeriod);
}
