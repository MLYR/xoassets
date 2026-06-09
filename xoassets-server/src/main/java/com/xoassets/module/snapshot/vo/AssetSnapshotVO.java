package com.xoassets.module.snapshot.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

/**
 * 资产快照返回对象，供首页、分析页和后续 AI 报告统一使用。
 */
@Data
@Builder
public class AssetSnapshotVO {

    /**
     * 主键ID。
     */
    private Long id;
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
}
