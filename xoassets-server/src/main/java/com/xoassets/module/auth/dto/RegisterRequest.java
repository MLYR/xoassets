package com.xoassets.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求参数。
 */
@Data
public class RegisterRequest {

    /**
     * 用户名。
     */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名不能超过50个字符")
    private String username;

    /**
     * 密码。
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6到50位之间")
    private String password;

    /**
     * 昵称。
     */
    @Size(max = 50, message = "昵称不能超过50个字符")
    private String nickname;
}
