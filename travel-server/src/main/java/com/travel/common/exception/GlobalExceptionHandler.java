package com.travel.common.exception;

import com.travel.common.result.ApiResponse;
import com.travel.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * <p>
 * 统一将业务异常、参数校验异常和系统异常转换为标准响应结构。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常。
     *
     * @param e 业务异常
     * @return 标准错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理 {@code @RequestBody} 触发的参数校验异常。
     *
     * @param e 参数校验异常
     * @return 标准错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // 同一请求可能触发多个字段错误，这里合并后一次性返回，减少前端反复提交成本。
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return ApiResponse.error(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 处理 {@code @ModelAttribute} 触发的参数绑定异常。
     *
     * @param e 参数绑定异常
     * @return 标准错误响应
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBindException(BindException e) {
        // 与 RequestBody 校验保持同一错误格式，前端无需区分参数来源。
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败: {}", message);
        return ApiResponse.error(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 处理 {@code @RequestParam} 或路径参数触发的约束校验异常。
     *
     * @param e 约束校验异常
     * @return 标准错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException e) {
        // 路径参数和查询参数的约束错误统一折叠成标准提示文案。
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("约束校验失败: {}", message);
        return ApiResponse.error(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 处理静态资源不存在异常，避免浏览器探测请求被记录为系统错误。
     *
     * @param e 静态资源不存在异常
     * @return 标准错误响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("静态资源不存在: {}", e.getResourcePath());
        return ApiResponse.error(HttpStatus.NOT_FOUND.value(), "请求的资源不存在");
    }

    /**
     * 处理未被显式捕获的系统异常。
     *
     * @param e 系统异常
     * @return 标准错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.error(ResultCode.SYSTEM_ERROR);
    }
}
