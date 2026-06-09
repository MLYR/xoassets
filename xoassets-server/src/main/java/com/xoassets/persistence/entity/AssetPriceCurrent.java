package com.xoassets.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 资产当前价格实体，每个资产只保留一条最新可估值价格。
 */
@Data
@TableName("xo_asset_price_current")
public class AssetPriceCurrent {

    /**
     * 资产ID。
     */
    @TableId
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
    /**
     * 原始响应JSON。
     */
    private String rawJson;
    /**
     * 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
