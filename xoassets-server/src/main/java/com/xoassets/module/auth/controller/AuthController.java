package com.xoassets.module.auth.controller;

import com.xoassets.common.api.Result;
import com.xoassets.module.auth.dto.ChangePasswordRequest;
import com.xoassets.module.auth.dto.LoginRequest;
import com.xoassets.module.auth.dto.RegisterRequest;
import com.xoassets.module.auth.dto.UpdateProfileRequest;
import com.xoassets.module.auth.service.AuthService;
import com.xoassets.module.auth.vo.LoginVO;
import com.xoassets.module.auth.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：注册、登录和当前用户信息。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 注册账号。
     */
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    /**
     * 登录并返回 JWT。
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /**
     * 查询当前登录用户。
     */
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(authService.me());
    }

    /**
     * 修改当前登录用户昵称。
     */
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.success(authService.updateProfile(request));
    }

    /**
     * 修改当前登录用户密码。
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return Result.success(null);
    }
}
