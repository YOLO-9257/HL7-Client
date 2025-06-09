package com.hl7.client.infrastructure.adapter.network.exception;

/**
 * 消息处理异常
 * 用于标准化消息处理过程中的异常
 */
public class MessageHandlingException extends RuntimeException {

    private final String errorCode;

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public MessageHandlingException(String message) {
        this("MH-000", message);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误代码
     * @param message 错误消息
     */
    public MessageHandlingException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 构造函数
     *
     * @param message 错误消息
     * @param cause 原始异常
     */
    public MessageHandlingException(String message, Throwable cause) {
        this("MH-000", message, cause);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param cause 原始异常
     */
    public MessageHandlingException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误代码
     *
     * @return 错误代码
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 创建解析异常
     *
     * @param message 错误消息
     * @return 异常实例
     */
    public static MessageHandlingException parseError(String message) {
        return new MessageHandlingException("MH-001", "解析错误: " + message);
    }

    /**
     * 创建解析异常
     *
     * @param message 错误消息
     * @param cause 原始异常
     * @return 异常实例
     */
    public static MessageHandlingException parseError(String message, Throwable cause) {
        return new MessageHandlingException("MH-001", "解析错误: " + message, cause);
    }

    /**
     * 创建完整性检查异常
     *
     * @param message 错误消息
     * @return 异常实例
     */
    public static MessageHandlingException completenessError(String message) {
        return new MessageHandlingException("MH-002", "完整性检查错误: " + message);
    }

    /**
     * 创建完整性检查异常
     *
     * @param message 错误消息
     * @param cause 原始异常
     * @return 异常实例
     */
    public static MessageHandlingException completenessError(String message, Throwable cause) {
        return new MessageHandlingException("MH-002", "完整性检查错误: " + message, cause);
    }

    /**
     * 创建处理异常
     *
     * @param message 错误消息
     * @return 异常实例
     */
    public static MessageHandlingException processingError(String message) {
        return new MessageHandlingException("MH-003", "处理错误: " + message);
    }

    /**
     * 创建处理异常
     *
     * @param message 错误消息
     * @param cause 原始异常
     * @return 异常实例
     */
    public static MessageHandlingException processingError(String message, Throwable cause) {
        return new MessageHandlingException("MH-003", "处理错误: " + message, cause);
    }
}
