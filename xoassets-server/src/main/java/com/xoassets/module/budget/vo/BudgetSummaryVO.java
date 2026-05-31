package com.xoassets.module.budget.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 月度预算汇总返回对象。
 */
@Data
@Builder
public class BudgetSummaryVO {

    private String month;
    private BigDecimal totalBudget;
    private BigDecimal totalUsed;
    private BigDecimal totalRemaining;
    private BigDecimal usageRate;
    private String usageStatus;
    private String usageStatusLabel;
    private List<BudgetVO> items;
}
