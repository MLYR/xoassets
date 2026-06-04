package com.xoassets.module.investment.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 投资交易返回对象。
 */
@Data
@Builder
public class InvestmentTransactionVO {

    private Long id;
    private Long holdingId;
    private Long assetId;
    private Long accountId;
    private String accountName;
    private String assetName;
    private String symbol;
    private String type;
    private String inputMode;
    private BigDecimal tradeAmount;
    private BigDecimal tradeQuantity;
    private BigDecimal tradePrice;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal costAmount;
    private BigDecimal realizedProfit;
    private LocalDate tradeDate;
    private LocalDate confirmedDate;
    private BigDecimal confirmedNav;
    private BigDecimal confirmedQuantity;
    private String status;
    private java.time.LocalDateTime revokeTime;
    private String revokeReason;
    private LocalDateTime transactionTime;
    private String note;
}
