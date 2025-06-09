package com.hl7.client.infrastructure.adapter.network;

import com.hl7.client.domain.model.Device;
import com.hl7.client.domain.model.Message;

/**
 * 消息处理委托接口
 * 定义消息处理和转发的方法
 */
public interface MessageHandlerDelegate {

    /**
     * 处理接收到的原始消息
     *
     * @param device 接收消息的设备
     * @param rawMessage 原始消息内容
     * @return 处理是否成功
     */
    boolean processMessage(Device device, String rawMessage);

    /**
     * 检查消息是否完整
     *
     * @param device
     * @param message 消息内容
     * @return 消息是否完整
     */
    String isMessageComplete(Message message);
}
