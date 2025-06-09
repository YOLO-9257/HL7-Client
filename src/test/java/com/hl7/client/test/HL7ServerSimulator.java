package com.hl7.client.test;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * HL7服务器模拟器
 * 用于测试HL7客户端连接功能
 */
@Slf4j
public class HL7ServerSimulator {

    private final int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private boolean running = false;

    /**
     * 构造函数
     *
     * @param port 监听端口
     */
    public HL7ServerSimulator(int port) {
        this.port = port;
    }

    /**
     * 启动服务器
     *
     * @return 是否成功启动
     */
    public boolean start() {
        try {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();

            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new StringDecoder());
                            pipeline.addLast(new StringEncoder());
                            pipeline.addLast(new HL7ServerHandler());
                        }
                    });

            // 绑定端口并启动服务器
            ChannelFuture future = bootstrap.bind(port).sync();
            if (future.isSuccess()) {
                serverChannel = future.channel();
                running = true;
                log.info("HL7服务器已启动，监听端口: {}", port);
                return true;
            } else {
                log.error("HL7服务器启动失败");
                stop();
                return false;
            }
        } catch (Exception e) {
            log.error("启动HL7服务器时出错: {}", e.getMessage());
            stop();
            return false;
        }
    }

    /**
     * 停止服务器
     */
    public void stop() {
        running = false;
        if (serverChannel != null) {
            serverChannel.close();
            serverChannel = null;
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }

        log.info("HL7服务器已停止");
    }

    /**
     * 检查服务器是否正在运行
     *
     * @return 是否正在运行
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * HL7服务器处理器
     * 处理接收到的HL7消息
     */
    private static class HL7ServerHandler extends SimpleChannelInboundHandler<String> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) {
            log.info("收到HL7消息: \n{}", msg);

            // 生成ACK应答消息
            String ackMessage = generateAckMessage(msg);

            // 发送ACK应答
            ctx.writeAndFlush(ackMessage);
            log.info("发送ACK应答: \n{}", ackMessage);
        }

        /**
         * 生成ACK应答消息
         *
         * @param originalMessage 原始消息
         * @return ACK消息
         */
        private String generateAckMessage(String originalMessage) {
            // 简单实现，提取MSH段信息并构建ACK消息
            String[] lines = originalMessage.split("\r");
            if (lines.length > 0 && lines[0].startsWith("MSH")) {
                String[] mshFields = lines[0].split("\\|");
                if (mshFields.length >= 10) {
                    StringBuilder ack = new StringBuilder();
                    // MSH段
                    ack.append("MSH|^~\\&|")
                       .append(mshFields[5]).append("|")  // 接收应用作为发送应用
                       .append(mshFields[6]).append("|")  // 接收设施作为发送设施
                       .append(mshFields[3]).append("|")  // 发送应用作为接收应用
                       .append(mshFields[4]).append("|")  // 发送设施作为接收设施
                       .append(HL7DeviceSimulator.getCurrentTimeStamp()).append("||")
                       .append("ACK").append("|")
                       .append("ACK").append(mshFields[9]).append("|")  // 控制ID
                       .append("P|2.5\r");

                    // MSA段 - 应答段
                    ack.append("MSA|AA|").append(mshFields[9]).append("|消息处理成功\r");

                    return ack.toString();
                }
            }

            // 默认ACK消息
            return "MSH|^~\\&|TEST_SERVER|TEST_FACILITY|TEST_CLIENT|TEST_CLIENT_FACILITY|"
                    + HL7DeviceSimulator.getCurrentTimeStamp()
                    + "||ACK|ACK12345|P|2.5\rMSA|AA|12345|消息处理成功\r";
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            log.info("客户端连接: {}", ctx.channel().remoteAddress());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.info("客户端断开连接: {}", ctx.channel().remoteAddress());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("连接异常: {}", cause.getMessage());
            ctx.close();
        }
    }

    /**
     * 主方法，启动服务器
     */
    public static void main(String[] args) {
        int port = 8088; // 默认端口

        // 从命令行参数获取端口
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("端口格式错误，使用默认端口: " + port);
            }
        }

        HL7ServerSimulator server = new HL7ServerSimulator(port);
        if (server.start()) {
            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

            System.out.println("HL7测试服务器已启动，监听端口: " + port);
            System.out.println("按Ctrl+C停止服务器");
        } else {
            System.err.println("HL7测试服务器启动失败");
        }
    }
}
