package com.hl7.client.infrastructure.adapter.network;

import com.hl7.client.application.MessageProcessor;
import com.hl7.client.domain.model.Device;
import com.hl7.client.domain.service.HL7MessageParser;
import com.hl7.client.infrastructure.config.CommunicationConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 基于Netty的服务器网络适配器
 * 提供高效的网络监听能力
 */
@Slf4j
@Component
public class NettyServerAdapter extends AbstractNettyAdapter {

    private Channel serverChannel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private final Set<Channel> connectedClients = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Autowired
    @Lazy
    private MessageProcessor messageProcessor;

    @Autowired
    private HL7MessageParser hl7MessageParser;

    @Value("${hl7.netty.auto-process:true}")
    private boolean autoProcessEnabled;

    @Autowired
    public NettyServerAdapter() {
        super();
    }

    /**
     * 从配置初始化网络服务器
     * @param config 网络配置
     * @param device 设备信息
     */
    public void initializeFromConfig(CommunicationConfig.NetworkConfig config, Device device) {
        super.initialize(device);
        this.port = config.getPort();
        this.protocol = "TCP"; // 默认使用TCP
    }

    /**
     * 解析连接参数
     *
     * @param params 连接参数字符串
     */
    @Override
    protected void parseConnectionParams(String params) {
        try {
            String[] paramParts = params.split(":");
            if (paramParts.length < 1) {
                throw new IllegalArgumentException("连接参数格式错误，正确格式为：port[:protocol][:mode]");
            }

            this.port = Integer.parseInt(paramParts[0]);

            // 如果提供了协议类型，则记录下来
            if (paramParts.length >= 2) {
                this.protocol = paramParts[1];
            } else {
                this.protocol = "TCP"; // 默认使用TCP
            }

            log.info("初始化网络服务器适配器 - 设备: {}, 端口: {}, 协议: {}",
                    device.getName(), port, protocol);

        } catch (NumberFormatException e) {
            String errorMsg = "端口号格式错误: " + e.getMessage();
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        } catch (Exception e) {
            String errorMsg = "初始化网络适配器失败: " + e.getMessage();
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    @Override
    public boolean connect() {
        // 确保之前的服务已关闭
        disconnect();

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new StringDecoder());
                            pipeline.addLast(new StringEncoder());
                            pipeline.addLast(new NettyMessageHandler(
                                    receivedMessages,
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
                                    NettyServerAdapter.this, // 设备适配器自身
                                    messageHandlerDelegate // 消息处理委托
                            ));
                        }
                    });

            // 绑定端口并启动服务器
            ChannelFuture future = bootstrap.bind(port).sync();
            if (future.isSuccess()) {
                serverChannel = future.channel();

                // 修复：初始化连接客户端集合，添加null检查防止NPE
                try {
                    Channel parent = serverChannel.parent();
                    if (parent != null) {
                        parent.attr(NettyMessageHandler.CONNECTED_CLIENTS).set(connectedClients);
                        log.debug("成功设置连接客户端集合到父通道");
                    } else {
                        log.warn("服务器通道的父通道为null，无法设置连接客户端集合属性");
                        // 在serverChannel上设置属性，作为备选方案
                        serverChannel.attr(NettyMessageHandler.CONNECTED_CLIENTS).set(connectedClients);
                        log.debug("已在服务器通道上设置连接客户端集合属性作为替代");
                    }
                } catch (Exception e) {
                    log.warn("设置连接客户端集合时出错: {}", e.getMessage());
                    // 备用处理：在服务器通道上设置属性
                    serverChannel.attr(NettyMessageHandler.CONNECTED_CLIENTS).set(connectedClients);
                }

                log.info("服务器已启动，监听端口: {}", port);
                return true;
            } else {
                log.error("启动服务器失败: {}", future.cause() != null ? future.cause().getMessage() : "未知原因");
                return false;
            }
        } catch (Exception e) {
            log.error("启动服务器失败: {}", e.getMessage());
            disconnect();
            return false;
        }
    }

    @Override
    public void disconnect() {
        // 清空连接的客户端集合
        connectedClients.clear();

        // 关闭服务器通道
        closeChannel(serverChannel, "服务器通道");
        serverChannel = null;

        // 关闭事件循环组
        shutdownEventLoopGroup(bossGroup, "Boss线程组");
        bossGroup = null;

        shutdownEventLoopGroup(workerGroup, "Worker线程组");
        workerGroup = null;

        log.info("服务器已停止，端口释放: {}", port);
    }

    @Override
    public boolean send(String data) {
        // 广播消息到所有连接的客户端
        if (connectedClients.isEmpty()) {
            log.warn("未连接任何客户端，无法发送数据");
            return false;
        }

        int successCount = 0;
        for (Channel channel : connectedClients) {
            if (isChannelValid(channel)) {
                try {
                    ChannelFuture future = channel.writeAndFlush(data);
                    future.sync();
                    if (future.isSuccess()) {
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("向客户端 {} 发送数据失败: {}", channel.remoteAddress(), e.getMessage());
                }
            }
        }

        if (successCount > 0) {
            log.info("成功向 {}/{} 个客户端发送数据: {}", successCount, connectedClients.size(), data);
            return true;
        } else {
            log.warn("没有成功发送数据到任何客户端");
            return false;
        }
    }

    @Override
    public String receive() {
        try {
            String message = receivedMessages.poll(10, TimeUnit.SECONDS);
            if (message != null) {
                log.debug("从客户端接收到数据: {}", message);
            }
            return message;
        } catch (InterruptedException e) {
            log.error("接收数据被中断: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public boolean isConnected() {
        return serverChannel != null && serverChannel.isActive() &&
               isEventLoopGroupValid(bossGroup) && isEventLoopGroupValid(workerGroup);
    }

    @Override
    public String processReceivedData(String rawData) {
        return super.processReceivedData(rawData);
    }
}
