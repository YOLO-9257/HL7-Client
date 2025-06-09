package com.hl7.client.infrastructure.adapter.network;

import com.hl7.client.infrastructure.adapter.DeviceAdapter;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.BlockingQueue;

/**
 * Netty通道初始化器
 * 负责初始化通道处理器链
 */
@Slf4j
public class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final BlockingQueue<String> messageQueue;
    private final Set<Channel> connectedClients;
    private final DeviceAdapter deviceAdapter;
    private final MessageHandlerDelegate messageHandlerDelegate;

    /**
     * 构造函数
     *
     * @param messageQueue 消息队列
     * @param connectedClients 已连接客户端集合
     * @param deviceAdapter 设备适配器
     * @param messageHandlerDelegate 消息处理委托
     */
    public NettyChannelInitializer(
            BlockingQueue<String> messageQueue,
            Set<Channel> connectedClients,
            DeviceAdapter deviceAdapter,
            MessageHandlerDelegate messageHandlerDelegate) {
        this.messageQueue = messageQueue;
        this.connectedClients = connectedClients;
        this.deviceAdapter = deviceAdapter;
        this.messageHandlerDelegate = messageHandlerDelegate;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        // 添加基础的编解码器
        pipeline.addLast(new StringDecoder());
        pipeline.addLast(new StringEncoder());

        // 添加自定义消息处理器
        pipeline.addLast(new NettyMessageHandler(
                messageQueue,
                // 客户端连接时的回调
                channel -> {
                    connectedClients.add(channel);
                    log.info("客户端已连接，当前连接数: {}", connectedClients.size());
                },
                // 客户端断开时的回调
                channel -> {
                    connectedClients.remove(channel);
                    log.info("客户端已断开，当前连接数: {}", connectedClients.size());
                },
                deviceAdapter, // 设备适配器自身
                messageHandlerDelegate // 消息处理委托
        ));

        log.debug("通道初始化完成: {}", ch.remoteAddress());
    }
}
