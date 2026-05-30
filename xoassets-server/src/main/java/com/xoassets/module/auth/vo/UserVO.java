package com.xoassets.module.auth.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 用户展示对象，不包含密码哈希等敏感字段。
 */
@Data
@Builder
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
}
