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
 * 账户余额修正实体，记录非普通收支导致的余额校准事件。
 */
@Data
@TableName("xo_account_balance_adjustment")
public class AccountBalanceAdjustment {

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 所属用户ID。
     */
    private Long userId;
    /**
     * 账户ID。
     */
    private Long accountId;
    /**
     * 修正前余额。
     */
    private BigDecimal beforeBalance;
    /**
     * 修正后余额。
     */
    private BigDecimal afterBalance;
    /**
     * 余额变动金额。
     */
    private BigDecimal deltaAmount;
    /**
     * 原因。
     */
    private String reason;
    /**
     * 操作来源。
     */
    private String operatorType;
    /**
     * 业务日期。
     */
    private LocalDate bizDate;
    /**
     * 业务发生时间。
     */
    private LocalDateTime bizTime;
    /**
     * 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    /**
     * 逻辑删除标记。
     */
    @TableLogic
    private Integer deleted;
}
