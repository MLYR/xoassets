package com.xoassets.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 资产日级价格实体，长期保存交易日 open/high/low/close 结果。
 */
@Data
@TableName("xo_asset_price_daily")
public class AssetPriceDaily {

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 资产ID。
     */
    private Long assetId;
    /**
     * 交易日期。
     */
    private LocalDate tradeDate;
    /**
     * 开盘价。
     */
    private BigDecimal openPrice;
    /**
     * 收盘价。
     */
    private BigDecimal closePrice;
    /**
     * 最高价。
     */
    private BigDecimal highPrice;
    /**
     * 最低价。
     */
    private BigDecimal lowPrice;
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
     * 币种。
     */
    private String currency;
    /**
     * 来源。
     */
    private String source;
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
    /**
     * 逻辑删除标记。
     */
    @TableLogic
    private Integer deleted;
}
