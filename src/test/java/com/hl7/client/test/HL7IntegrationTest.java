package com.hl7.client.test;

import lombok.extern.slf4j.Slf4j;

/**
 * HL7集成测试
 * 启动模拟服务器和模拟客户端进行交互测试
 */
@Slf4j
public class HL7IntegrationTest {

    private static final int SERVER_PORT = 8088;

    /**
     * 主测试方法
     */
    public static void main(String[] args) {
        log.info("=== 开始HL7集成测试 ===");

        // 启动模拟服务器
        HL7MockServer server = new HL7MockServer(SERVER_PORT);
        server.start();

        try {
            // 等待服务器启动完成
            Thread.sleep(1000);

            log.info("=== 服务器已启动，开始发送测试消息 ===");

            // 执行各种测试场景
            HL7TestScenarios.basicTest("localhost", SERVER_PORT);
            Thread.sleep(1000);
            log.info("接收到消息数: {}", server.getMessageCount());

            server.clearMessages();
            HL7TestScenarios.multiTypeMessageTest("localhost", SERVER_PORT);
            Thread.sleep(1000);
            log.info("接收到消息数: {}", server.getMessageCount());

            server.clearMessages();
            HL7TestScenarios.batchTest("localhost", SERVER_PORT, 5);
            Thread.sleep(2000);
            log.info("接收到消息数: {}", server.getMessageCount());

            server.clearMessages();
            HL7TestScenarios.errorMessageTest("localhost", SERVER_PORT);
            Thread.sleep(1000);
            log.info("接收到消息数: {}", server.getMessageCount());

            log.info("=== 集成测试完成 ===");

            // 允许用户查看结果
            log.info("测试已完成，按回车键退出...");
            System.in.read();

        } catch (Exception e) {
            log.error("测试过程中出错: {}", e.getMessage());
        } finally {
            // 停止服务器
            server.stop();
        }
    }

    /**
     * 运行完整测试套件
     */
    public static void runFullTestSuite() {
        HL7MockServer server = new HL7MockServer(SERVER_PORT);
        server.start();

        try {
            // 等待服务器启动
            Thread.sleep(1000);

            // 运行所有测试场景
            HL7TestScenarios.runAllTests("localhost", SERVER_PORT);

            // 等待所有消息处理完成
            Thread.sleep(2000);

            // 输出结果
            log.info("测试完成，共接收到 {} 条消息", server.getMessageCount());

        } catch (Exception e) {
            log.error("测试套件执行出错: {}", e.getMessage());
        } finally {
            server.stop();
        }
    }
}
