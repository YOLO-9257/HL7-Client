package com.hl7.client.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通信配置类
 * 用于统一管理串口和网络通信的配置参数
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "communication")
public class CommunicationConfig {

    /**
     * 是否启用自动发现设备
     */
    private boolean autoDiscovery = false;

    /**
     * 是否自动启动监听
     */
    private boolean autoStart = true;

    /**
     * 串口配置列表
     */
    private List<SerialConfig> serialPorts = new ArrayList<>();

    /**
     * 网络配置列表
     */
    private List<NetworkConfig> networks = new ArrayList<>();

    /**
     * 设备特定配置
     */
    private Map<String, DeviceConfig> devices = new ConcurrentHashMap<>();

    /**
     * 串口配置
     */
    @Data
    public static class SerialConfig {
        /**
         * 串口名称
         */
        private String portName;

        /**
         * 波特率
         */
        private int baudRate = 9600;

        /**
         * 数据位
         */
        private int dataBits = 8;

        /**
         * 停止位
         */
        private int stopBits = 1;

        /**
         * 校验位
         */
        private int parity = 0;

        /**
         * 关联的设备型号
         */
        private String deviceModel;

        /**
         * 是否启用
         */
        private boolean enabled = true;
    }

    /**
     * 网络配置
     */
    @Data
    public static class NetworkConfig {
        /**
         * 主机名或IP地址
         */
        private String host = "0.0.0.0";

        /**
         * 端口号
         */
        private int port = 8888;

        /**
         * 网络模式 (SERVER/CLIENT)
         */
        private String mode = "SERVER";

        /**
         * 关联的设备型号
         */
        private String deviceModel;

        /**
         * 是否启用
         */
        private boolean enabled = true;
    }

    /**
     * 设备配置
     */
    @Data
    public static class DeviceConfig {
        /**
         * 设备型号
         */
        private String model;

        /**
         * 设备名称
         */
        private String name;

        /**
         * 设备描述
         */
        private String description;

        /**
         * 消息类型
         */
        private String messageType;

        /**
         * 自定义参数
         */
        private Map<String, String> parameters = new ConcurrentHashMap<>();
    }
}
