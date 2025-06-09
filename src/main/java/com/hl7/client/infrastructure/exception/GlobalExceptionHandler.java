package com.hl7.client.infrastructure.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 全局异常处理器
 * 统一处理应用中抛出的异常
 */
@Slf4j
@Component
public class GlobalExceptionHandler {

    /**
     * 处理ApplicationException类型的异常
     *
     * @param ex 应用异常
     */
    public void handleApplicationException(ApplicationException ex) {
        log.error("应用异常: [{}] {}", ex.getErrorCode(), ex.getMessage(), ex);
    }

    /**
     * 处理DeviceConnectionException类型的异常
     *
     * @param ex 设备连接异常
     */
    public void handleDeviceConnectionException(DeviceConnectionException ex) {
        log.error("设备连接异常: [{}] {}", ex.getErrorCode(), ex.getMessage(), ex);
        // 这里可以添加设备连接异常的特殊处理逻辑
    }

    /**
     * 处理MessageProcessingException类型的异常
     *
     * @param ex 消息处理异常
     */
    public void handleMessageProcessingException(MessageProcessingException ex) {
        log.error("消息处理异常: [{}] {}", ex.getErrorCode(), ex.getMessage(), ex);
        // 这里可以添加消息处理异常的特殊处理逻辑
    }

    /**
     * 处理ConfigurationException类型的异常
     *
     * @param ex 配置异常
     */
    public void handleConfigurationException(ConfigurationException ex) {
        log.error("配置异常: [{}] {}", ex.getErrorCode(), ex.getMessage(), ex);
        // 这里可以添加配置异常的特殊处理逻辑
    }

    /**
     * 处理未知异常
     *
     * @param ex 未知异常
     */
    public void handleException(Exception ex) {
        log.error("未知异常: {}", ex.getMessage(), ex);
    }
}
