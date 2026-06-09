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

    /**
     * 日期。
     */
    private LocalDate date;
    /**
     * 收益金额。
     */
    private BigDecimal profitAmount;
    /**
     * 收益率。
     */
    private BigDecimal profitRate;
    /**
     * 持仓市值。
     */
    private BigDecimal marketValue;
    /**
     * 价格。
     */
    private BigDecimal price;
    /**
     * 上一交易日价格。
     */
    private BigDecimal previousPrice;
    /**
     * 是否有有效价格。
     */
    private Boolean hasPrice;
    /**
     * 是否交易日。
     */
    private Boolean tradingDay;
    /**
     * 是否休市。
     */
    private Boolean marketClosed;
    /**
     * 状态文案。
     */
    private String statusLabel;
    /**
     * 价格展示文案。
     */
    private String priceLabel;
}
