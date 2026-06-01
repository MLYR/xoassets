package com.xoassets.module.investment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 资产自动识别结果，供前端新增持仓时选择并自动填充资产信息。
 */
@Data
@Builder
public class AssetLookupVO {

    private String name;
    private String symbol;
    private String assetType;
    private String market;
    private String currency;
    private String quoteSource;
    private String quoteKey;
    private BigDecimal latestPrice;
    private BigDecimal previousClose;
    private BigDecimal changePercent;
    private LocalDateTime quoteTime;
}
