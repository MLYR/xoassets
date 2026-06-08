package com.xoassets.module.account.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 账户余额修正展示对象。
 */
@Data
@Builder
public class AccountBalanceAdjustmentVO {

    private Long id;
    private Long accountId;
    private BigDecimal beforeBalance;
    private BigDecimal afterBalance;
    private BigDecimal deltaAmount;
    private String reason;
    private LocalDate bizDate;
    private LocalDateTime bizTime;
    private LocalDateTime createdAt;
}
