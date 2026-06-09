package com.xoassets.module.auth.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 用户展示对象，不包含密码哈希等敏感字段。
 */
@Data
@Builder
public class UserVO {

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 用户名。
     */
    private String username;
    /**
     * 昵称。
     */
    private String nickname;
    /**
     * 头像地址。
     */
    private String avatarUrl;
}
