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
 * 持仓每日收益实体，按展示日保存收益日历的权威明细。
 */
@Data
@TableName("xo_investment_holding_daily_profit")
public class InvestmentHoldingDailyProfit {

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 所属用户ID。
     */
    private Long userId;
    /**
     * 持仓ID。
     */
    private Long holdingId;
    /**
     * 资产ID。
     */
    private Long assetId;
    /**
     * 资产类型。
     */
    private String assetType;
    /**
     * 资产模块：FUND / STOCK / CRYPTO / OTHER。
     */
    private String module;
    /**
     * 收益日历展示日期。
     */
    private LocalDate displayDate;
    /**
     * 实际价格或净值日期。
     */
    private LocalDate priceDate;
    /**
     * 上一个价格日期。
     */
    private LocalDate previousPriceDate;
    /**
     * 收益基准份额日期。
     */
    private LocalDate quantityDate;
    /**
     * 收益基准数量。
     */
    private BigDecimal quantity;
    /**
     * 展示日价格。
     */
    private BigDecimal price;
    /**
     * 上一价格。
     */
    private BigDecimal previousPrice;
    /**
     * 收益金额。
     */
    private BigDecimal profitAmount;
    /**
     * 收益率。
     */
    private BigDecimal profitRate;
    /**
     * 收益率基准金额。
     */
    private BigDecimal baseAmount;
    /**
     * 持仓市值。
     */
    private BigDecimal marketValue;
    /**
     * 币种。
     */
    private String currency;
    /**
     * 计算状态：NORMAL / MARKET_CLOSED / PRICE_MISSING。
     */
    private String status;
    /**
     * 状态文案。
     */
    private String statusLabel;
    /**
     * 计算版本。
     */
    private Integer calcVersion;
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
