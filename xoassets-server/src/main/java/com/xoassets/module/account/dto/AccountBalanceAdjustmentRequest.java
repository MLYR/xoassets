package com.xoassets.module.account.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 账户余额修正请求，修正不计入普通收支但会进入账户账本。
 */
@Data
public class AccountBalanceAdjustmentRequest {

    /**
     * 修正后余额。
     */
    @NotNull(message = "修正后余额不能为空")
    private BigDecimal afterBalance;

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
}
