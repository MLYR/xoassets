package com.xoassets.module.investment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 持仓详情汇总返回对象，只统计当前持仓下未撤销的投资交易。
 */
@Data
@Builder
public class HoldingDetailSummaryVO {

    /**
     * 买入总额。
     */
    private BigDecimal totalBuyAmount;
    /**
     * 卖出总额。
     */
    private BigDecimal totalSellAmount;
    /**
     * 手续费合计。
     */
    private BigDecimal totalFee;
    /**
     * 待确认金额。
     */
    private BigDecimal pendingConfirmAmount;
    /**
     * 已实现收益。
     */
    private BigDecimal realizedProfit;
    /**
     * 浮动盈亏。
     */
    private BigDecimal floatingProfit;
    /**
     * 总收益。
     */
    private BigDecimal totalProfit;
    /**
     * 总收益率。
     */
    private BigDecimal totalProfitRate;
    /**
     * 买入次数。
     */
    private Integer buyCount;
    /**
     * 卖出次数。
     */
    private Integer sellCount;
    /**
     * 首次买入时间。
     */
    private LocalDateTime firstBuyTime;
    /**
     * 最近交易时间。
     */
    private LocalDateTime lastTradeTime;
}
