package com.xoassets.module.investment.controller;

import com.xoassets.common.api.Result;
import com.xoassets.module.investment.service.HoldingService;
import com.xoassets.module.investment.vo.HoldingVO;
import com.xoassets.module.investment.vo.InvestmentCalendarDayProfitVO;
import com.xoassets.module.investment.vo.InvestmentOverviewVO;
import com.xoassets.module.investment.vo.InvestmentTrendVO;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 投资模块化展示接口，供 Web / App 按基金、股票、虚拟货币拆分收益口径。
 */
@RestController
@RequestMapping("/api/investments")
public class InvestmentController {

    private final HoldingService holdingService;

    public InvestmentController(HoldingService holdingService) {
        this.holdingService = holdingService;
    }

    /**
     * 查询投资总览，今日收益和昨日收益按资产范围拆分。
     */
    @GetMapping("/overview")
    public Result<InvestmentOverviewVO> overview() {
        return Result.success(holdingService.overview());
    }

    /**
     * 查询投资模块持仓列表，module 支持 ALL / FUND / STOCK / CRYPTO。
     */
    @GetMapping("/holdings")
    public Result<List<HoldingVO>> holdings(@RequestParam(required = false, defaultValue = "ALL") String module) {
        return Result.success(holdingService.list(module));
    }

    /**
     * 查询投资模块趋势，ALL 走投资日快照，单模块走历史持仓和日级价格重建。
     */
    @GetMapping("/trend")
    public Result<InvestmentTrendVO> trend(
            @RequestParam(required = false, defaultValue = "ALL") String module,
            @RequestParam(required = false, defaultValue = "MONTH") String period,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(holdingService.trend(module, period, startDate, endDate));
    }

    /**
     * 查询单持仓收益日历，前端按月历格子直接展示每天收益。
     */
    @GetMapping("/holdings/{id}/profit-calendar")
    public Result<List<InvestmentCalendarDayProfitVO>> profitCalendar(
            @PathVariable Long id,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        YearMonth targetMonth = year == null || month == null ? YearMonth.now() : YearMonth.of(year, month);
        return Result.success(holdingService.profitCalendar(id, targetMonth));
    }
}
