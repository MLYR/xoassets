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

    /**
     * 资产ID。
     */
    private Long assetId;

    /**
     * 资产名称。
     */
    private String assetName;
    /**
     * 资产代码。
     */
    private String symbol;
    /**
     * 资产类型。
     */
    private String assetType;
    /**
     * 交易市场。
     */
    private String market;
    /**
     * 币种。
     */
    private String currency = "CNY";
    /**
     * 行情来源。
     */
    private String quoteSource = "MANUAL";
    /**
     * 行情查询键。
     */
    private String quoteKey;
    /**
     * 最新价格。
     */
    private BigDecimal latestPrice;
    /**
     * 上一交易日收盘价。
     */
    private BigDecimal previousClose;
    /**
     * 较上一交易日涨跌幅。
     */
    private BigDecimal changePercent;
    /**
     * 报价时间。
     */
    private LocalDateTime quoteTime;
    /**
     * 市场状态。
     */
    private String marketStatus;

    /**
     * 持仓数量。
     */
    @NotNull(message = "持仓数量不能为空")
    @DecimalMin(value = "0.0000", message = "持仓数量不能小于0")
    private BigDecimal quantity;

    /**
     * 平均成本。
     */
    @NotNull(message = "平均成本不能为空")
    @DecimalMin(value = "0.0000", message = "平均成本不能小于0")
    private BigDecimal avgCost;

    /**
     * 备注。
     */
    private String remark;
}
