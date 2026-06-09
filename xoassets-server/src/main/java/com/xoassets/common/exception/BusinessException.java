package com.xoassets.common.exception;

import com.xoassets.common.api.ErrorCode;
import lombok.Getter;

/**
 * 业务异常：用于明确的参数、权限和业务规则错误。
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码。
     */
    private final ErrorCode errorCode;

    /**
     * 使用错误码默认文案创建业务异常。
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 使用业务场景自定义文案创建业务异常。
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
