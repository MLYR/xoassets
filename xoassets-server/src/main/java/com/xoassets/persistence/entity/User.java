package com.xoassets.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户实体：只保存账号、密码哈希和基础展示信息。
 */
@Data
@TableName("xo_user")
public class User {

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 用户名。
     */
    private String username;
    /**
     * 密码哈希值。
     */
    private String passwordHash;
    /**
     * 昵称。
     */
    private String nickname;
    /**
     * 头像地址。
     */
    private String avatarUrl;
    /**
     * 状态。
     */
    private Integer status;
    /**
     * 最后登录时间。
     */
    private LocalDateTime lastLoginAt;
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
