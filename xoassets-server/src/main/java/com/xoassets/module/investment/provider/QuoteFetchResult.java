package com.xoassets.module.investment.provider;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 行情 provider 返回的标准价格结果。
 */
public record QuoteFetchResult(
        BigDecimal price,
        String currency,
        String source,
        LocalDateTime quoteTime,
        String rawJson) {
}
