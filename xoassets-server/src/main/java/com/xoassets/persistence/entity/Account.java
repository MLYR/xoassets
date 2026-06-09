package com.xoassets.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 账户实体：余额为当前余额，所有业务读写必须按 user_id 隔离。
 */
@Data
@TableName("xo_account")
public class Account {

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
     * 业务类型。
     */
    private String type;
    /**
     * 账户余额。
     */
    private BigDecimal balance;
    /**
     * 初始余额。
     */
    private BigDecimal initialBalance;
    /**
     * 币种。
     */
    private String currency;
    /**
     * 状态。
     */
    private Integer status;
    /**
     * 排序值。
     */
    private Integer sortOrder;
    /**
     * 备注。
     */
    private String remark;
    /**
     * 数据版本号。
     */
    private Long version;
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
