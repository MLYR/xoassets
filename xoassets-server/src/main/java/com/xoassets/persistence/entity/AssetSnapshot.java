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
 * 用户资产快照实体：每天记录用户资产、负债、预算和月度收支口径。
 */
@Data
@TableName("xo_asset_snapshot")
public class AssetSnapshot {

    private Long id;
    private Long userId;
    private LocalDate snapshotDate;
    private BigDecimal cashAsset;
    private BigDecimal investmentAsset;
    private BigDecimal totalAsset;
    private BigDecimal liability;
    private BigDecimal netAsset;
    private BigDecimal investmentCost;
    private BigDecimal investmentProfit;
    private BigDecimal investmentProfitRate;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpense;
    private BigDecimal monthlyBalance;
    private BigDecimal budgetUsedAmount;
    private BigDecimal budgetTotalAmount;
    private BigDecimal budgetUsageRate;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
