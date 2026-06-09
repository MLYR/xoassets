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

    /**
     * 资产ID。
     */
    @NotNull(message = "资产不能为空")
    private Long assetId;

    /**
     * 价格。
     */
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.000001", message = "价格必须大于0")
    private BigDecimal price;

    /**
     * 币种。
     */
    private String currency;
    /**
     * 报价时间。
     */
    private LocalDateTime quoteTime;
}
