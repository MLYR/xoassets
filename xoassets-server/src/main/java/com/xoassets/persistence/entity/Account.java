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

    private Long id;
    private Long userId;
    private String name;
    private String type;
    private BigDecimal balance;
    private BigDecimal initialBalance;
    private String currency;
    private Integer status;
    private Integer sortOrder;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
