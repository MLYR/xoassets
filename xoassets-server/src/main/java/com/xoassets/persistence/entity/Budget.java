package com.xoassets.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户预算实体，所有查询和修改必须按 user_id 隔离。
 */
@Data
@TableName("xo_budget")
public class Budget {

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 所属用户ID。
     */
    private Long userId;
    /**
     * 月份。
     */
    private String month;
    /**
     * 分类ID。
     */
    private Long categoryId;
    /**
     * 预算类型。
     */
    private String budgetType;
    /**
     * 金额。
     */
    private BigDecimal amount;
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
