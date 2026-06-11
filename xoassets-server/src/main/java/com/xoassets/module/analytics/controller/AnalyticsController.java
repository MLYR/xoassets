package com.xoassets.module.analytics.controller;

import com.xoassets.common.api.Result;
import com.xoassets.module.analytics.service.AnalyticsService;
import com.xoassets.module.analytics.vo.AnalyticsOverviewVO;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据分析聚合接口：为 Web 数据分析页一次性返回主要图表和 KPI。
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    /**
     * 数据分析聚合服务。
     */
    private final AnalyticsService analyticsService;

    /**
     * 注入接口依赖。
     */
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * 查询数据分析页聚合总览，日期和月份为空时由下游服务使用默认范围。
     */
    @GetMapping("/overview")
    public Result<AnalyticsOverviewVO> overview(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth startMonth,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth endMonth,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth selectedMonth,
            @RequestParam(required = false, defaultValue = "ALL") String investmentModule,
            @RequestParam(required = false, defaultValue = "MONTH") String investmentPeriod) {
        return Result.success(analyticsService.overview(startDate, endDate, startMonth, endMonth, selectedMonth, investmentModule, investmentPeriod));
    }
}
