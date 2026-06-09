package com.xoassets.module.account.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 账户详情统计，供 Web 详情页和移动端月度资金概览展示。
 */
@Data
@Builder
public class AccountFlowStatisticsVO {

    /**
     * 收入金额。
     */
    private BigDecimal incomeAmount;
    /**
     * 支出金额。
     */
    private BigDecimal expenseAmount;
    /**
     * 转入金额。
     */
    private BigDecimal transferInAmount;
    /**
     * 转出金额。
     */
    private BigDecimal transferOutAmount;
    /**
     * 投资买入金额。
     */
    private BigDecimal investmentBuyAmount;
    /**
     * 投资卖出金额。
     */
    private BigDecimal investmentSellAmount;
    /**
     * 余额修正金额。
     */
    private BigDecimal adjustmentAmount;
    /**
     * 净流入金额。
     */
    private BigDecimal netFlowAmount;
    /**
     * 分类支出统计。
     */
    private List<NameAmountItem> categoryExpenseStats;
    /**
     * 每日余额趋势。
     */
    private List<DailyBalanceItem> dailyBalanceTrend;

    /**
     * 名称加金额的通用统计项。
     */
    @Data
    @Builder
    public static class NameAmountItem {
        /**
         * 名称。
         */
        private String name;
        /**
         * 金额。
         */
        private BigDecimal amount;
    }

    /**
     * 按日期聚合的日终余额曲线点。
     */
    @Data
    @Builder
    public static class DailyBalanceItem {
        /**
         * 日期。
         */
        private String date;
        /**
         * 日终余额。
         */
        private BigDecimal endBalance;
        /**
         * 流入金额。
         */
        private BigDecimal inflow;
        /**
         * 流出金额。
         */
        private BigDecimal outflow;
        /**
         * 余额修正金额。
         */
        private BigDecimal adjustmentAmount;
    }
}
