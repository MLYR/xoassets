package com.xoassets.module.auth.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 登录返回对象，包含访问令牌、刷新令牌和用户基础信息。
 */
@Data
@Builder
public class LoginVO {

    /**
     * 访问令牌。
     */
    private String accessToken;
    /**
     * 刷新令牌。
     */
    private String refreshToken;
    /**
     * 登录用户信息。
     */
    private UserVO user;
}
