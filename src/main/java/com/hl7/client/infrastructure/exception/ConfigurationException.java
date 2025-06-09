package com.hl7.client.infrastructure.exception;

/**
 * 配置异常
 * 当配置相关操作出现问题时抛出
 */
public class ConfigurationException extends ApplicationException {

    private static final String ERROR_CODE_PREFIX = "CONFIG_";

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public ConfigurationException(String message) {
        super(ERROR_CODE_PREFIX + "001", message);
    }

    /**
     * 构造函数
     *
     * @param message 错误消息
     * @param cause 原始异常
     */
    public ConfigurationException(String message, Throwable cause) {
        super(ERROR_CODE_PREFIX + "001", message, cause);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误代码后缀
     * @param message 错误消息
     */
    public ConfigurationException(String errorCode, String message) {
        super(ERROR_CODE_PREFIX + errorCode, message);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误代码后缀
     * @param message 错误消息
     * @param cause 原始异常
     */
    public ConfigurationException(String errorCode, String message, Throwable cause) {
        super(ERROR_CODE_PREFIX + errorCode, message, cause);
    }
}
