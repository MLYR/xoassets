package com.xoassets.module.investment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 持仓新增和修改请求参数。
 */
@Data
public class HoldingRequest {

    @NotNull(message = "资产不能为空")
    private Long assetId;

    @NotNull(message = "持仓数量不能为空")
    @DecimalMin(value = "0.0000000001", message = "持仓数量必须大于0")
    private BigDecimal quantity;

    @NotNull(message = "平均成本不能为空")
    @DecimalMin(value = "0.0000", message = "平均成本不能小于0")
    private BigDecimal avgCost;

    private String remark;
}
