package com.hl7.client.test;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HL7模拟服务器
 * 用于模拟HL7消息的发送和接收，支持双向通信
 */
@Slf4j
public class HL7MockServer {
    // HL7 MLLP协议帧定义
    private static final char START_OF_BLOCK = '\u001b'; // VT (ASCII 11)
    private static final char END_OF_BLOCK = '\u001c';   // FS (ASCII 28)
    private static final char CARRIAGE_RETURN = '\r';    // CR (ASCII 13)

    private final int port;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private OutputStream outputStream;
    private InputStream inputStream;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService executorService;
    private final ScheduledExecutorService messageProcessorService;

    // 线程安全的消息队列
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

    // 接收到的消息和响应记录
    @Getter
    private final List<String> receivedMessages = new ArrayList<>();
    @Getter
    private final List<String> sentResponses = new ArrayList<>();

    // 自动应答模式（收到消息后自动返回ACK）
    private boolean autoAckMode = true;

    /**
     * 构造函数
     *
     * @param port 监听端口
     */
    public HL7MockServer(int port) {
        this.port = port;
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "hl7-server-worker");
            thread.setDaemon(true);
            return thread;
        });
        this.messageProcessorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "hl7-message-processor");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 启动服务器
     */
    public void start() {
        if (running.get()) {
            log.warn("服务器已经在运行中");
            return;
        }

        try {
            serverSocket = new ServerSocket(port);
            running.set(true);
            log.info("HL7模拟服务器已启动，监听端口: {}", port);

            // 启动消息处理线程
            messageProcessorService.scheduleWithFixedDelay(
                this::processMessageQueue, 0, 100, TimeUnit.MILLISECONDS);

            // 启动连接接收线程
            executorService.submit(this::acceptConnections);

        } catch (IOException e) {
            log.error("启动服务器失败: {}", e.getMessage(), e);
            running.set(false);
        }
    }

    /**
     * 接收连接
     */
    private void acceptConnections() {
        while (running.get() && !serverSocket.isClosed()) {
            try {
                log.info("等待客户端连接...");
                Socket socket = serverSocket.accept();
                log.info("接收到新连接: {}", socket.getInetAddress().getHostAddress());

                // 保存当前客户端连接
                synchronized (this) {
                    // 如果已有连接，先关闭
                    if (clientSocket != null && !clientSocket.isClosed()) {
                        try {
                            clientSocket.close();
                        } catch (IOException e) {
                            log.warn("关闭旧连接时出错", e);
                        }
                    }

                    clientSocket = socket;
                    outputStream = clientSocket.getOutputStream();
                    inputStream = clientSocket.getInputStream();
                }

                // 启动消息接收线程
                executorService.submit(() -> receiveMessages(socket));

            } catch (IOException e) {
                if (running.get()) {
                    log.error("接收连接时出错: {}", e.getMessage(), e);
                    try {
                        Thread.sleep(1000); // 避免CPU过度使用
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    /**
     * 接收消息
     *
     * @param socket 客户端Socket
     */
    private void receiveMessages(Socket socket) {
        try (BufferedInputStream bis = new BufferedInputStream(socket.getInputStream())) {
            ByteArrayOutputStream messageBuffer = new ByteArrayOutputStream();
            boolean inMessage = false;
            int b;

            while (running.get() && !socket.isClosed() && (b = bis.read()) != -1) {
                // 检查消息起始标记
                if (b == START_OF_BLOCK) {
                    inMessage = true;
                    messageBuffer.reset();
                    continue;
                }

                // 如果在消息内，记录字节
                if (inMessage) {
                    messageBuffer.write(b);

                    // 检查是否到达消息结束
                    if (b == CARRIAGE_RETURN && messageBuffer.size() > 1) {
                        int prev = messageBuffer.toByteArray()[messageBuffer.size() - 2] & 0xFF;
                        if (prev == END_OF_BLOCK) {
                            // 提取实际消息内容（去除结束标记）
                            byte[] bytes = messageBuffer.toByteArray();
                            String message = new String(bytes, 0, bytes.length - 2);

                            // 添加到消息队列
                            messageQueue.add(message);
                            inMessage = false;
                        }
                    }
                }
            }
        } catch (IOException e) {
            if (running.get()) {
                log.error("接收消息时出错: {}", e.getMessage(), e);
            }
        } finally {
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                log.error("关闭套接字时出错", e);
            }
        }
    }

    /**
     * 处理消息队列
     */
    private void processMessageQueue() {
        try {
            String message = messageQueue.poll(100, TimeUnit.MILLISECONDS);
            if (message != null) {
                processHL7Message(message);

                // 如果开启自动应答，则返回ACK
                if (autoAckMode) {
                    String ack = generateAck(message);
                    sendResponse(ack);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("处理消息队列时出错", e);
        }
    }

    /**
     * 处理HL7消息
     *
     * @param message HL7消息
     */
    private void processHL7Message(String message) {
        log.info("接收到HL7消息:\n{}", message);
        synchronized (receivedMessages) {
            receivedMessages.add(message);
        }
    }

    /**
     * 生成HL7 ACK响应消息
     *
     * @param message 原始HL7消息
     * @return ACK响应消息
     */
    private String generateAck(String message) {
        // 简单ACK生成示例，实际实现可能需要根据HL7标准解析原消息并构建ACK
        String[] lines = message.split("\r|\n");
        String msh = "";

        // 查找MSH段
        for (String line : lines) {
            if (line.startsWith("MSH") || line.trim().startsWith("MSH")) {
                msh = line.trim();
                break;
            }
        }

        if (msh.isEmpty()) {
            // 如果没找到MSH段，返回简单ACK
            return "MSH|^~\\&|RECEIVING_APP|RECEIVING_FACILITY|SENDING_APP|SENDING_FACILITY|"
                + getCurrentTimestamp() + "||ACK|" + getMessageControlId() + "|P|2.3\r"
                + "MSA|AA|" + getMessageControlId() + "|消息已接收|";
        }

        // 从MSH段提取发送和接收应用/设施
        String[] mshFields = msh.split("\\|");
        String sendingApp = mshFields.length > 2 ? mshFields[2] : "";
        String sendingFacility = mshFields.length > 3 ? mshFields[3] : "";
        String receivingApp = mshFields.length > 4 ? mshFields[4] : "";
        String receivingFacility = mshFields.length > 5 ? mshFields[5] : "";
        String messageControlId = mshFields.length > 9 ? mshFields[9] : getMessageControlId();

        // 构建ACK
        return "MSH|^~\\&|" + receivingApp + "|" + receivingFacility + "|"
            + sendingApp + "|" + sendingFacility + "|" + getCurrentTimestamp()
            + "||ACK|" + getMessageControlId() + "|P|2.3\r"
            + "MSA|AA|" + messageControlId + "|消息已接收|";
    }

    /**
     * 获取当前时间戳（HL7格式）
     */
    private String getCurrentTimestamp() {
        return new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
    }

    /**
     * 获取消息控制ID
     */
    private String getMessageControlId() {
        return "MSG" + System.currentTimeMillis() % 10000;
    }

    /**
     * 设置自动应答模式
     *
     * @param autoAck 是否自动应答
     */
    public void setAutoAckMode(boolean autoAck) {
        this.autoAckMode = autoAck;
    }

    /**
     * 主动发送HL7消息
     *
     * @param message HL7消息内容
     * @return 是否发送成功
     */
    public boolean sendMessage(String message) {
        return sendData(formatHL7Message(message));
    }

    /**
     * 发送响应消息
     *
     * @param message 响应消息内容
     * @return 是否发送成功
     */
    public boolean sendResponse(String message) {
        synchronized (sentResponses) {
            sentResponses.add(message);
        }
        return sendData(formatHL7Message(message));
    }

    /**
     * 格式化HL7消息（添加MLLP帧）
     *
     * @param message 原始HL7消息
     * @return 格式化后的消息
     */
    private byte[] formatHL7Message(String message) {
        // 确保消息以\r结尾
        if (!message.endsWith("\r")) {
            message = message + "\r";
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(START_OF_BLOCK);
        try {
            baos.write(message.getBytes());
        } catch (IOException e) {
            log.error("格式化HL7消息时出错", e);
        }
        baos.write(END_OF_BLOCK);
        baos.write(CARRIAGE_RETURN);

        return baos.toByteArray();
    }

    /**
     * 发送数据
     *
     * @param data 要发送的数据
     * @return 是否发送成功
     */
    private boolean sendData(byte[] data) {
        synchronized (this) {
            if (outputStream == null || clientSocket == null || clientSocket.isClosed()) {
                log.error("没有有效的客户端连接，无法发送消息");
                return false;
            }

            try {
                outputStream.write(data);
                outputStream.flush();
                log.info("已发送消息，长度: {} 字节", data.length);
                return true;
            } catch (IOException e) {
                log.error("发送消息时出错: {}", e.getMessage(), e);
                return false;
            }
        }
    }

    /**
     * 等待连接建立（用于测试/客户端模式）
     *
     * @param timeoutMs 超时时间（毫秒）
     * @return 是否连接成功
     */
    public boolean waitForConnection(long timeoutMs) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            synchronized (this) {
                if (clientSocket != null && !clientSocket.isClosed() && clientSocket.isConnected()) {
                    return true;
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * 等待接收指定数量的消息
     *
     * @param count 期望的消息数量
     * @param timeoutMs 超时时间（毫秒）
     * @return 是否接收到足够消息
     */
    public boolean waitForMessages(int count, long timeoutMs) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            synchronized (receivedMessages) {
                if (receivedMessages.size() >= count) {
                    return true;
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * 停止服务器
     */
    public void stop() {
        if (!running.get()) {
            log.warn("服务器已经停止");
            return;
        }

        running.set(false);

        // 关闭当前连接
        synchronized (this) {
            if (clientSocket != null && !clientSocket.isClosed()) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    log.error("关闭客户端连接时出错: {}", e.getMessage(), e);
                }
                clientSocket = null;
            }

            outputStream = null;
            inputStream = null;
        }

        // 关闭服务器Socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log.error("关闭服务器Socket时出错: {}", e.getMessage(), e);
        }

        // 关闭线程池
        executorService.shutdownNow();
        messageProcessorService.shutdownNow();

        log.info("HL7模拟服务器已停止");
    }

    /**
     * 清除接收到的消息
     */
    public void clearMessages() {
        synchronized (receivedMessages) {
            receivedMessages.clear();
        }
        synchronized (sentResponses) {
            sentResponses.clear();
        }
    }

    /**
     * 获取接收到的消息数量
     *
     * @return 消息数量
     */
    public int getMessageCount() {
        synchronized (receivedMessages) {
            return receivedMessages.size();
        }
    }

    /**
     * 检查服务器是否正在运行
     *
     * @return 服务器运行状态
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 连接是否有效
     *
     * @return 连接状态
     */
    public boolean isConnected() {
        synchronized (this) {
            return clientSocket != null && !clientSocket.isClosed() && clientSocket.isConnected();
        }
    }

    /**
     * 主方法 - 示例用法
     */
    public static void main(String[] args) {
        // 默认端口
        int port = 8088;

        // 从命令行参数获取端口
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                log.error("无效的端口参数: {}", args[0]);
            }
        }

        // 创建并启动服务器
        HL7MockServer server = new HL7MockServer(port);
        server.start();

        // 等待连接建立
        if (!server.waitForConnection(10000)) {
            log.warn("等待连接超时，将在30秒后继续执行...");
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 示例：发送测试消息
        sendTestMessage(server);

        // 等待用户输入停止
        log.info("服务器已启动，按回车键停止...");
        try {
            System.in.read();
        } catch (IOException e) {
            log.error("读取输入时出错: {}", e.getMessage());
        }

        server.stop();
    }

    /**
     * 发送测试消息示例
     *
     * @param server HL7服务器实例
     */
    private static void sendTestMessage(HL7MockServer server) {
        try {
            if (!server.isConnected()) {
                log.warn("未建立连接，无法发送测试消息");
                return;
            }

            // 示例HL7消息
            String testMessage1 = "MSH|^~\\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20240923162541||ORM^O01|MSG00001|P|2.3\r" +
                "PID|1||240923005917||DOE^JOHN||19800101|M\r" +
                "ORC|NW|240923005917||||||20240923150322\r" +
                "OBR|1|240923005917||PT^PT^L^FIB^FIB^L^TT|||20240923150322";

            log.info("发送测试消息...");
            boolean success = server.sendMessage(testMessage1);

            if (success) {
                log.info("测试消息发送成功，等待响应...");

                // 等待10秒看是否收到响应
                if (server.waitForMessages(1, 10000)) {
                    log.info("已收到响应消息！");
                } else {
                    log.warn("在指定时间内未收到响应消息");
                }
            } else {
                log.error("测试消息发送失败");
            }
        } catch (Exception e) {
            log.error("发送测试消息时发生异常", e);
        }
    }
}
