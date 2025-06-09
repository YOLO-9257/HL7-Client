package com.hl7.client.infrastructure.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hl7.client.infrastructure.config.ApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * WebSocket客户端
 * 用于与服务器通信
 */
@Slf4j
@Component
public class WebSocketClient {

    private final ApplicationProperties properties;
    private final ObjectMapper objectMapper;
    private org.java_websocket.client.WebSocketClient wsClient;
    private final Map<String, WebSocketMessageHandler> messageHandlers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private boolean connected = false;

    public WebSocketClient(ApplicationProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
//        connect();

        // 定时检查连接状态
//        scheduler.scheduleAtFixedRate(this::checkConnection,
//                properties.getDevice().getCheckInterval(),
//                properties.getDevice().getCheckInterval(),
//                TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void destroy() {
        disconnect();
        scheduler.shutdown();
    }

    /**
     * 建立连接
     */
    public void connect() {
        try {
            String serverUrl = properties.getServer().getAddress();
//            log.info("尝试连接到WebSocket服务器: {}", serverUrl);

            URI serverUri = URI.create(serverUrl);
            wsClient = new InternalWebSocketClient(serverUri);
            wsClient.connect();
        } catch (Exception e) {
            log.error("连接WebSocket服务器失败: {}", e.getMessage());
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.close();
        }
    }

    /**
     * 发送消息
     *
     * @param message 消息内容
     * @return 是否发送成功
     */
    public boolean sendMessage(Object message) {
        if (wsClient == null || !wsClient.isOpen()) {
            log.error("WebSocket未连接，无法发送消息");
            return false;
        }

        try {
            String json = objectMapper.writeValueAsString(message);
            wsClient.send(json);
            log.debug("WebSocket发送消息: {}", json);
            return true;
        } catch (Exception e) {
            log.error("WebSocket发送消息失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 注册消息处理器
     *
     * @param messageType 消息类型
     * @param handler 处理器
     */
    public void registerMessageHandler(String messageType, WebSocketMessageHandler handler) {
        messageHandlers.put(messageType, handler);
        log.info("注册了WebSocket消息处理器: {}", messageType);
    }

    /**
     * 检查连接状态
     */
    private void checkConnection() {
        if (wsClient == null || !wsClient.isOpen()) {
//            log.warn("WebSocket连接已断开，尝试重新连接");

//            if (properties.getDevice().isAutoReconnect()) {
//                connect();
//            }
        }
    }

    /**
     * 内部WebSocket客户端
     */
    private class InternalWebSocketClient extends org.java_websocket.client.WebSocketClient {

        public InternalWebSocketClient(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            log.info("WebSocket连接已打开: {}", getURI());
            connected = true;
        }

        @Override
        public void onMessage(String message) {
            log.debug("收到WebSocket消息: {}", message);

            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(message, Map.class);
                String type = (String) data.get("type");

                if (type != null && messageHandlers.containsKey(type)) {
                    messageHandlers.get(type).handleMessage(data);
                } else {
                    log.warn("未找到处理WebSocket消息的处理器，消息类型: {}", type);
                }
            } catch (Exception e) {
                log.error("处理WebSocket消息时出错: {}", e.getMessage());
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            log.info("WebSocket连接已关闭: code={}, reason={}, remote={}", code, reason, remote);
            connected = false;
        }

        @Override
        public void onError(Exception ex) {
            log.error("WebSocket连接发生错误: {}", ex.getMessage());
        }
    }

    /**
     * 获取是否已连接
     *
     * @return 是否已连接
     */
    public boolean isConnected() {
        return connected && wsClient != null && wsClient.isOpen();
    }
}
