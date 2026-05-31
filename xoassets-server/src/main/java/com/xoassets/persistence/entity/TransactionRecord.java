package com.xoassets.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 流水实体：amount 始终保存正数，方向由 type 决定。
 */
@Data
@TableName("xo_transaction")
public class TransactionRecord {

    private Long id;
    private Long userId;
    private String type;
    private BigDecimal amount;
    private Long accountId;
    private Long targetAccountId;
    private Long categoryId;
    private Long originalTransactionId;
    private LocalDateTime transactionTime;
    private String note;
    private String imageUrl;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
