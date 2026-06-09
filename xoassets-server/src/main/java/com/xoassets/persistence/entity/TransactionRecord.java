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

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 所属用户ID。
     */
    private Long userId;
    /**
     * 业务类型。
     */
    private String type;
    /**
     * 金额。
     */
    private BigDecimal amount;
    /**
     * 账户ID。
     */
    private Long accountId;
    /**
     * 转入账户ID。
     */
    private Long targetAccountId;
    /**
     * 分类ID。
     */
    private Long categoryId;
    /**
     * 原流水ID。
     */
    private Long originalTransactionId;
    /**
     * 交易发生时间。
     */
    private LocalDateTime transactionTime;
    /**
     * 备注。
     */
    private String note;
    /**
     * 图片地址。
     */
    private String imageUrl;
    /**
     * 状态。
     */
    private Integer status;
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
