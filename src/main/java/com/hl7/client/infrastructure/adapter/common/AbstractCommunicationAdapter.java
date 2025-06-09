package com.hl7.client.infrastructure.adapter.common;

import com.hl7.client.domain.constants.ApplicationConstants;
import com.hl7.client.domain.model.Device;
import com.hl7.client.domain.model.Message;
import com.hl7.client.domain.model.MessageStatus;
import com.hl7.client.infrastructure.adapter.DeviceAdapter;
import com.hl7.client.infrastructure.adapter.network.MessageHandlerDelegate;
import com.hl7.client.infrastructure.adapter.network.message.MessageCompletionStrategyManager;
import com.hl7.client.infrastructure.util.SnowflakeIdGenerator;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 抽象通信适配器
 * 统一串口和网络通信的接口和处理逻辑
 */
@Slf4j
public abstract class AbstractCommunicationAdapter implements DeviceAdapter {

    @Getter @Setter
    protected Device device;

    @Getter
    protected final BlockingQueue<String> receivedMessages;

    @Getter @Setter
    protected StringBuilder messageBuffer = new StringBuilder();

    @Getter
    protected LocalDateTime lastMessageTime = LocalDateTime.now();

    @Autowired
    protected MessageHandlerDelegate messageHandlerDelegate;

    @Autowired
    protected MessageCompletionStrategyManager strategyManager;

    @Value("${" + ApplicationConstants.PropertyKeys.MESSAGE_QUEUE_MAX_SIZE + ":"
        + ApplicationConstants.MessageProcessing.MAX_RECEIVED_QUEUE_SIZE + "}")
    private int maxQueueSize;

    @Value("${" + ApplicationConstants.PropertyKeys.MESSAGE_BUFFER_MAX_SIZE + ":"
        + ApplicationConstants.MessageProcessing.MAX_BUFFER_SIZE_BYTES + "}")
    private int maxBufferSize;

    // 消息统计
    private final AtomicLong totalReceivedBytes = new AtomicLong(0);
    private final AtomicLong totalReceivedMessages = new AtomicLong(0);
    private final AtomicLong totalInvalidMessages = new AtomicLong(0);
    private LocalDateTime lastStatsResetTime = LocalDateTime.now();

    /**
     * 构造函数，初始化接收消息队列
     */
    public AbstractCommunicationAdapter() {
        // 使用有界队列，防止内存溢出
        this.receivedMessages = new LinkedBlockingQueue<>(
            ApplicationConstants.MessageProcessing.MAX_RECEIVED_QUEUE_SIZE);
    }

    /**
     * 初始化设备
     * @param device 设备信息
     */
    @Override
    public void initialize(Device device) {
        this.device = device;
        log.info("设备 {} ({}) 初始化", device.getName(), device.getModel());
    }

    /**
     * 处理接收到的原始数据
     *
     * @param rawData 接收到的原始数据字符串
     * @return 如果需要响应，返回响应内容；否则返回null
     */
    @Override
    public String processReceivedData(String rawData) {
        if (rawData == null || rawData.isEmpty()) {
            return null;
        }

        try {
            updateLastMessageTime();
            totalReceivedBytes.addAndGet(rawData.length());

            // 1. 检查缓冲区大小并添加数据
            if (!addToBuffer(rawData)) {
                return null;
            }

            // 2. 检查设备是否初始化
            if (!isDeviceInitialized()) {
                return null;
            }

            // 3. 创建消息对象和检查完整性
            String fullMsg = messageBuffer.toString();
            Message message = createMessage(fullMsg);

            // 4. 检查消息是否完整，如果不完整或需要响应，则返回响应
            String responseToSend = messageHandlerDelegate.isMessageComplete(message);
            if (responseToSend != null) {
                return responseToSend;
            }

            // 5. 处理完整消息并加入队列
            processAndQueueMessage(fullMsg);

            // 6. 记录统计信息
            logStatsPeriodically();

            return null;
        } catch (Exception e) {
            log.error("处理接收数据时发生错误: {}", e.getMessage(), e);
            resetBuffer();
            return null;
        }
    }

    /**
     * 更新最后消息时间
     */
    private void updateLastMessageTime() {
        lastMessageTime = LocalDateTime.now();
    }

    /**
     * 添加数据到缓冲区，并检查缓冲区大小
     *
     * @param rawData 接收的原始数据
     * @return 是否成功添加（如果缓冲区溢出则返回false）
     */
    private boolean addToBuffer(String rawData) {
        // 检查缓冲区大小，如果超过限制，清空缓冲区并返回错误
        if (messageBuffer.length() + rawData.length() > maxBufferSize) {
            log.error("消息缓冲区超过最大限制 {} 字节，当前: {} 字节，新数据: {} 字节",
                maxBufferSize, messageBuffer.length(), rawData.length());
            resetBuffer();
            return false;
        }

        // 添加到缓冲区
        messageBuffer.append(rawData);
        return true;
    }

    /**
     * 检查设备是否初始化
     *
     * @return 设备是否已初始化
     */
    private boolean isDeviceInitialized() {
        if (device == null) {
            log.error("设备未初始化，无法处理消息");
            return false;
        }
        return true;
    }

    /**
     * 创建消息对象
     *
     * @param content 消息内容
     * @return 消息对象
     */
    private Message createMessage(String content) {
        return Message.builder()
            .id(String.valueOf(SnowflakeIdGenerator.getInstance().nextId()))
            .deviceId(device.getId())
            .deviceModel(device.getModel())
            .rawContent(content)
            .receivedTime(LocalDateTime.now())
            .status(MessageStatus.NEW.name())
            .build();
    }

    /**
     * 处理完整的消息并将其加入队列
     *
     * @param fullMsg 完整消息内容
     */
    private void processAndQueueMessage(String fullMsg) {
        // 处理消息
        log.info("接收到完整消息，长度: {}", fullMsg.length());
        messageHandlerDelegate.processMessage(device, fullMsg);

        // 添加到队列
        addToQueue(fullMsg);

        // 重置缓冲区
        resetBuffer();

        // 更新计数器
        totalReceivedMessages.incrementAndGet();
    }

    /**
     * 添加消息到队列
     *
     * @param fullMsg 完整消息内容
     */
    private void addToQueue(String fullMsg) {
        // 添加到消息队列，如果队列已满，不阻塞而是丢弃最老的消息
        if (receivedMessages.size() >= maxQueueSize) {
            String oldestMessage = receivedMessages.poll();
            if (oldestMessage != null) {
                log.warn("接收队列已满，丢弃最早的消息: {}",
                    oldestMessage.substring(0, Math.min(20, oldestMessage.length())) + "...");
            }
        }

        // 尝试添加消息到队列，如果失败则记录错误
        try {
            if (!receivedMessages.offer(fullMsg, 1, TimeUnit.SECONDS)) {
                log.error("无法将消息添加到队列，队列可能已满");
            }
        } catch (InterruptedException e) {
            log.error("添加消息到队列时被中断: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 重置缓冲区
     */
    private void resetBuffer() {
        messageBuffer.setLength(0);
    }

    /**
     * 定期记录统计信息
     */
    private void logStatsPeriodically() {
        LocalDateTime now = LocalDateTime.now();
        if (ChronoUnit.HOURS.between(lastStatsResetTime, now) >= 1) {
            String deviceName = device != null ? device.getName() : "未知";
            String deviceModel = device != null ? device.getModel() : "未知";

            log.info("设备 {} ({}) 通信统计 - 接收消息数: {}, 接收字节数: {}, 无效消息数: {}",
                deviceName, deviceModel,
                totalReceivedMessages.get(),
                totalReceivedBytes.get(),
                totalInvalidMessages.get());

            // 重置统计数据
            lastStatsResetTime = now;
            totalReceivedBytes.set(0);
            totalReceivedMessages.set(0);
            totalInvalidMessages.set(0);
        }
    }

    /**
     * 检查并清理过期的消息缓冲
     * 如果消息缓冲区中的数据长时间未更新，则清空缓冲区
     */
    public void checkAndCleanBuffer() {
        if (messageBuffer.length() > 0) {
            long millisSinceLastMessage = Duration.between(lastMessageTime, LocalDateTime.now()).toMillis();

            if (millisSinceLastMessage > ApplicationConstants.Timeout.MESSAGE_BUFFER_TIMEOUT_MS) {
                log.warn("消息缓冲区数据已超时 ({} ms)，当前缓冲区大小: {} 字节，清空缓冲区",
                    millisSinceLastMessage, messageBuffer.length());
                resetBuffer();
            }
        }
    }

    /**
     * 获取消息接收队列的当前大小
     * @return 队列大小
     */
    public int getReceivedMessagesQueueSize() {
        return receivedMessages.size();
    }

    /**
     * 获取消息缓冲区的当前大小
     * @return 缓冲区大小（字节）
     */
    public int getMessageBufferSize() {
        return messageBuffer.length();
    }

    /**
     * 获取消息统计信息
     * @return 统计信息Map
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("receivedMessages", totalReceivedMessages.get());
        stats.put("receivedBytes", totalReceivedBytes.get());
        stats.put("invalidMessages", totalInvalidMessages.get());
        stats.put("queueSize", receivedMessages.size());
        stats.put("bufferSize", messageBuffer.length());
        stats.put("lastMessageTime", lastMessageTime);
        return stats;
    }

    /**
     * 记录无效消息
     */
    protected void incrementInvalidMessagesCount() {
        totalInvalidMessages.incrementAndGet();
    }


}
