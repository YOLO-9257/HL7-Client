package com.hl7.client.infrastructure.adapter.network;

import com.hl7.client.domain.model.Device;
import com.hl7.client.infrastructure.adapter.common.AbstractCommunicationAdapter;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * Netty适配器抽象基类
 * 包含服务器模式和客户端模式共同的功能
 */
@Slf4j
public abstract class AbstractNettyAdapter extends AbstractCommunicationAdapter {

    protected int port;
    protected String protocol;

    /**
     * 初始化适配器
     *
     * @param device 设备信息
     */
    @Override
    public void initialize(Device device) {
        super.initialize(device);
        parseConnectionParams(device.getConnectionParams());
    }

    /**
     * 解析连接参数
     *
     * @param params 连接参数字符串
     */
    protected abstract void parseConnectionParams(String params);

    /**
     * 安全关闭EventLoopGroup
     *
     * @param group 要关闭的EventLoopGroup
     * @param groupName 组名称(用于日志)
     */
    protected void shutdownEventLoopGroup(EventLoopGroup group, String groupName) {
        if (group != null && !group.isShutdown()) {
            try {
                group.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
                log.debug("{} 线程组已关闭", groupName);
            } catch (Exception e) {
                log.warn("关闭 {} 线程组时出错: {}", groupName, e.getMessage());
            }
        }
    }

    /**
     * 安全关闭Channel
     *
     * @param channel 要关闭的Channel
     * @param channelName 通道名称(用于日志)
     */
    protected void closeChannel(Channel channel, String channelName) {
        if (channel != null && channel.isActive()) {
            try {
                channel.close().sync();
                log.debug("{} 已关闭", channelName);
            } catch (Exception e) {
                log.warn("关闭 {} 时出错: {}", channelName, e.getMessage());
            }
        }
    }

    /**
     * 检查Channel是否有效
     *
     * @param channel 要检查的Channel
     * @return 是否有效
     */
    protected boolean isChannelValid(Channel channel) {
        return channel != null && channel.isActive() && channel.isWritable();
    }

    /**
     * 检查EventLoopGroup是否有效
     *
     * @param group 要检查的EventLoopGroup
     * @return 是否有效
     */
    protected boolean isEventLoopGroupValid(EventLoopGroup group) {
        return group != null && !group.isShutdown() && !group.isTerminated();
    }
}
