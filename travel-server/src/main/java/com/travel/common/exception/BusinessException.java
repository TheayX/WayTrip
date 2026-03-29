package com.travel.common.exception;

import com.travel.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常。
 * <p>
 * 用于在业务流程中主动中断执行，并携带统一错误码与错误信息返回给调用方。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;
    private final String message;

    /**
     * 使用自定义错误码和错误信息创建业务异常。
     *
     * @param code 错误码
     * @param message 错误信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 使用预定义结果码创建业务异常。
     *
     * @param resultCode 预定义结果码
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    /**
     * 使用预定义结果码和自定义消息创建业务异常。
     *
     * @param resultCode 预定义结果码
     * @param message 自定义错误信息
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.message = message;
    }
}
