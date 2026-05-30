package com.xoassets.common.api;

import lombok.Getter;

/**
 * 统一错误码，前端可直接根据 code 做提示和登录态处理。
 */
@Getter
public enum ErrorCode {
    SUCCESS(0, "success"),
    PARAM_ERROR(40000, "请求参数错误"),
    USERNAME_OR_PASSWORD_ERROR(40001, "用户名或密码错误"),
    USERNAME_EXISTS(40002, "用户名已存在"),
    UNAUTHORIZED(40100, "未登录或 Token 无效"),
    FORBIDDEN(40300, "无权访问该资源"),
    NOT_FOUND(40400, "数据不存在"),
    CONFLICT(40900, "数据冲突"),
    BUSINESS_ERROR(42200, "业务规则不允许"),
    SYSTEM_ERROR(50000, "系统异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
