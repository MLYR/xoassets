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

    /**
     * 名称。
     */
    @NotBlank(message = "账户名称不能为空")
    private String name;

    /**
     * 业务类型。
     */
    @NotBlank(message = "账户类型不能为空")
    private String type;

    /**
     * 初始余额。
     */
    @NotNull(message = "初始余额不能为空")
    private BigDecimal initialBalance;

    /**
     * 账户余额。
     */
    private BigDecimal balance;
    /**
     * 币种。
     */
    private String currency = "CNY";
    /**
     * 状态。
     */
    private Integer status = 1;
    /**
     * 排序值。
     */
    private Integer sortOrder = 0;
    /**
     * 备注。
     */
    private String remark;
}
