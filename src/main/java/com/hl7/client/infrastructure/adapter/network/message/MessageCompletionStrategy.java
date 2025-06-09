package com.hl7.client.infrastructure.adapter.network.message;

import com.hl7.client.domain.model.Message;

/**
 * 消息完整性检查策略接口
 * 用于支持不同设备型号的消息完整性检查逻辑
 */
public interface MessageCompletionStrategy {

    /**
     * 检查消息是否完整
     *
     * @param message 要检查的消息
     * @return 如果需要回应消息，返回回应内容；如果消息完整无需回应，返回null
     */
    String isMessageComplete(Message message);

    /**
     * 检查策略是否支持指定的设备型号
     *
     * @param deviceModel 设备型号
     * @return 是否支持
     */
    boolean supports(String deviceModel);

    /**
     * 获取策略优先级
     * 当多个策略都支持同一设备型号时，优先级高的策略将被使用
     * 优先级值越小，优先级越高
     *
     * @return 优先级值，默认返回100
     */
    default int getPriority() {
        return 100; // 默认优先级
    }

    /**
     * 获取策略描述，用于日志和调试
     *
     * @return 策略描述
     */
    default String getDescription() {
        return this.getClass().getSimpleName();
    }
}
