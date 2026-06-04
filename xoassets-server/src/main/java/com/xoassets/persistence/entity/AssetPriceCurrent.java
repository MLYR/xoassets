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

    @TableId
    private Long assetId;
    private BigDecimal price;
    private String currency;
    private BigDecimal previousClose;
    private BigDecimal changeAmount;
    private BigDecimal changePercent;
    private String source;
    private LocalDateTime quoteTime;
    private String marketStatus;
    private String rawJson;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
