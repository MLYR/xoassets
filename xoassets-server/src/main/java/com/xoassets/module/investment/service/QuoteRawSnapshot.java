package com.xoassets.module.investment.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Redis 原始行情快照，短期保存 provider 返回的未经日级聚合的价格点。
 */
public record QuoteRawSnapshot(
        Long assetId,
        BigDecimal price,
        String currency,
        BigDecimal previousClose,
        BigDecimal changeAmount,
        BigDecimal changePercent,
        String source,
        LocalDateTime quoteTime,
        String marketStatus) {
}
