package com.xoassets.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 资产价格快照实体，手动价和外部行情都写入这里。
 */
@Data
@TableName("xo_asset_price")
public class AssetPrice {

    private Long id;
    private Long assetId;
    private BigDecimal price;
    private String currency;
    private String source;
    private LocalDateTime quoteTime;
    private String rawJson;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
