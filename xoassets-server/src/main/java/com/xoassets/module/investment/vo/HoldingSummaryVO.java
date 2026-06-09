package com.xoassets.module.investment.vo;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * 持仓汇总返回对象，复用持仓明细同一套收益口径。
 */
@Data
@Builder
public class HoldingSummaryVO {

    /**
     * 持仓总市值。
     */
    private BigDecimal totalMarketValue;
    /**
     * 总成本。
     */
    private BigDecimal totalCost;
    /**
     * 今日收益是否可用。
     */
    private Boolean todayProfitAvailable;
    /**
     * 今日收益。
     */
    private BigDecimal todayProfit;
    /**
     * 今日收益率。
     */
    private BigDecimal todayProfitRate;
    /**
     * 昨日收益。
     */
    private BigDecimal yesterdayProfit;
    /**
     * 昨日收益率。
     */
    private BigDecimal yesterdayProfitRate;
    /**
     * 上月以来收益。
     */
    private BigDecimal lastMonthProfit;
    /**
     * 上月以来收益率。
     */
    private BigDecimal lastMonthProfitRate;
    /**
     * 浮动盈亏。
     */
    private BigDecimal floatingProfit;
    /**
     * 浮动盈亏率。
     */
    private BigDecimal floatingProfitRate;
    /**
     * 持仓数量。
     */
    private Integer holdingCount;
}
