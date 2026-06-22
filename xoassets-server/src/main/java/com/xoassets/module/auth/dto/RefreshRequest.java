package com.xoassets.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新令牌请求参数。
 */
@Data
public class RefreshRequest {

    /**
     * 刷新令牌。
     */
    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
}
