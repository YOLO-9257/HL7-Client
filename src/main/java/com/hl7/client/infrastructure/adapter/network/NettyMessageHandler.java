package com.hl7.client.infrastructure.adapter.network;

import com.hl7.client.domain.model.Device;
import com.hl7.client.infrastructure.adapter.DeviceAdapter;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

/**
 * Netty消息处理器
 * 用于管理Netty通道的消息处理
 */
@Slf4j
public class NettyMessageHandler extends SimpleChannelInboundHandler<String> {

    /**
     * 连接客户端的通道映射
     */
    public static final AttributeKey<Set<Channel>> CONNECTED_CLIENTS =
            AttributeKey.valueOf("CONNECTED_CLIENTS");

    private final BlockingQueue<String> messageQueue;
    private final Consumer<Channel> channelActiveHandler;
    private final Consumer<Channel> channelInactiveHandler;

    private final DeviceAdapter deviceAdapter;

    /**
     * 消息处理委托
     */
    private final MessageHandlerDelegate messageHandlerDelegate;

    /**
     * 构造函数
     *
     * @param messageQueue 消息队列，用于存储接收到的消息
     * @param channelActiveHandler 通道激活处理器
     * @param channelInactiveHandler 通道断开处理器
     * @param deviceAdapter 设备适配器
     * @param messageHandlerDelegate 消息处理委托
     */
    public NettyMessageHandler(
            BlockingQueue<String> messageQueue,
            Consumer<Channel> channelActiveHandler,
            Consumer<Channel> channelInactiveHandler,
            DeviceAdapter deviceAdapter,
            MessageHandlerDelegate messageHandlerDelegate) {
        this.messageQueue = messageQueue;
        this.channelActiveHandler = channelActiveHandler;
        this.channelInactiveHandler = channelInactiveHandler;
        this.deviceAdapter = deviceAdapter;
        this.messageHandlerDelegate = messageHandlerDelegate;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        try {
            // 记录接收到的消息
            String clientInfo = ctx.channel().remoteAddress() != null ?
                    ctx.channel().remoteAddress().toString() : "未知";
            if (log.isDebugEnabled()) {
                log.debug("从 {} 接收到消息: {}", clientInfo, msg);
            }

            Device device = deviceAdapter.getDevice();
            if (device == null) {
                log.error("设备未配置，无法处理消息");
                return;
            }

            // 使用消息处理委托处理消息
            String response = deviceAdapter.processReceivedData(msg);

            // 如果需要响应，则发送响应
            if (response != null) {
                ctx.writeAndFlush(response + "\r");
            }
        } catch (Exception e) {
            log.error("处理消息时发生错误: {}", e.getMessage(), e);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        String clientInfo = ctx.channel().remoteAddress() != null ?
                ctx.channel().remoteAddress().toString() : "未知";
        log.info("通道激活: {}", clientInfo);

        if (channelActiveHandler != null) {
            channelActiveHandler.accept(ctx.channel());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String clientInfo = ctx.channel().remoteAddress() != null ?
                ctx.channel().remoteAddress().toString() : "未知";
        log.info("通道断开: {}", clientInfo);

        if (channelInactiveHandler != null) {
            channelInactiveHandler.accept(ctx.channel());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String clientInfo = ctx.channel().remoteAddress() != null ?
                ctx.channel().remoteAddress().toString() : "未知";
        log.error("通道 {} 连接异常: {}", clientInfo, cause.getMessage());
        ctx.close();
    }
}
