package com.xoassets.module.auth.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 登录返回对象，包含 JWT 和用户基础信息。
 */
@Data
@Builder
public class LoginVO {

    private String token;
    private UserVO user;
}
