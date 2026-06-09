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

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 账户ID。
     */
    private Long accountId;
    /**
     * 修正前余额。
     */
    private BigDecimal beforeBalance;
    /**
     * 修正后余额。
     */
    private BigDecimal afterBalance;
    /**
     * 余额变动金额。
     */
    private BigDecimal deltaAmount;
    /**
     * 原因。
     */
    private String reason;
    /**
     * 业务日期。
     */
    private LocalDate bizDate;
    /**
     * 业务发生时间。
     */
    private LocalDateTime bizTime;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
