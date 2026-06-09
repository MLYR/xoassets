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

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 资产ID。
     */
    private Long assetId;
    /**
     * 价格。
     */
    private BigDecimal price;
    /**
     * 币种。
     */
    private String currency;
    /**
     * 上一交易日收盘价。
     */
    private BigDecimal previousClose;
    /**
     * 较上一交易日涨跌额。
     */
    private BigDecimal changeAmount;
    /**
     * 较上一交易日涨跌幅。
     */
    private BigDecimal changePercent;
    /**
     * 来源。
     */
    private String source;
    /**
     * 报价时间。
     */
    private LocalDateTime quoteTime;
    /**
     * 市场状态。
     */
    private String marketStatus;
}
