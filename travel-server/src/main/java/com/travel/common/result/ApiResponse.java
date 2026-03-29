package com.travel.common.result;

import lombok.Data;

/**
 * 通用 API 响应对象。
 *
 * @param <T> 响应数据类型
 */
@Data
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;
    private long timestamp;

    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 创建完整响应对象。
     *
     * @param code 业务状态码
     * @param message 响应消息
     * @param data 响应数据
     */
    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 创建无数据的成功响应。
     *
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(0, "success", null);
    }

    /**
     * 创建携带数据的成功响应。
     *
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    /**
     * 创建自定义消息的成功响应。
     *
     * @param message 响应消息
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(0, message, data);
    }

    /**
     * 创建自定义错误码和消息的失败响应。
     *
     * @param code 错误码
     * @param message 错误消息
     * @param <T> 响应数据类型
     * @return 失败响应
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /**
     * 使用预定义结果码创建失败响应。
     *
     * @param resultCode 预定义结果码
     * @param <T> 响应数据类型
     * @return 失败响应
     */
    public static <T> ApiResponse<T> error(ResultCode resultCode) {
        return new ApiResponse<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /**
     * 使用预定义结果码和自定义消息创建失败响应。
     *
     * @param resultCode 预定义结果码
     * @param message 自定义错误消息
     * @param <T> 响应数据类型
     * @return 失败响应
     */
    public static <T> ApiResponse<T> error(ResultCode resultCode, String message) {
        return new ApiResponse<>(resultCode.getCode(), message, null);
    }
}
