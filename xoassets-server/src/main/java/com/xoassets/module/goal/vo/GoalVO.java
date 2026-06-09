package com.xoassets.module.goal.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

/**
 * 资产目标展示对象，包含进度和达成测算。
 */
@Data
@Builder
public class GoalVO {

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 名称。
     */
    private String name;
    /**
     * 目标金额。
     */
    private BigDecimal targetAmount;
    /**
     * 当前金额。
     */
    private BigDecimal currentAmount;
    /**
     * 目标日期。
     */
    private LocalDate targetDate;
    /**
     * 状态。
     */
    private String status;
    /**
     * 状态文案。
     */
    private String statusLabel;
    /**
     * 完成率。
     */
    private BigDecimal completionRate;
    /**
     * 剩余额度。
     */
    private BigDecimal remainingAmount;
    /**
     * 剩余天数。
     */
    private Long daysLeft;
    /**
     * 每月需存金额。
     */
    private BigDecimal monthlyRequiredAmount;
}
