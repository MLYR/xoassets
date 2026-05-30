package com.xoassets.module.auth.service;

import com.xoassets.module.auth.dto.LoginRequest;
import com.xoassets.module.auth.dto.RegisterRequest;
import com.xoassets.module.auth.vo.LoginVO;
import com.xoassets.module.auth.vo.UserVO;

/**
 * 认证服务接口：Controller 只依赖接口，具体实现放在 impl 包。
 */
public interface AuthService {

    /**
     * 注册新用户。
     */
    UserVO register(RegisterRequest request);

    /**
     * 登录并返回 Token 和用户信息。
     */
    LoginVO login(LoginRequest request);

    /**
     * 查询当前用户信息。
     */
    UserVO me();
}
