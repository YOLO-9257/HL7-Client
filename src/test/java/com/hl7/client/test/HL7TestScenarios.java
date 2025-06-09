package com.hl7.client.test;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * HL7测试场景
 * 包含多种测试场景和测试用例
 */
@Slf4j
public class HL7TestScenarios {

    /**
     * 基础测试 - 单个消息发送
     *
     * @param host 服务器地址
     * @param port 服务器端口
     */
    public static void basicTest(String host, int port) {
        log.info("=== 执行基础测试 - 单个消息发送 ===");
        HL7DeviceSimulator simulator = new HL7DeviceSimulator(host, port);

        // 创建测试消息
        String message = HL7DeviceSimulator.generateORU_R01("P00001", "测试患者", "GLU", "110");

        // 发送消息
        boolean success = simulator.sendMessage(message);
        log.info("消息发送{}", success ? "成功" : "失败");
    }

    /**
     * 批量测试 - 多个消息发送
     *
     * @param host 服务器地址
     * @param port 服务器端口
     * @param count 消息数量
     */
    public static void batchTest(String host, int port, int count) {
        log.info("=== 执行批量测试 - {}个消息发送 ===", count);
        HL7DeviceSimulator simulator = new HL7DeviceSimulator(host, port);
        simulator.setLongConnection(true);

        // 创建测试消息
        List<String> messages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            messages.add(HL7DeviceSimulator.generateORU_R01(
                    "P" + (10000 + i),
                    "批量患者" + i,
                    "GLU",
                    String.valueOf(70 + (int)(Math.random() * 70))
            ));
        }

        // 发送消息
        simulator.connect();
        int success = simulator.sendMessages(messages);
        simulator.disconnect();

        log.info("成功发送 {} 条消息，共 {} 条", success, count);
    }

    /**
     * 压力测试 - 多线程模拟多个设备并发发送
     *
     * @param host 服务器地址
     * @param port 服务器端口
     * @param deviceCount 设备数量
     * @param messagePerDevice 每个设备发送的消息数
     */
    public static void stressTest(String host, int port, int deviceCount, int messagePerDevice) {
        log.info("=== 执行压力测试 - {}个设备，每个设备{}条消息 ===", deviceCount, messagePerDevice);

        final CountDownLatch latch = new CountDownLatch(deviceCount);
        ExecutorService executorService = Executors.newFixedThreadPool(deviceCount);

        for (int i = 0; i < deviceCount; i++) {
            final int deviceId = i;
            executorService.submit(() -> {
                try {
                    HL7DeviceSimulator simulator = new HL7DeviceSimulator(host, port);
                    simulator.setLongConnection(true);
                    simulator.setDelayBetweenMessages(100); // 更快的发送速度

                    // 创建测试消息
                    List<String> messages = new ArrayList<>();
                    for (int j = 0; j < messagePerDevice; j++) {
                        messages.add(HL7DeviceSimulator.generateORU_R01(
                                "P" + (deviceId * 1000 + j),
                                "压测患者D" + deviceId + "M" + j,
                                "TEST" + j,
                                String.valueOf(70 + (int)(Math.random() * 70))
                        ));
                    }

                    // 发送消息
                    simulator.connect();
                    int success = simulator.sendMessages(messages);
                    simulator.disconnect();

                    log.info("设备 {} 成功发送 {} 条消息，共 {} 条", deviceId, success, messagePerDevice);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            // 等待所有线程完成
            latch.await();
            executorService.shutdown();
            log.info("压力测试完成，总共发送 {} 条消息", deviceCount * messagePerDevice);
        } catch (InterruptedException e) {
            log.error("压力测试中断: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 多种类型消息测试
     *
     * @param host 服务器地址
     * @param port 服务器端口
     */
    public static void multiTypeMessageTest(String host, int port) {
        log.info("=== 执行多种类型消息测试 ===");
        HL7DeviceSimulator simulator = new HL7DeviceSimulator(host, port);
        simulator.setLongConnection(true);
        simulator.connect();

        // 1. 血糖仪消息
        String glucoseMessage = HL7DeviceSimulator.generateORU_R01("P10001", "王健康", "GLU", "110");
        simulator.sendMessage(glucoseMessage);

        // 2. 血气分析仪消息
        String bloodGasMessage =
                "MSH|^~\\&|ANALYZER|LAB|LIS|HOSPITAL|" + HL7DeviceSimulator.getCurrentTimeStamp() +
                "||ORU^R01|BG12345|P|2.5\r" +
                "PID|||P10002||李医生||19750315|F\r" +
                "OBR|1|ORD123456||BG Panel|\r" +
                "OBX|1|NM|pH||7.35|pH|7.35-7.45|N|||F\r" +
                "OBX|2|NM|pCO2||45|mmHg|35-45|N|||F\r" +
                "OBX|3|NM|pO2||80|mmHg|80-100|N|||F\r";
        simulator.sendMessage(bloodGasMessage);

        // 3. 生化分析仪消息
        String biochemistryMessage =
                "MSH|^~\\&|BIOCHEM|LAB|LIS|HOSPITAL|" + HL7DeviceSimulator.getCurrentTimeStamp() +
                "||ORU^R01|BC12345|P|2.5\r" +
                "PID|||P10003||张患者||19650520|M\r" +
                "OBR|1|ORD789012||LIVER|\r" +
                "OBX|1|NM|ALT||45|U/L|0-40|H|||F\r" +
                "OBX|2|NM|AST||42|U/L|0-40|H|||F\r" +
                "OBX|3|NM|ALP||88|U/L|40-150|N|||F\r" +
                "OBX|4|NM|TBIL||0.8|mg/dL|0.1-1.2|N|||F\r";
        simulator.sendMessage(biochemistryMessage);

        // 4. 血液分析仪消息
        String hematologyMessage =
                "MSH|^~\\&|HEMA|LAB|LIS|HOSPITAL|" + HL7DeviceSimulator.getCurrentTimeStamp() +
                "||ORU^R01|HE12345|P|2.5\r" +
                "PID|||P10004||赵病人||19900710|F\r" +
                "OBR|1|ORD345678||CBC|\r" +
                "OBX|1|NM|WBC||6.5|10^3/uL|4.5-11.0|N|||F\r" +
                "OBX|2|NM|RBC||4.8|10^6/uL|4.0-5.5|N|||F\r" +
                "OBX|3|NM|HGB||14.2|g/dL|12.0-16.0|N|||F\r" +
                "OBX|4|NM|HCT||42|%|36-48|N|||F\r" +
                "OBX|5|NM|PLT||250|10^3/uL|150-450|N|||F\r";
        simulator.sendMessage(hematologyMessage);

        simulator.disconnect();
        log.info("多种类型消息测试完成");
    }

    /**
     * 错误消息测试
     *
     * @param host 服务器地址
     * @param port 服务器端口
     */
    public static void errorMessageTest(String host, int port) {
        log.info("=== 执行错误消息测试 ===");
        HL7DeviceSimulator simulator = new HL7DeviceSimulator(host, port);

        // 1. 缺少必要段的消息
        String missingSegmentMessage =
                "MSH|^~\\&|ERROR|LAB|LIS|HOSPITAL|" + HL7DeviceSimulator.getCurrentTimeStamp() +
                "||ORU^R01|ERR12345|P|2.5\r" +
                // 缺少PID段
                "OBR|1|ORD123456||ERROR|\r" +
                "OBX|1|NM|TEST||123|unit|100-200|N|||F\r";
        simulator.sendMessage(missingSegmentMessage);

        // 2. 格式错误的消息
        String malformedMessage =
                "MSHXX|ERROR|LAB|LIS|HOSPITAL|" + HL7DeviceSimulator.getCurrentTimeStamp() +
                "||ORU^R01|ERR12346|P|2.5\r" +
                "PID|||P99999||错误患者||19800101|M\r" +
                "OBR|1|ORD123456||ERROR|\r" +
                "OBX|1|NM|TEST||abc|unit|100-200|N|||F\r"; // 值类型不匹配
        simulator.sendMessage(malformedMessage);

        // 3. 不完整的消息
        String incompleteMessage =
                "MSH|^~\\&|ERROR|LAB|LIS|HOSPITAL|" + HL7DeviceSimulator.getCurrentTimeStamp() +
                "||ORU^R01|ERR12347|P|2.5\r" +
                "PID|||P99998||不完整患者||";
        simulator.sendMessage(incompleteMessage);

        log.info("错误消息测试完成");
    }

    /**
     * 执行所有测试
     *
     * @param host 服务器地址
     * @param port 服务器端口
     */
    public static void runAllTests(String host, int port) {
        log.info("开始执行所有测试...");

        basicTest(host, port);
        batchTest(host, port, 10);
        multiTypeMessageTest(host, port);
        errorMessageTest(host, port);
        stressTest(host, port, 5, 10);

        log.info("所有测试执行完成");
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        String host = "localhost";
        int port = 8088;

        // 从命令行参数获取主机和端口
        if (args.length >= 2) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        }

        log.info("使用服务器地址: {}:{}", host, port);

        // 运行所有测试
        runAllTests(host, port);
    }
}
