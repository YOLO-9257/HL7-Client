package com.hl7.client.test;

import com.hl7.client.domain.model.Device;
import com.hl7.client.domain.model.DeviceStatus;
import com.hl7.client.infrastructure.adapter.DeviceAdapter;
import com.hl7.client.infrastructure.adapter.network.NettyServerAdapter;
import com.hl7.client.infrastructure.util.SnowflakeIdGenerator;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 服务器模式测试类
 * 用于测试Netty服务器模式功能
 */
@Slf4j
public class ServerModeTest {

    /**
     * 测试服务器模式功能
     *
     * @param port 服务器监听端口
     */
    public static void testServerMode(int port) {
        log.info("=== 开始服务器模式测试 ===");
        log.info("监听端口: {}", port);

        // 1. 创建并启动服务器
        Device serverDevice = createServerDevice(port);
        log.info("创建服务器设备: {}", serverDevice.getName());

        DeviceAdapter serverAdapter = new NettyServerAdapter();
        serverAdapter.initialize(serverDevice);
        log.info("服务器适配器初始化完成");

        // 启动服务器
        log.info("启动服务器...");
        boolean started = serverAdapter.connect();

        if (!started) {
            log.error("服务器启动失败！");
            return;
        }

        log.info("服务器已启动，等待客户端连接");

        // 创建线程来接收消息
        CountDownLatch messageReceived = new CountDownLatch(1);
        Thread receiveThread = new Thread(() -> {
            try {
                log.info("等待接收客户端消息...");
                String message = serverAdapter.receive();
                if (message != null) {
                    log.info("服务器收到消息: \n{}", message);
                    messageReceived.countDown();
                } else {
                    log.warn("服务器未收到消息");
                }
            } catch (Exception e) {
                log.error("接收消息线程异常: {}", e.getMessage(), e);
            }
        });
        receiveThread.start();

        try {
            // 2. 创建客户端连接到服务器
            Thread.sleep(1000); // 等待服务器完全启动
            log.info("创建测试客户端连接到服务器...");

            try (Socket clientSocket = new Socket("localhost", port)) {
                log.info("客户端已连接到服务器");

                // 3. 发送测试消息
                String testMessage = "MSH|^~\\&|TEST_CLIENT|TEST_FACILITY|SERVER|HOSPITAL|"
                        + HL7DeviceSimulator.getCurrentTimeStamp()
                        + "||ORU^R01|TEST123|P|2.5\r"
                        + "PID|||TEST123||测试患者||19800101|M\r"
                        + "OBR|1|" + HL7DeviceSimulator.generateOrderNumber() + "||TEST|\r"
                        + "OBX|1|ST|TEST||这是服务器模式测试消息|||||F\r";

                log.info("客户端发送测试消息: \n{}", testMessage);
                OutputStream out = clientSocket.getOutputStream();
                out.write(testMessage.getBytes(StandardCharsets.UTF_8));
                out.flush();

                // 4. 等待服务器接收消息
                if (messageReceived.await(10, TimeUnit.SECONDS)) {
                    log.info("服务器成功接收到消息");

                    // 5. 从服务器发送响应
                    String response = "MSH|^~\\&|SERVER|HOSPITAL|TEST_CLIENT|TEST_FACILITY|"
                            + HL7DeviceSimulator.getCurrentTimeStamp()
                            + "||ACK|ACK123|P|2.5\r"
                            + "MSA|AA|TEST123|服务器处理成功\r";

                    log.info("服务器发送响应: \n{}", response);
                    serverAdapter.send(response);

                    // 等待一下，确保响应被发送
                    Thread.sleep(1000);
                } else {
                    log.error("等待服务器接收消息超时");
                }
            } catch (IOException e) {
                log.error("客户端连接出错: {}", e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("测试过程中出错: {}", e.getMessage(), e);
        } finally {
            // 6. 停止服务器
            log.info("停止服务器");
            serverAdapter.disconnect();
            log.info("服务器已停止");
        }

        log.info("=== 服务器模式测试结束 ===");
    }

    /**
     * 创建服务器设备对象
     *
     * @param port 监听端口
     * @return 服务器设备
     */
    private static Device createServerDevice(int port) {
        return Device.builder()
                .id(String.valueOf(SnowflakeIdGenerator.getInstance().nextId()))
                .name("测试服务器")
                .model("SERVER_TEST")
                .manufacturer("测试厂商")
                .connectionType("NETWORK")
                .connectionParams(String.format("%d:TCP:SERVER", port))
                .status(DeviceStatus.DISCONNECTED)
                .build();
    }

    /**
     * 主方法，执行测试
     */
    public static void main(String[] args) {
        int port = 8088;

        // 从命令行参数获取端口
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("端口格式错误，使用默认端口: " + port);
            }
        }

        // 执行测试
        testServerMode(port);
    }
}
