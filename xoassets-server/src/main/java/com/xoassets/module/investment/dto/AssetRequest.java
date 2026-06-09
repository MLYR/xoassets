package com.xoassets.module.investment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 公共资产新增请求参数。
 */
@Data
public class AssetRequest {

    /**
     * 资产代码。
     */
    @NotBlank(message = "资产代码不能为空")
    private String symbol;

    /**
     * 名称。
     */
    @NotBlank(message = "资产名称不能为空")
    private String name;

    /**
     * 业务类型。
     */
    @NotBlank(message = "资产类型不能为空")
    private String type;

    /**
     * 交易市场。
     */
    private String market = "UNKNOWN";
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
}
