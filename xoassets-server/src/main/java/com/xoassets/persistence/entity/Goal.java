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
 * 资产目标实体，所有查询和修改必须按 user_id 隔离。
 */
@Data
@TableName("xo_goal")
public class Goal {

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 所属用户ID。
     */
    private Long userId;
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
