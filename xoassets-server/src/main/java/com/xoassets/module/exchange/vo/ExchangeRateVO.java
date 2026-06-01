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

    private String baseCurrency;
    private String targetCurrency;
    private BigDecimal rate;
    private String source;
    private LocalDateTime quoteTime;
}
