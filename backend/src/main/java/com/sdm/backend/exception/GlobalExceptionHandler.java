package com.sdm.backend.exception;

import com.sdm.backend.dto.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.sql.SQLIntegrityConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), localizeMessage(e.getMessage(), "业务处理失败"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + "：" + error.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        log.warn("参数校验异常: {}", message);
        return Result.error(400, message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + "：" + error.getDefaultMessage())
                .findFirst()
                .orElse("参数绑定失败");
        log.warn("参数绑定异常: {}", message);
        return Result.error(400, message);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return Result.error(405, "不支持的请求方法：" + e.getMethod());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFoundException(NoHandlerFoundException e) {
        log.warn("资源未找到: {}", e.getRequestURL());
        return Result.error(404, "请求的资源不存在");
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("认证失败: {}", e.getMessage());
        return Result.error(401, "用户名或密码错误");
    }

    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleDisabledException(DisabledException e) {
        log.warn("用户被禁用: {}", e.getMessage());
        return Result.error(401, "用户已被禁用，请联系管理员");
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthenticationException(AuthenticationException e) {
        log.warn("认证异常: {}", e.getMessage());
        return Result.error(401, "认证失败，请重新登录");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("访问被拒绝: {}", e.getMessage());
        return Result.error(403, "抱歉，您没有访问权限");
    }

    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleSQLIntegrityConstraintViolationException(SQLIntegrityConstraintViolationException e) {
        String message = e.getMessage();
        log.warn("数据库约束异常: {}", message);

        if (message != null && message.contains("Duplicate entry")) {
            return Result.error(400, "数据已存在，请勿重复提交");
        }
        if (message != null && message.contains("cannot be null")) {
            return Result.error(400, "必填项不能为空");
        }
        if (message != null && message.contains("Foreign key constraint")) {
            return Result.error(400, "数据关联校验失败，请先处理关联数据");
        }

        return Result.error(400, "数据库操作失败");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.warn("数据完整性异常: {}", e.getMessage());
        if (e.getCause() instanceof SQLIntegrityConstraintViolationException sqlException) {
            return handleSQLIntegrityConstraintViolationException(sqlException);
        }
        return Result.error(400, "数据校验失败");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数异常: {}", e.getMessage());
        return Result.error(400, localizeMessage(e.getMessage(), "请求参数不正确，请检查后重试"));
    }

    @ExceptionHandler(CannotGetJdbcConnectionException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleCannotGetJdbcConnectionException(CannotGetJdbcConnectionException e) {
        log.error("数据库连接异常", e);
        return Result.error(500, localizeMessage(e.getMessage(), "无法连接数据库，请检查数据库配置"));
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常", e);
        return Result.error(500, localizeMessage(e.getMessage(), "服务器内部错误，请稍后重试"));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(500, "系统繁忙，请稍后重试");
    }

    private String localizeMessage(String message, String fallback) {
        if (message == null || message.isBlank()) {
            return fallback;
        }
        if (containsChinese(message)) {
            return message;
        }
        if (message.contains("Access denied for user")) {
            return "数据库账号或密码错误，请检查数据库配置";
        }
        if (message.contains("Failed to obtain JDBC Connection")) {
            return "无法获取数据库连接，请检查数据库服务和连接配置";
        }
        if (message.contains("Duplicate entry")) {
            return "数据已存在，请勿重复提交";
        }
        if (message.contains("cannot be null")) {
            return "必填项不能为空";
        }
        if (message.contains("Building is required")) {
            return "请选择楼栋";
        }
        if (message.contains("Total floors must be greater than 0")) {
            return "总楼层必须大于 0";
        }
        if (message.contains("Rooms per floor must be greater than 0")) {
            return "每层房间数量必须大于 0";
        }
        if (message.contains("Capacity must be greater than 0")) {
            return "房间容量必须大于 0";
        }
        if (message.contains("Room number already exists")) {
            return message.replace("Room number already exists", "房间号已存在");
        }
        if (message.contains("No active counselors available for assignment")) {
            return "没有可用的辅导员，请先检查辅导员账号状态";
        }
        if (message.contains("No counselor available")) {
            return "当前没有可用的辅导员";
        }
        return fallback;
    }

    private boolean containsChinese(String text) {
        return text.chars().anyMatch(ch -> ch >= 0x4E00 && ch <= 0x9FFF);
    }
}
