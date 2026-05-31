package com.xoassets.module.goal.vo;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * 资产目标汇总返回对象。
 */
@Data
@Builder
public class GoalSummaryVO {

    private BigDecimal totalTargetAmount;
    private BigDecimal totalCurrentAmount;
    private BigDecimal totalRemainingAmount;
    private BigDecimal overallCompletionRate;
    private Long activeGoalCount;
    private Long completedGoalCount;
}
