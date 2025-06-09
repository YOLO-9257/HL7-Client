package com.hl7.client.infrastructure.exception;

import lombok.Getter;

/**
 * 应用异常基类
 * 所有自定义业务异常都应继承此类
 */
@Getter
public class ApplicationException extends RuntimeException {

    /**
     * 错误代码
     */
    private final String errorCode;

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public ApplicationException(String message) {
        this("UNKNOWN", message);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误代码
     * @param message 错误消息
     */
    public ApplicationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param cause 原始异常
     */
    public ApplicationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
