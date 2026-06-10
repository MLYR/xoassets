package com.xoassets.module.investment.service;

import com.xoassets.module.investment.vo.InvestmentCalendarDayProfitVO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * 持仓每日收益服务，负责生成和读取收益日历持久化数据。
 */
public interface InvestmentHoldingDailyProfitService {

    /**
     * 重建指定用户在日期区间内的持仓每日收益。
     */
    void rebuildForUser(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 查询单持仓月度收益日历。
     */
    List<InvestmentCalendarDayProfitVO> holdingCalendar(Long userId, Long holdingId, YearMonth month);

    /**
     * 查询当前用户全持仓每日收益。
     */
    List<InvestmentCalendarDayProfitVO> userCalendar(Long userId, YearMonth month);

    /**
     * 按日期聚合指定用户的持仓每日收益。
     */
    Map<LocalDate, DailyProfitSummary> aggregateByDate(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 查询指定日期前最近一个收益日，返回 ALL 和各模块汇总。
     */
    Map<String, DailyProfitSummary> latestByModuleBefore(Long userId, LocalDate date);

    /**
     * 查询单持仓在指定日期前最近一个收益日。
     */
    HoldingDailyProfitSummary latestHoldingBefore(Long userId, Long holdingId, LocalDate date);

    /**
     * 每日收益汇总。
     */
    record DailyProfitSummary(BigDecimal profit, BigDecimal baseAmount) {
    }

    /**
     * 单持仓收益明细汇总。
     */
    record HoldingDailyProfitSummary(
            BigDecimal profit,
            BigDecimal baseAmount,
            BigDecimal price,
            BigDecimal previousPrice) {
    }
}
