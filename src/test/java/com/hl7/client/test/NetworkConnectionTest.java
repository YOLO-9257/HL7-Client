package com.hl7.client.test;

import com.hl7.client.domain.model.Device;
import com.hl7.client.domain.model.DeviceStatus;
import com.hl7.client.infrastructure.adapter.network.NettySocketAdapter;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * 网络连接测试类
 * 专门用于测试Netty连接功能
 */
@Slf4j
public class NetworkConnectionTest {

    /**
     * 测试Netty连接功能
     *
     * @param host 服务器主机
     * @param port 服务器端口
     */
    public static void testNettyConnection(String host, int port) {
        log.info("=== 开始Netty连接测试 ===");
        log.info("目标服务器: {}:{}", host, port);

        // 1. 先启动测试服务器（如果需要）
        HL7ServerSimulator server = null;
        boolean startedLocalServer = false;

        // 如果是本地测试，启动服务器
        if ("localhost".equals(host) || "127.0.0.1".equals(host)) {
            log.info("启动本地测试服务器...");
            server = new HL7ServerSimulator(port);
            startedLocalServer = server.start();

            if (startedLocalServer) {
                log.info("本地测试服务器启动成功");
            } else {
                log.warn("本地测试服务器启动失败，将尝试连接可能已经存在的服务器");
            }
        }

        try {
            // 2. 创建测试设备
            Device testDevice = createTestDevice(host, port);
            log.info("创建测试设备: {}", testDevice.getName());

            // 3. 创建并初始化Netty适配器
            NettySocketAdapter adapter = new NettySocketAdapter();
            adapter.initialize(testDevice);
            log.info("Netty适配器初始化完成");

            // 4. 测试连接
            log.info("尝试连接...");
            boolean connected = adapter.connect();

            if (connected) {
                log.info("连接成功！");

                // 5. 发送测试消息
                String testMessage = "MSH|^~\\&|TEST|TEST_FACILITY|LIS|HOSPITAL|"
                        + HL7DeviceSimulator.getCurrentTimeStamp()
                        + "||ORU^R01|TEST123|P|2.5\r"
                        + "PID|||TEST123||测试患者||19800101|M\r"
                        + "OBR|1|" + HL7DeviceSimulator.generateOrderNumber() + "||TEST|\r"
                        + "OBX|1|ST|TEST||测试消息内容|||||F\r";

                log.info("发送测试消息: \n{}", testMessage);
                boolean sent = adapter.send(testMessage);

                if (sent) {
                    log.info("消息发送成功");

                    // 6. 尝试接收响应
                    log.info("等待接收响应...");
                    String response = adapter.receive();

                    if (response != null) {
                        log.info("接收到响应: \n{}", response);
                    } else {
                        log.warn("未接收到响应");
                    }
                } else {
                    log.error("消息发送失败");
                }

                // 7. 断开连接
                log.info("断开连接");
                adapter.disconnect();
                log.info("连接已断开");
            } else {
                log.error("连接失败！");
            }
        } catch (Exception e) {
            log.error("测试过程中出错: {}", e.getMessage(), e);
        } finally {
            // 8. 停止服务器（如果是本地启动的）
            if (server != null && startedLocalServer) {
                log.info("停止本地测试服务器");
                server.stop();
            }
        }

        log.info("=== Netty连接测试结束 ===");
    }

    /**
     * 创建测试设备对象
     *
     * @param host 服务器主机
     * @param port 服务器端口
     * @return 测试设备
     */
    private static Device createTestDevice(String host, int port) {
        return Device.builder()
                .id(UUID.randomUUID().toString())
                .name("测试设备")
                .model("测试型号")
                .manufacturer("测试厂商")
                .connectionType("NETWORK")
                .connectionParams(String.format("%s:%d:TCP:true", host, port))
                .status(DeviceStatus.DISCONNECTED)
                .build();
    }

    /**
     * 主方法，执行测试
     */
    public static void main(String[] args) {
        String host = "localhost";
        int port = 8088;

        // 从命令行参数获取主机和端口
        if (args.length >= 2) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        }

        // 执行测试
        testNettyConnection(host, port);
    }
}
