package com.hl7.client.infrastructure.adapter.network;

import com.hl7.client.domain.model.Device;
import com.hl7.client.infrastructure.config.CommunicationConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 基于Netty的客户端网络适配器
 * 提供高效的网络连接能力
 */
@Slf4j
@Component
public class NettySocketAdapter extends AbstractNettyAdapter {

    private String host;
    private Channel channel;
    private EventLoopGroup group;

    @Value("${hl7.netty.auto-process:true}")
    private boolean autoProcessEnabled;

    @Autowired
    public NettySocketAdapter() {
        super();
    }

    /**
     * 从配置初始化网络客户端
     * @param config 网络配置
     * @param device 设备信息
     */
    public void initializeFromConfig(CommunicationConfig.NetworkConfig config, Device device) {
        super.initialize(device);
        this.host = config.getHost();
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
        // 从连接参数中解析主机、端口、协议和连接模式
        try {
            String[] paramParts = params.split(":");
            if (paramParts.length < 2) {
                throw new IllegalArgumentException("连接参数格式错误，正确格式为：host:port[:protocol][:isLongConnection]");
            }

            this.host = paramParts[0];
            this.port = Integer.parseInt(paramParts[1]);

            // 记录更详细的参数信息
            log.info("初始化网络适配器 - 设备: {}, 主机: {}, 端口: {}",
                    device.getName(), host, port);

            if (paramParts.length >= 3) {
                this.protocol = paramParts[2]; // 设置协议类型到父类的变量中
                log.info("协议类型: {}", protocol);
            } else {
                this.protocol = "TCP"; // 默认使用TCP
            }

            if (paramParts.length >= 4) {
                boolean isLongConnection = Boolean.parseBoolean(paramParts[3]);
                log.info("连接模式: {}", isLongConnection ? "长连接" : "短连接");
                // 可以根据连接模式调整策略，如设置心跳等
            }
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
        // 确保之前的连接已关闭
        disconnect();

        group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 5秒连接超时
                    .option(ChannelOption.SO_KEEPALIVE, true) // 启用TCP keepalive
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new StringDecoder());
                            pipeline.addLast(new StringEncoder());
                            pipeline.addLast(new NettyMessageHandler(
                                    receivedMessages,
                                    null, // 不需要连接回调
                                    null, // 不需要断开回调
                                    NettySocketAdapter.this, // 设备适配器自身
                                    messageHandlerDelegate // 消息处理委托
                            ));
                        }
                    });

            // 添加重试逻辑
            int maxRetries = 3;
            int retryCount = 0;
            boolean connected = false;
            Exception lastException = null;

            while (!connected && retryCount < maxRetries) {
                try {
                    log.info("尝试连接到 {}:{} (尝试 {}/{})", host, port, retryCount + 1, maxRetries);
                    ChannelFuture future = bootstrap.connect(host, port);
                    // 等待连接超时或完成
                    future.await(5000, TimeUnit.MILLISECONDS);

                    if (future.isSuccess()) {
                        channel = future.channel();
                        log.info("成功连接到设备 {} 的 {}:{}", device.getName(), host, port);
                        connected = true;
                    } else {
                        Throwable cause = future.cause();
                        log.warn("连接尝试 {}/{} 失败: {}", retryCount + 1, maxRetries,
                                cause != null ? cause.getMessage() : "未知原因");
                        retryCount++;

                        // 重试前等待
                        if (retryCount < maxRetries) {
                            Thread.sleep(1000); // 等待1秒后重试
                        }
                    }
                } catch (Exception e) {
                    lastException = e;
                    log.warn("连接尝试 {}/{} 抛出异常: {}", retryCount + 1, maxRetries, e.getMessage());
                    retryCount++;

                    // 重试前等待
                    if (retryCount < maxRetries) {
                        try {
                            Thread.sleep(1000); // 等待1秒后重试
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }

            if (!connected) {
                log.error("在 {} 次尝试后无法连接到设备 {}: {}", maxRetries, device.getName(),
                        lastException != null ? lastException.getMessage() : "未知原因");
                disconnect();
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("连接设备失败: {}", e.getMessage());
            disconnect();
            return false;
        }
    }

    @Override
    public void disconnect() {
        // 关闭通道
        closeChannel(channel, "客户端通道");
        channel = null;

        // 关闭事件循环组
        shutdownEventLoopGroup(group, "客户端线程组");
        group = null;

        log.info("已断开设备 {} 的连接", device.getName());
    }

    @Override
    public boolean send(String data) {
        if (!isConnected()) {
            log.error("发送失败：设备未连接");
            return false;
        }

        try {
            ChannelFuture future = channel.writeAndFlush(data);
            future.sync();

            if (future.isSuccess()) {
                log.debug("成功发送数据到设备 {} 的 {}:{}", device.getName(), host, port);
                return true;
            } else {
                log.error("发送数据失败: {}", future.cause() != null ? future.cause().getMessage() : "未知原因");
                return false;
            }
        } catch (Exception e) {
            log.error("发送数据时发生异常: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String receive() {
        try {
            String message = receivedMessages.poll(10, TimeUnit.SECONDS);
            if (message != null) {
                log.debug("从设备 {} 接收到数据: {}", device.getName(), message);
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
        return channel != null && channel.isActive() && isEventLoopGroupValid(group);
    }

    @Override
    public String processReceivedData(String rawData) {
        return super.processReceivedData(rawData);
    }
}
