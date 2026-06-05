package com.xoassets.module.account.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 账户资金流向统计，供账户详情页图表展示。
 */
@Data
@Builder
public class AccountFlowStatisticsVO {

    private BigDecimal incomeAmount;
    private BigDecimal expenseAmount;
    private BigDecimal transferInAmount;
    private BigDecimal transferOutAmount;
    private BigDecimal investmentBuyAmount;
    private BigDecimal investmentSellAmount;
    private BigDecimal adjustmentAmount;
    private BigDecimal netFlowAmount;
    private List<NameAmountItem> categoryExpenseStats;
    private List<NameAmountItem> investmentFlowStats;
    private List<DailyFlowItem> dailyFlowTrend;
    private List<DailyBalanceItem> dailyBalanceTrend;

    /**
     * 名称加金额的通用统计项。
     */
    @Data
    @Builder
    public static class NameAmountItem {
        private String name;
        private BigDecimal amount;
    }

    /**
     * 按日期聚合的流入、流出和净流入。
     */
    @Data
    @Builder
    public static class DailyFlowItem {
        private String date;
        private BigDecimal inflow;
        private BigDecimal outflow;
        private BigDecimal netFlow;
    }

    /**
     * 按日期聚合的日终余额曲线点。
     */
    @Data
    @Builder
    public static class DailyBalanceItem {
        private String date;
        private BigDecimal endBalance;
        private BigDecimal inflow;
        private BigDecimal outflow;
        private BigDecimal adjustmentAmount;
    }
}
