package com.xoassets.module.investment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 公共资产新增请求参数。
 */
@Data
public class AssetRequest {

    @NotBlank(message = "资产代码不能为空")
    private String symbol;

    @NotBlank(message = "资产名称不能为空")
    private String name;

    @NotBlank(message = "资产类型不能为空")
    private String type;

    private String currency = "CNY";
    private String quoteSource = "MANUAL";
    private String quoteKey;
}
