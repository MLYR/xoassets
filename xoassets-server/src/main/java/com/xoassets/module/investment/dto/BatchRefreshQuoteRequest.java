package com.xoassets.module.investment.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

/**
 * 批量刷新行情请求参数，前端只传 XOAssets 内部资产 ID。
 */
@Data
public class BatchRefreshQuoteRequest {

    /**
     * 业务ID列表。
     */
    @NotEmpty(message = "资产列表不能为空")
    private List<Long> assetIds;
}
