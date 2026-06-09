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

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 所属用户ID。
     */
    private Long userId;
    /**
     * 快照日期。
     */
    private LocalDate snapshotDate;
    /**
     * 账户资产。
     */
    private BigDecimal cashAsset;
    /**
     * 投资资产。
     */
    private BigDecimal investmentAsset;
    /**
     * 总资产。
     */
    private BigDecimal totalAsset;
    /**
     * 负债。
     */
    private BigDecimal liability;
    /**
     * 净资产。
     */
    private BigDecimal netAsset;
    /**
     * 投资成本。
     */
    private BigDecimal investmentCost;
    /**
     * 投资收益。
     */
    private BigDecimal investmentProfit;
    /**
     * 投资收益率。
     */
    private BigDecimal investmentProfitRate;
    /**
     * 当月收入。
     */
    private BigDecimal monthlyIncome;
    /**
     * 当月支出。
     */
    private BigDecimal monthlyExpense;
    /**
     * 当月结余。
     */
    private BigDecimal monthlyBalance;
    /**
     * 预算已用金额。
     */
    private BigDecimal budgetUsedAmount;
    /**
     * 预算总额。
     */
    private BigDecimal budgetTotalAmount;
    /**
     * 预算使用率。
     */
    private BigDecimal budgetUsageRate;
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
