package com.xoassets.module.auth.service;

import com.xoassets.module.auth.dto.LoginRequest;
import com.xoassets.module.auth.dto.RegisterRequest;
import com.xoassets.module.auth.dto.RefreshRequest;
import com.xoassets.module.auth.dto.ChangePasswordRequest;
import com.xoassets.module.auth.dto.UpdateProfileRequest;
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
     * 使用刷新令牌换取新的访问令牌和刷新令牌。
     */
    LoginVO refresh(RefreshRequest request);

    /**
     * 查询当前用户信息。
     */
    UserVO me();

    /**
     * 修改当前用户资料。
     */
    UserVO updateProfile(UpdateProfileRequest request);

    /**
     * 修改当前用户密码。
     */
    void changePassword(ChangePasswordRequest request);
}
