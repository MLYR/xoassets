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

    private Long id;
    private String month;
    private Long categoryId;
    private String categoryName;
    private String budgetType;
    private BigDecimal amount;
    private BigDecimal usedAmount;
    private BigDecimal remainingAmount;
    private BigDecimal usageRate;
    private String usageStatus;
    private String usageStatusLabel;
    private Integer status;
}
