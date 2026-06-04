package com.xoassets.module.investment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 持仓新增和修改请求参数。
 */
@Data
public class HoldingRequest {

    private Long assetId;

    private String assetName;
    private String symbol;
    private String assetType;
    private String market;
    private String currency = "CNY";
    private String quoteSource = "MANUAL";
    private String quoteKey;
    private BigDecimal latestPrice;
    private BigDecimal previousClose;
    private BigDecimal changePercent;
    private LocalDateTime quoteTime;
    private String marketStatus;

    @NotNull(message = "持仓数量不能为空")
    @DecimalMin(value = "0.0000", message = "持仓数量不能小于0")
    private BigDecimal quantity;

    @NotNull(message = "平均成本不能为空")
    @DecimalMin(value = "0.0000", message = "平均成本不能小于0")
    private BigDecimal avgCost;

    private String remark;
}
