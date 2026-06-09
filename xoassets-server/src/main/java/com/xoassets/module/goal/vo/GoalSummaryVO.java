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

    /**
     * 目标金额合计。
     */
    private BigDecimal totalTargetAmount;
    /**
     * 当前金额合计。
     */
    private BigDecimal totalCurrentAmount;
    /**
     * 剩余目标金额。
     */
    private BigDecimal totalRemainingAmount;
    /**
     * 整体完成率。
     */
    private BigDecimal overallCompletionRate;
    /**
     * 进行中目标数。
     */
    private Long activeGoalCount;
    /**
     * 已完成目标数。
     */
    private Long completedGoalCount;
}
