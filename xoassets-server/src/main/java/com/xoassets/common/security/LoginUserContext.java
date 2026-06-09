package com.xoassets.common.security;

import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 登录用户上下文工具，业务层通过它获取当前 user_id。
 */
public final class LoginUserContext {

    /**
     * 注入依赖组件。
     */
    private LoginUserContext() {
    }

    /**
     * 获取当前登录用户 ID；未登录时抛出统一未授权异常。
     */
    public static Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return loginUser.userId();
    }
}
