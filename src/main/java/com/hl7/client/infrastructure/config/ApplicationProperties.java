package com.hl7.client.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用程序配置属性
 * 管理应用程序的各种配置项
 */
@Data
@ConfigurationProperties(prefix = "application")
public class ApplicationProperties {

    /** 服务器相关配置 */
    private Server server = new Server();

    /** 消息处理相关配置 */
    private Message message = new Message();

    /** 设备相关配置 */
    private Device device = new Device();

    @Data
    public static class Server {
        /** 服务器地址 */
        private String address = "http://localhost:8080/api";

        /** 是否启用SSL */
        private boolean ssl = false;

        /** 连接超时时间(毫秒) */
        private int connectTimeout = 5000;

        /** 读取超时时间(毫秒) */
        private int readTimeout = 10000;
    }

    @Data
    public static class Message {
        /** 消息处理间隔(毫秒) */
        private int processInterval = 5000;

        /** 消息重试间隔(毫秒) */
        private int retryInterval = 60000;

        /** 消息保存天数 */
        private int saveDays = 30;
    }

    @Data
    public static class Device {
        /** 设备连接检查间隔(毫秒) */
        private int checkInterval = 30000;

        /** 设备自动重连间隔(毫秒) */
        private int reconnectInterval = 60000;

        /** 是否启用自动重连 */
        private boolean autoReconnect = true;
    }
}
