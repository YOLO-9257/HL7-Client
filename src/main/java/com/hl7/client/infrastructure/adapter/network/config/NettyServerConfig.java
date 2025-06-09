package com.hl7.client.infrastructure.adapter.network.config;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Netty服务器配置类
 * 封装服务器配置参数
 */
@Data
@Builder
@Slf4j
public class NettyServerConfig {

    /** 服务器端口 */
    private final int port;

    /** 协议类型 */
    private final String protocol;

    /** 连接模式 */
    private final String mode;

    /** Boss线程组线程数 */
    @Builder.Default
    private final int bossThreads = 1;

    /** Worker线程组线程数 */
    @Builder.Default
    private final int workerThreads = 0; // 0表示使用默认值

    /** 连接队列大小 */
    @Builder.Default
    private final int backlog = 100;

    /** TCP无延迟 */
    @Builder.Default
    private final boolean tcpNoDelay = true;

    /** 保持连接活跃 */
    @Builder.Default
    private final boolean keepAlive = true;

    /** 接收超时时间（秒） */
    @Builder.Default
    private final int receiveTimeoutSeconds = 10;

    /** 消息发送超时时间（秒） */
    @Builder.Default
    private final int sendTimeoutSeconds = 3;

    /**
     * 从连接参数字符串解析配置
     *
     * @param params 连接参数字符串（格式：port[:protocol][:mode]）
     * @return Netty服务器配置
     * @throws IllegalArgumentException 参数格式错误
     */
    public static NettyServerConfig fromConnectionParams(String params) {
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("连接参数不能为空");
        }

        try {
            String[] paramParts = params.split(":");
            if (paramParts.length < 1) {
                throw new IllegalArgumentException("连接参数格式错误，正确格式为：port[:protocol][:mode]");
            }

            // 解析端口号
            int port = Integer.parseInt(paramParts[0]);
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("端口号必须在1-65535范围内");
            }

            // 构建配置
            NettyServerConfigBuilder builder = NettyServerConfig.builder().port(port);

            // 解析协议（如果提供）
            if (paramParts.length >= 2 && !paramParts[1].isEmpty()) {
                builder.protocol(paramParts[1]);
            } else {
                builder.protocol("TCP"); // 默认使用TCP
            }

            // 解析模式（如果提供）
            if (paramParts.length >= 3 && !paramParts[2].isEmpty()) {
                builder.mode(paramParts[2]);
            }

            NettyServerConfig config = builder.build();
            log.info("已解析Netty服务器配置: {}", config);
            return config;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("端口号格式错误: " + e.getMessage());
        } catch (Exception e) {
            throw new IllegalArgumentException("解析连接参数失败: " + e.getMessage());
        }
    }

    /**
     * 验证配置是否有效
     *
     * @return 是否有效
     */
    public boolean isValid() {
        return port > 0 && port <= 65535 && protocol != null && !protocol.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("NettyServerConfig(port=%d, protocol=%s, mode=%s, backlog=%d, bossThreads=%d, workerThreads=%d)",
                port, protocol, mode, backlog, bossThreads, workerThreads);
    }
}
