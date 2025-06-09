package com.hl7.client.infrastructure.ws;

import java.util.Map;

/**
 * WebSocket消息处理器接口
 * 处理从WebSocket接收到的消息
 */
@FunctionalInterface
public interface WebSocketMessageHandler {

    /**
     * 处理消息
     *
     * @param message 消息内容
     */
    void handleMessage(Map<String, Object> message);
}
