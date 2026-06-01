package com.xoassets.module.investment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 资产价格返回对象。
 */
@Data
@Builder
public class AssetPriceVO {

    private Long id;
    private Long assetId;
    private BigDecimal price;
    private String currency;
    private BigDecimal previousClose;
    private BigDecimal changeAmount;
    private BigDecimal changePercent;
    private String source;
    private LocalDateTime quoteTime;
    private String marketStatus;
}
