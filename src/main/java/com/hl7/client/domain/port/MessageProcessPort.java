package com.hl7.client.domain.port;

import com.hl7.client.domain.model.Message;

/**
 * 消息处理端口接口 - 领域核心端口
 * 定义消息处理的核心功能
 */
public interface MessageProcessPort {
    /**
     * 处理消息
     *
     * @param message 要处理的消息
     * @return 处理后的消息
     */
    Message processMessage(Message message);

    /**
     * 发送消息到服务端
     *
     * @param message 要发送的消息
     * @return 发送是否成功
     */
    boolean sendToServer(Message message);

    /**
     * 重试发送失败的消息
     *
     * @param message 要重试的消息
     * @return 重试是否成功
     */
    boolean retryFailedMessage(Message message);
}
