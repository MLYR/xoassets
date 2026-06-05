package com.xoassets.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 账户日终余额快照实体，用于账户详情资产曲线。
 */
@Data
@TableName("xo_account_daily_balance_snapshot")
public class AccountDailyBalanceSnapshot {

    private Long id;
    private Long userId;
    private Long accountId;
    private LocalDate snapshotDate;
    private BigDecimal endBalance;
    private BigDecimal inflowAmount;
    private BigDecimal outflowAmount;
    private BigDecimal adjustmentAmount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
