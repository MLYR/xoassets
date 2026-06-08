package com.xoassets.module.investment.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

/**
 * 单个持仓的日历收益格子，按价格日和历史持仓份额计算当天收益。
 */
@Data
@Builder
public class InvestmentCalendarDayProfitVO {

    private LocalDate date;
    private BigDecimal profitAmount;
    private BigDecimal profitRate;
    private BigDecimal marketValue;
    private BigDecimal price;
    private BigDecimal previousPrice;
    private Boolean hasPrice;
    private Boolean tradingDay;
    private Boolean marketClosed;
    private String statusLabel;
    private String priceLabel;
}
