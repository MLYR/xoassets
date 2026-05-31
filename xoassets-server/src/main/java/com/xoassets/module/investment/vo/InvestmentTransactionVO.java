package com.xoassets.module.investment.vo;

import java.math.BigDecimal;
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
    private String assetName;
    private String symbol;
    private String type;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal fee;
    private LocalDateTime transactionTime;
    private String note;
}
