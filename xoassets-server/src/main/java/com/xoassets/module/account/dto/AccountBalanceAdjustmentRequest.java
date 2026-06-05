package com.xoassets.module.account.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * 账户余额修正请求，修正不计入普通收支但会进入账户账本。
 */
@Data
public class AccountBalanceAdjustmentRequest {

    @NotNull(message = "修正后余额不能为空")
    private BigDecimal afterBalance;

    private String reason;
    private LocalDate bizDate;
}
