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

    /**
     * 月份。
     */
    private String month;
    /**
     * 预算总额。
     */
    private BigDecimal totalBudget;
    /**
     * 已使用总额。
     */
    private BigDecimal totalUsed;
    /**
     * 剩余额度合计。
     */
    private BigDecimal totalRemaining;
    /**
     * 使用率。
     */
    private BigDecimal usageRate;
    /**
     * 使用状态。
     */
    private String usageStatus;
    /**
     * 使用状态文案。
     */
    private String usageStatusLabel;
    /**
     * 明细列表。
     */
    private List<BudgetVO> items;
}
