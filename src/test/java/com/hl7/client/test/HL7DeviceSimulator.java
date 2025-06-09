package com.hl7.client.test;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * HL7仪器模拟器
 * 用于模拟医疗仪器发送HL7消息，测试系统接收和解析功能
 * 支持双向通信，可以接收服务器的响应
 */
@Slf4j
@Data
public class HL7DeviceSimulator {

    // 连接配置
    private String host = "localhost";
    private int port = 8088;
    private boolean longConnection = true;
    private int delayBetweenMessages = 1000; // 消息间延迟(毫秒)
    private int responseTimeout = 5000; // 等待响应超时(毫秒)

    // Socket连接
    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;

    // 接收线程
    private Thread receiverThread;
    private volatile boolean running = false;

    // 响应处理
    private Consumer<String> responseHandler;
    private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();

    // HL7消息结束符
    private static final String MESSAGE_TERMINATOR = "\r";

    /**
     * 构造函数
     */
    public HL7DeviceSimulator() {
    }

    /**
     * 构造函数
     *
     * @param host 服务器地址
     * @param port 服务器端口
     */
    public HL7DeviceSimulator(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 设置响应处理器
     *
     * @param responseHandler 响应处理回调函数
     */
    public void setResponseHandler(Consumer<String> responseHandler) {
        this.responseHandler = responseHandler;
    }

    /**
     * 连接到服务器
     *
     * @return 是否连接成功
     */
    public boolean connect() {
        try {
            socket = new Socket(host, port);
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
            log.info("已连接到服务器 {}:{}", host, port);

            // 启动接收线程
            startReceiver();

            return true;
        } catch (IOException e) {
            log.error("连接服务器失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 启动接收线程
     */
    private void startReceiver() {
        if (receiverThread != null && receiverThread.isAlive()) {
            return;
        }

        running = true;
        receiverThread = new Thread(this::receiveMessages);
        receiverThread.setDaemon(true);
        receiverThread.setName("HL7-Receiver");
        receiverThread.start();
        log.info("已启动消息接收线程");
    }

    /**
     * 接收消息线程
     */
    private void receiveMessages() {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            StringBuilder messageBuffer = new StringBuilder();
            int c;

            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    // 逐字符读取
                    while ((c = reader.read()) != -1) {
                        char ch = (char) c;
                        messageBuffer.append(ch);

                        // 检测消息结束符
                        if (ch == '\r' || messageBuffer.toString().endsWith(MESSAGE_TERMINATOR)) {
                            String message = messageBuffer.toString();
                            if (message.contains("ack")) {
                                String testMessage2 = " \u00025R|2|^^^PT     |1.03|INR||||||||20240923150322\n" +
                                        " \u000365\n" +
                                        " \u00026R|3|^^^FIB    |15.5|s||||||||20240923150322\n" +
                                        " \u000305\n" +
                                        " \u00027R|4|^^^FIB    |2.86|g/L||||||||20240923150322\n" +
                                        " \u00037B\n" +
                                        " \u00020R|5|^^^TT     |15.0|s||||||||20240923150322\n" +
                                        " \u0003F3\n" +
                                        " \u00021L|1\n" +
                                        " \u00033A\r";
                                sendMessage(testMessage2);
                            }

//                            handleResponse(message);
                            messageBuffer = new StringBuilder();
                        }
                    }


                    // 如果读到流结束，且连接已断开
                    if (socket.isClosed() || !socket.isConnected()) {
                        break;
                    }
                } catch (IOException e) {
                    if (!running) {
                        break; // 如果已经停止运行，则退出
                    }
                    log.error("接收消息时出错: {}", e.getMessage());

                    // 尝试重新连接
                    if (longConnection && !socket.isConnected()) {
                        log.info("尝试重新连接...");
                        disconnect();
                        if (connect()) {
                            log.info("重新连接成功");
                        } else {
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("接收线程异常: {}", e.getMessage());
        } finally {
            log.info("接收线程已停止");
        }
    }

    /**
     * 处理接收到的响应
     *
     * @param response 响应消息
     */
    private void handleResponse(String response) {
        log.info("收到服务器响应: \n{}", response);

        // 添加到响应队列
        responseQueue.offer(response);

        // 如果设置了响应处理器，调用处理器
        if (responseHandler != null) {
            try {
                responseHandler.accept(response);
            } catch (Exception e) {
                log.error("处理响应时出错: {}", e.getMessage());
            }
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        // 停止接收线程
        running = false;

        if (receiverThread != null) {
            receiverThread.interrupt();
            receiverThread = null;
        }

        try {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }

            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
                socket = null;
            }

            log.info("已断开与服务器的连接");
        } catch (IOException e) {
            log.error("断开连接时出错: {}", e.getMessage());
        }
    }

    /**
     * 发送单条HL7消息
     *
     * @param message HL7消息内容
     * @return 是否发送成功
     */
    public boolean sendMessage(String message) {
        try {
            if (socket == null || socket.isClosed()) {
                if (!connect()) {
                    return false;
                }
            }

            // 添加消息起始符和结束符
            String formattedMessage = formatHL7Message(message);

            // 发送消息
            outputStream.write(formattedMessage.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            log.info("消息发送成功: \n{}", message);

            // 如果是短连接模式，发送后关闭
            if (!longConnection) {
                disconnect();
            }

            return true;
        } catch (IOException e) {
            log.error("发送消息失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 发送HL7消息并等待响应
     *
     * @param message HL7消息内容
     * @return 服务器响应，如果超时返回null
     */
    public String sendMessageAndWaitResponse(String message) {
        // 清空之前的响应
        responseQueue.clear();

        // 发送消息
        if (!sendMessage(message)) {
            return null;
        }

        // 等待响应
        try {
            return responseQueue.poll(responseTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("等待响应时被中断: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * 发送多条HL7消息
     *
     * @param messages HL7消息列表
     * @return 成功发送的消息数
     */
    public int sendMessages(List<String> messages) {
        int successCount = 0;

        try {
            if (socket == null || socket.isClosed()) {
                if (!connect()) {
                    return 0;
                }
            }

            for (String message : messages) {
                // 添加消息起始符和结束符
                String formattedMessage = formatHL7Message(message);

                // 发送消息
                outputStream.write(formattedMessage.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                log.info("消息发送成功: \n{}", message);
                successCount++;

                // 等待指定时间
                if (delayBetweenMessages > 0) {
                    Thread.sleep(delayBetweenMessages);
                }
            }

            // 如果是短连接模式，发送后关闭
            if (!longConnection) {
                disconnect();
            }
        } catch (IOException | InterruptedException e) {
            log.error("发送消息过程中出错: {}", e.getMessage());
        }

        return successCount;
    }

    /**
     * 发送多条HL7消息并获取所有响应
     *
     * @param messages HL7消息列表
     * @return 服务器响应列表
     */
    public List<String> sendMessagesAndGetResponses(List<String> messages) {
        List<String> responses = new ArrayList<>();

        for (String message : messages) {
            String response = sendMessageAndWaitResponse(message);
            if (response != null) {
                responses.add(response);
            }
        }

        return responses;
    }

    /**
     * 格式化HL7消息
     *
     * @param message 原始HL7消息
     * @return 格式化后的消息
     */
    private String formatHL7Message(String message) {
        if (!message.endsWith(MESSAGE_TERMINATOR)) {
//            message += MESSAGE_TERMINATOR;
        }
        return message;
    }

    /**
     * 生成示例ORU_R01结果消息
     *
     * @param patientId 患者ID
     * @param patientName 患者姓名
     * @param testId 检测项目ID
     * @param testValue 检测值
     * @return HL7格式消息
     */
    public static String generateORU_R01(String patientId, String patientName, String testId, String testValue) {
        StringBuilder sb = new StringBuilder();

        // MSH段 - 消息头
        sb.append("MSH|^~\\&|LIS|HOSPITAL|EHR|HOSPITAL|")
          .append(getCurrentTimeStamp())
          .append("||ORU^R01|").append(generateMessageId())
          .append("|P|2.5\r");

        // PID段 - 患者信息
        sb.append("PID|||").append(patientId).append("||")
          .append(patientName).append("||19800101|M\r");

        // OBR段 - 观察请求
        sb.append("OBR|1|").append(generateOrderNumber()).append("||")
          .append(testId).append("|").append(getCurrentDate()).append("\r");

        // OBX段 - 观察结果
        sb.append("OBX|1|ST|").append(testId).append("^血糖测试||")
          .append(testValue).append("|mg/dL|70-110|H|||F\r");

        return sb.toString();
    }

    /**
     * 生成当前时间戳
     */
    public static String getCurrentTimeStamp() {
        return java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /**
     * 生成当前日期
     */
    public static String getCurrentDate() {
        return java.time.LocalDate.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    /**
     * 生成消息ID
     */
    public static String generateMessageId() {
        return "MSG" + System.currentTimeMillis();
    }

    /**
     * 生成订单号
     */
    public static String generateOrderNumber() {
        return "ORD" + System.currentTimeMillis();
    }

    /**
     * 主测试方法
     */
    public static void main(String[] args) {
        // 创建模拟器实例
        HL7DeviceSimulator simulator = new HL7DeviceSimulator("localhost", 8088);
        simulator.setLongConnection(true);

        // 设置响应处理器
        simulator.setResponseHandler(response -> {
            System.out.println("收到响应: " + response);
            // 这里可以添加对响应的解析和处理逻辑
        });

        String testMessage1= " \u00021H|\\^&||||||||||P|1|20240923162541\n" +
                " \u0003FF\n" +
                " \u00022P|1||^240923005917\n" +
                " \u0003FF\n" +
                " \u00023O|1|^240923005917||^^^PT\\^^^PT\\^^^FIB\\^^^FIB\\^^^TT\n" +
                " \u0003FF\n" +
                " \u00024R|1|^^^PT     |13.3|s||||||||20240923150322\n" +
                " \u0003F0\n" ;
        String testMessage2 = " \u00025R|2|^^^PT     |1.03|INR||||||||20240923150322\n" +
                " \u000365\n" +
                " \u00026R|3|^^^FIB    |15.5|s||||||||20240923150322\n" +
                " \u000305\n" +
                " \u00027R|4|^^^FIB    |2.86|g/L||||||||20240923150322\n" +
                " \u00037B\n" +
                " \u00020R|5|^^^TT     |15.0|s||||||||20240923150322\n" +
                " \u0003F3\n" +
                " \u00021L|1\n" +
                " \u00033A\r";

        // 准备测试消息
        List<String> testMessages = new ArrayList<>();
        testMessages.add(testMessage1);
        testMessages.add(testMessage2);

        // 发送测试消息并等待响应
        simulator.connect();
        simulator.sendMessageAndWaitResponse(testMessage1);


        // 方式1: 单独发送每条消息并处理响应
//        for (String message : testMessages) {
//            String response = simulator.sendMessageAndWaitResponse(message);
//            if (response != null) {
//                System.out.println("发送消息后收到响应: " + response);
//            } else {
//                System.out.println("发送消息后未收到响应或响应超时");
//            }
//        }
//
        // 方式2: 批量发送消息
//        int success = simulator.sendMessages(testMessages);
//        System.out.println("成功发送 " + success + " 条消息，共 " + testMessages.size() + " 条");

        // 等待一段时间以确保收到所有响应
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 关闭连接
        simulator.disconnect();
    }
}
