package com.xoassets.module.exchange.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 汇率展示对象。
 */
@Data
@Builder
public class ExchangeRateVO {

    /**
     * 基准币种。
     */
    private String baseCurrency;
    /**
     * 目标币种。
     */
    private String targetCurrency;
    /**
     * 汇率。
     */
    private BigDecimal rate;
    /**
     * 来源。
     */
    private String source;
    /**
     * 报价时间。
     */
    private LocalDateTime quoteTime;
}
