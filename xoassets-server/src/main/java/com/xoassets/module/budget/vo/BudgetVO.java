package com.xoassets.module.budget.vo;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * 预算展示对象，包含预算金额和当前使用进度。
 */
@Data
@Builder
public class BudgetVO {

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 月份。
     */
    private String month;
    /**
     * 分类ID。
     */
    private Long categoryId;
    /**
     * 分类名称。
     */
    private String categoryName;
    /**
     * 预算类型。
     */
    private String budgetType;
    /**
     * 金额。
     */
    private BigDecimal amount;
    /**
     * 已使用金额。
     */
    private BigDecimal usedAmount;
    /**
     * 剩余额度。
     */
    private BigDecimal remainingAmount;
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
     * 状态。
     */
    private Integer status;
}
