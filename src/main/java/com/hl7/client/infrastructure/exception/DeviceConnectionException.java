package com.hl7.client.infrastructure.exception;

/**
 * 设备连接异常
 * 当设备连接出现问题时抛出
 */
public class DeviceConnectionException extends ApplicationException {

    private static final String ERROR_CODE_PREFIX = "DEVICE_CONN_";

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public DeviceConnectionException(String message) {
        super(ERROR_CODE_PREFIX + "001", message);
    }

    /**
     * 构造函数
     *
     * @param message 错误消息
     * @param cause 原始异常
     */
    public DeviceConnectionException(String message, Throwable cause) {
        super(ERROR_CODE_PREFIX + "001", message, cause);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误代码后缀
     * @param message 错误消息
     */
    public DeviceConnectionException(String errorCode, String message) {
        super(ERROR_CODE_PREFIX + errorCode, message);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误代码后缀
     * @param message 错误消息
     * @param cause 原始异常
     */
    public DeviceConnectionException(String errorCode, String message, Throwable cause) {
        super(ERROR_CODE_PREFIX + errorCode, message, cause);
    }
}
