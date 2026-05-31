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

    private YearMonth month;
    private BigDecimal marketValue;
    private BigDecimal totalCost;
    private BigDecimal floatingProfit;
}
