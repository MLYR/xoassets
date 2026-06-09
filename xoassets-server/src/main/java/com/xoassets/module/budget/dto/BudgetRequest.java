package com.xoassets.module.budget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 预算新增和修改请求参数。
 */
@Data
public class BudgetRequest {

    /**
     * 月份。
     */
    @NotBlank(message = "预算月份不能为空")
    private String month;

    /**
     * 分类ID。
     */
    private Long categoryId;

    /**
     * 预算类型。
     */
    @NotBlank(message = "预算类型不能为空")
    private String budgetType;

    /**
     * 金额。
     */
    @NotNull(message = "预算金额不能为空")
    @DecimalMin(value = "0.0001", message = "预算金额必须大于0")
    private BigDecimal amount;

    /**
     * 状态。
     */
    private Integer status = 1;
}
