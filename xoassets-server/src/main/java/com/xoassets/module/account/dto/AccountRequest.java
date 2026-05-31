package com.xoassets.module.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 账户新增和修改请求参数。
 */
@Data
public class AccountRequest {

    @NotBlank(message = "账户名称不能为空")
    private String name;

    @NotBlank(message = "账户类型不能为空")
    private String type;

    @NotNull(message = "初始余额不能为空")
    private BigDecimal initialBalance;

    private BigDecimal balance;
    private String currency = "CNY";
    private Integer status = 1;
    private Integer sortOrder = 0;
    private String remark;
}
