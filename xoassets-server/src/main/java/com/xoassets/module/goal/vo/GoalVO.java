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

    private Long id;
    private String name;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private LocalDate targetDate;
    private String status;
    private String statusLabel;
    private BigDecimal completionRate;
    private BigDecimal remainingAmount;
    private Long daysLeft;
    private BigDecimal monthlyRequiredAmount;
}
