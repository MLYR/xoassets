package com.xoassets.module.goal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * 资产目标新增和修改请求参数。
 */
@Data
public class GoalRequest {

    @NotBlank(message = "目标名称不能为空")
    private String name;

    @NotNull(message = "目标金额不能为空")
    @DecimalMin(value = "0.0001", message = "目标金额必须大于0")
    private BigDecimal targetAmount;

    @DecimalMin(value = "0.0000", message = "当前金额不能小于0")
    private BigDecimal currentAmount = BigDecimal.ZERO;

    private LocalDate targetDate;
    private String status = "ACTIVE";
    private Boolean useCurrentNetAssets = false;
}
