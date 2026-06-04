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

    private BigDecimal totalBuyAmount;
    private BigDecimal totalSellAmount;
    private BigDecimal totalFee;
    private BigDecimal pendingConfirmAmount;
    private BigDecimal realizedProfit;
    private BigDecimal floatingProfit;
    private BigDecimal totalProfit;
    private BigDecimal totalProfitRate;
    private Integer buyCount;
    private Integer sellCount;
    private LocalDateTime firstBuyTime;
    private LocalDateTime lastTradeTime;
}
