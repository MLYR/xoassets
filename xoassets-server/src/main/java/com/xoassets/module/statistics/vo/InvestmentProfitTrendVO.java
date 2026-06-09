package com.xoassets.module.statistics.vo;

import java.math.BigDecimal;
import java.time.YearMonth;
import lombok.Builder;
import lombok.Data;

/**
 * 投资盈亏趋势点。
 */
@Data
@Builder
public class InvestmentProfitTrendVO {

    /**
     * 月份。
     */
    private YearMonth month;
    /**
     * 持仓市值。
     */
    private BigDecimal marketValue;
    /**
     * 总成本。
     */
    private BigDecimal totalCost;
    /**
     * 浮动盈亏。
     */
    private BigDecimal floatingProfit;
}
