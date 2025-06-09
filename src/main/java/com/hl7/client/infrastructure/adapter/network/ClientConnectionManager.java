package com.hl7.client.infrastructure.adapter.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 客户端连接管理器
 * 负责管理已连接的客户端通道
 */
@Slf4j
public class ClientConnectionManager {

    private final Set<Channel> connectedClients;
    public static final AttributeKey<Set<Channel>> CONNECTED_CLIENTS =
            AttributeKey.newInstance("CONNECTED_CLIENTS");

    /**
     * 构造函数
     */
    public ClientConnectionManager() {
        // 使用线程安全的集合存储客户端连接
        this.connectedClients = Collections.newSetFromMap(new ConcurrentHashMap<>());
    }

    /**
     * 为服务器通道设置连接属性
     *
     * @param serverChannel 服务器通道
     */
    public void initializeServerChannel(Channel serverChannel) {
        if (serverChannel == null) {
            log.warn("服务器通道为null，无法初始化");
            return;
        }

        try {
            // 尝试在父通道上设置属性
            Channel parent = serverChannel.parent();
            if (parent != null) {
                parent.attr(CONNECTED_CLIENTS).set(connectedClients);
                log.debug("成功设置连接客户端集合到父通道");
            } else {
                log.warn("服务器通道的父通道为null，将在服务器通道上设置属性");
                // 在服务器通道上设置属性，作为备选方案
                serverChannel.attr(CONNECTED_CLIENTS).set(connectedClients);
            }
        } catch (Exception e) {
            log.warn("设置连接客户端集合时出错: {}", e.getMessage());
            // 备用处理：在服务器通道上设置属性
            serverChannel.attr(CONNECTED_CLIENTS).set(connectedClients);
        }
    }

    /**
     * 添加客户端连接
     *
     * @param clientChannel 客户端通道
     */
    public void addClient(Channel clientChannel) {
        if (clientChannel != null && clientChannel.isActive()) {
            connectedClients.add(clientChannel);
            log.info("客户端已连接，当前连接数: {}", connectedClients.size());
        }
    }

    /**
     * 移除客户端连接
     *
     * @param clientChannel 客户端通道
     */
    public void removeClient(Channel clientChannel) {
        if (clientChannel != null) {
            connectedClients.remove(clientChannel);
            log.info("客户端已断开，当前连接数: {}", connectedClients.size());
        }
    }

    /**
     * 向所有连接的客户端广播消息
     *
     * @param message 要广播的消息
     * @return 是否至少有一个客户端成功接收
     */
    public boolean broadcastToAllClients(String message) {
        if (connectedClients.isEmpty()) {
            log.warn("无法发送数据：没有活跃的客户端连接");
            return false;
        }

        boolean sentToAny = false;

        // 向所有连接的客户端发送消息
        for (Channel channel : connectedClients) {
            if (channel != null && channel.isActive() && channel.isWritable()) {
                try {
                    ChannelFuture future = channel.writeAndFlush(message);
                    future.await(3, TimeUnit.SECONDS); // 等待3秒确保消息发送
                    if (future.isSuccess()) {
                        sentToAny = true;
                        log.debug("成功发送数据到客户端: {}", channel.remoteAddress());
                    } else {
                        log.warn("向客户端 {} 发送数据失败: {}",
                                channel.remoteAddress(),
                                future.cause() != null ? future.cause().getMessage() : "未知原因");
                    }
                } catch (Exception e) {
                    log.error("向客户端发送数据时发生异常: {}", e.getMessage());
                }
            }
        }

        return sentToAny;
    }

    /**
     * 获取已连接的客户端数量
     *
     * @return 客户端数量
     */
    public int getClientCount() {
        return connectedClients.size();
    }

    /**
     * 关闭所有客户端连接
     */
    public void closeAllConnections() {
        // 创建副本避免并发修改异常
        for (Channel channel : connectedClients.toArray(new Channel[0])) {
            try {
                if (channel.isOpen()) {
                    channel.close();
                }
            } catch (Exception e) {
                log.warn("关闭客户端连接时出错: {}", e.getMessage());
            }
        }
        connectedClients.clear();
        log.info("已关闭所有客户端连接");
    }

    /**
     * 获取连接的客户端集合
     *
     * @return 客户端集合
     */
    public Set<Channel> getConnectedClients() {
        return connectedClients;
    }
}
