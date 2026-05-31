package com.xoassets.common.exception;

import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.api.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.TransactionException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：把后端异常稳定转换成统一响应结构。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务层主动抛出的可预期异常。
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException exception, HttpServletRequest request) {
        log.warn("业务异常 method={}, uri={}, code={}, message={}",
                request.getMethod(), request.getRequestURI(), exception.getErrorCode().getCode(), exception.getMessage());
        return Result.failure(exception.getErrorCode(), exception.getMessage());
    }

    /**
     * 处理参数绑定和 Bean Validation 校验失败。
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(Exception exception, HttpServletRequest request) {
        log.warn("参数校验异常 method={}, uri={}, message={}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        return Result.failure(ErrorCode.PARAM_ERROR, exception.getMessage());
    }

    /**
     * 处理未登录或 Token 无效的认证异常。
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthenticationException(AuthenticationException exception, HttpServletRequest request) {
        log.warn("认证异常 method={}, uri={}, message={}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        return Result.failure(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getMessage());
    }

    /**
     * 处理已登录但权限不足的访问异常。
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException exception, HttpServletRequest request) {
        log.warn("访问拒绝 method={}, uri={}, message={}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        return Result.failure(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.getMessage());
    }

    /**
     * 处理数据库访问异常，保留完整堆栈用于排查连接失败、SQL 错误和表结构问题。
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleDataAccessException(DataAccessException exception, HttpServletRequest request) {
        log.error("数据库访问异常 method={}, uri={}", request.getMethod(), request.getRequestURI(), exception);
        return Result.failure(ErrorCode.SYSTEM_ERROR, "数据库访问异常，请检查数据库连接和表结构");
    }

    /**
     * 处理事务异常，常见于数据库连接失败或事务提交回滚失败。
     */
    @ExceptionHandler(TransactionException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleTransactionException(TransactionException exception, HttpServletRequest request) {
        log.error("数据库事务异常 method={}, uri={}", request.getMethod(), request.getRequestURI(), exception);
        return Result.failure(ErrorCode.SYSTEM_ERROR, "数据库事务异常，请检查数据库连接和事务配置");
    }

    /**
     * 兜底处理未捕获异常，避免向前端暴露内部错误细节。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception exception, HttpServletRequest request) {
        log.error("系统异常 method={}, uri={}", request.getMethod(), request.getRequestURI(), exception);
        return Result.failure(ErrorCode.SYSTEM_ERROR, ErrorCode.SYSTEM_ERROR.getMessage());
    }
}
