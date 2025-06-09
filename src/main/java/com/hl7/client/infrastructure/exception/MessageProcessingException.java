package com.hl7.client.infrastructure.exception;

/**
 * 消息处理异常
 * 当消息处理过程中出现问题时抛出
 */
public class MessageProcessingException extends ApplicationException {

    private static final String ERROR_CODE_PREFIX = "MSG_PROC_";

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public MessageProcessingException(String message) {
        super(ERROR_CODE_PREFIX + "001", message);
    }

    /**
     * 构造函数
     *
     * @param message 错误消息
     * @param cause 原始异常
     */
    public MessageProcessingException(String message, Throwable cause) {
        super(ERROR_CODE_PREFIX + "001", message, cause);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误代码后缀
     * @param message 错误消息
     */
    public MessageProcessingException(String errorCode, String message) {
        super(ERROR_CODE_PREFIX + errorCode, message);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误代码后缀
     * @param message 错误消息
     * @param cause 原始异常
     */
    public MessageProcessingException(String errorCode, String message, Throwable cause) {
        super(ERROR_CODE_PREFIX + errorCode, message, cause);
    }
}
