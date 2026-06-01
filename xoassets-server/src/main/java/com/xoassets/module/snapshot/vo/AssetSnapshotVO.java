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

    private Long id;
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
}
