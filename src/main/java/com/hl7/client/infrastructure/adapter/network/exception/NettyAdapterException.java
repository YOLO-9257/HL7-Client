package com.hl7.client.infrastructure.adapter.network.exception;

/**
 * Netty适配器异常
 * 用于标准化网络适配器的异常处理
 */
public class NettyAdapterException extends RuntimeException {

    private final String errorCode;

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public NettyAdapterException(String message) {
        this("NA-000", message);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误代码
     * @param message 错误消息
     */
    public NettyAdapterException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 构造函数
     *
     * @param message 错误消息
     * @param cause 原始异常
     */
    public NettyAdapterException(String message, Throwable cause) {
        this("NA-000", message, cause);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param cause 原始异常
     */
    public NettyAdapterException(String errorCode, String message, Throwable cause) {
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
     * 创建连接异常
     *
     * @param message 错误消息
     * @return 异常实例
     */
    public static NettyAdapterException connectionError(String message) {
        return new NettyAdapterException("NA-001", "连接错误: " + message);
    }

    /**
     * 创建连接异常
     *
     * @param message 错误消息
     * @param cause 原始异常
     * @return 异常实例
     */
    public static NettyAdapterException connectionError(String message, Throwable cause) {
        return new NettyAdapterException("NA-001", "连接错误: " + message, cause);
    }

    /**
     * 创建发送异常
     *
     * @param message 错误消息
     * @return 异常实例
     */
    public static NettyAdapterException sendError(String message) {
        return new NettyAdapterException("NA-002", "发送错误: " + message);
    }

    /**
     * 创建发送异常
     *
     * @param message 错误消息
     * @param cause 原始异常
     * @return 异常实例
     */
    public static NettyAdapterException sendError(String message, Throwable cause) {
        return new NettyAdapterException("NA-002", "发送错误: " + message, cause);
    }

    /**
     * 创建接收异常
     *
     * @param message 错误消息
     * @return 异常实例
     */
    public static NettyAdapterException receiveError(String message) {
        return new NettyAdapterException("NA-003", "接收错误: " + message);
    }

    /**
     * 创建接收异常
     *
     * @param message 错误消息
     * @param cause 原始异常
     * @return 异常实例
     */
    public static NettyAdapterException receiveError(String message, Throwable cause) {
        return new NettyAdapterException("NA-003", "接收错误: " + message, cause);
    }

    /**
     * 创建配置异常
     *
     * @param message 错误消息
     * @return 异常实例
     */
    public static NettyAdapterException configError(String message) {
        return new NettyAdapterException("NA-004", "配置错误: " + message);
    }

    /**
     * 创建配置异常
     *
     * @param message 错误消息
     * @param cause 原始异常
     * @return 异常实例
     */
    public static NettyAdapterException configError(String message, Throwable cause) {
        return new NettyAdapterException("NA-004", "配置错误: " + message, cause);
    }
}
