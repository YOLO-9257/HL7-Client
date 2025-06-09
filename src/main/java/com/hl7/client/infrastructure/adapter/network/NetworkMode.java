package com.hl7.client.infrastructure.adapter.network;

/**
 * 网络连接模式枚举
 * 定义网络适配器的工作模式
 */
public enum NetworkMode {
    /**
     * 客户端模式 - 主动连接到远程服务器
     */
    CLIENT,

    /**
     * 服务器模式 - 监听端口，等待客户端连接
     */
    SERVER;

    /**
     * 从字符串解析网络模式
     *
     * @param value 模式字符串
     * @return 网络模式
     */
    public static NetworkMode fromString(String value) {
        if (value == null || value.isEmpty()) {
            return CLIENT; // 默认为客户端模式
        }

        try {
            return NetworkMode.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 无法解析时，返回默认模式
            return CLIENT;
        }
    }
}
