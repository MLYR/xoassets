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

    private Long id;
    private Long userId;
    private Long accountId;
    private BigDecimal beforeBalance;
    private BigDecimal afterBalance;
    private BigDecimal deltaAmount;
    private String reason;
    private String operatorType;
    private LocalDate bizDate;
    private LocalDateTime bizTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
