package com.xoassets.common.api;

import lombok.Data;

/**
 * 统一 API 响应结构，所有 Controller 都返回该结构。
 */
@Data
public class Result<T> {

    /**
     * 业务码。
     */
    private int code;
    /**
     * 提示信息。
     */
    private String message;
    /**
     * 业务数据。
     */
    private T data;
    /**
     * 链路追踪ID。
     */
    private String traceId;

    /**
     * 构造成功响应，业务数据放在 data 中返回。
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ErrorCode.SUCCESS.getCode());
        result.setMessage(ErrorCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    /**
     * 构造失败响应，允许业务层覆盖默认错误提示。
     */
    public static <T> Result<T> failure(ErrorCode errorCode, String message) {
        Result<T> result = new Result<>();
        result.setCode(errorCode.getCode());
        result.setMessage(message == null ? errorCode.getMessage() : message);
        return result;
    }
}
