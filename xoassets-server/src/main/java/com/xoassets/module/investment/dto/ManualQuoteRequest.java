package com.xoassets.module.investment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 手动报价请求参数。
 */
@Data
public class ManualQuoteRequest {

    @NotNull(message = "资产不能为空")
    private Long assetId;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.000001", message = "价格必须大于0")
    private BigDecimal price;

    private String currency;
    private LocalDateTime quoteTime;
}
