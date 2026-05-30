package com.xoassets.module.dashboard.controller;

import com.xoassets.common.api.Result;
import com.xoassets.module.dashboard.service.DashboardService;
import com.xoassets.module.dashboard.vo.DashboardOverviewVO;
import com.xoassets.module.transaction.vo.TransactionVO;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页仪表盘接口。
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 查询指定月份的首页概览，不传月份时默认当前月。
     */
    @GetMapping("/overview")
    public Result<DashboardOverviewVO> overview(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return Result.success(dashboardService.overview(month));
    }

    /**
     * 查询首页最近流水，限制最大数量由服务层兜底。
     */
    @GetMapping("/recent-transactions")
    public Result<List<TransactionVO>> recentTransactions(@RequestParam(defaultValue = "5") int limit) {
        return Result.success(dashboardService.recentTransactions(limit));
    }
}
