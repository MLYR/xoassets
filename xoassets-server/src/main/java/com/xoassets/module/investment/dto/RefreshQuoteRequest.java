package com.xoassets.module.investment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 行情刷新请求参数。
 */
@Data
public class RefreshQuoteRequest {

    /**
     * 资产ID。
     */
    @NotNull(message = "资产不能为空")
    private Long assetId;
}
