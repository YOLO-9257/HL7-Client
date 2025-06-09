package com.hl7.client.application;

import com.hl7.client.domain.constants.ApplicationConstants;
import com.hl7.client.domain.model.Device;
import com.hl7.client.domain.model.Message;
import com.hl7.client.domain.model.MessageStatus;
import com.hl7.client.domain.service.DeviceService;
import com.hl7.client.domain.service.MessageProcessService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 消息处理器
 * 负责消息的接收、处理和发送
 */
@Slf4j
@Component
public class MessageProcessor {

    private final DeviceService deviceService;
    private final MessageProcessService messageProcessService;

    @Lazy
    private final DeviceManager deviceManager;

    private final Queue<Message> messageQueue;

    @Getter
    private final Map<String, Message> processedMessages = new ConcurrentHashMap<>();

    // 性能监控指标
    private final AtomicInteger totalMessagesProcessed = new AtomicInteger(0);
    private final AtomicInteger failedMessagesCount = new AtomicInteger(0);
    private final AtomicInteger successMessagesCount = new AtomicInteger(0);
    private final AtomicLong totalProcessingTimeMs = new AtomicLong(0);
    private LocalDateTime lastStatsResetTime = LocalDateTime.now();

    // 队列健康状态标志
    private boolean queueHealthy = true;

    // 队列相关配置
    @Value("${" + ApplicationConstants.PropertyKeys.MESSAGE_QUEUE_MAX_SIZE + ":"
            + ApplicationConstants.MessageProcessing.MAX_QUEUE_SIZE + "}")
    private int maxQueueSize;

    @Value("${" + ApplicationConstants.PropertyKeys.MESSAGE_PROCESS_INTERVAL + ":"
            + ApplicationConstants.MessageProcessing.MESSAGE_PROCESS_INTERVAL_MS + "}")
    private int messageProcessInterval;

    @Value("${" + ApplicationConstants.PropertyKeys.MESSAGE_BATCH_SIZE + ":"
            + ApplicationConstants.MessageProcessing.BATCH_SIZE + "}")
    private int batchSize;

    @Autowired
    public MessageProcessor(DeviceService deviceService,
                           @Lazy MessageProcessService messageProcessService,
                           @Lazy DeviceManager deviceManager) {
        this.deviceService = deviceService;
        this.messageProcessService = messageProcessService;
        this.deviceManager = deviceManager;

        // 初始化使用有界队列
        this.messageQueue = new LinkedBlockingQueue<>(
                ApplicationConstants.MessageProcessing.MAX_QUEUE_SIZE);
    }

    /**
     * 初始化方法，记录配置信息
     */
    @PostConstruct
    public void init() {
        log.info("消息处理器初始化 - 最大队列大小: {}, 处理间隔: {}ms, 批处理大小: {}",
                maxQueueSize, messageProcessInterval, batchSize);
    }

    /**
     * 接收设备消息并加入队列
     *
     * @param deviceId 设备ID
     * @return 接收到的消息
     */
    public Message receiveMessage(String deviceId) {
        Device device = deviceManager.getDevice(deviceId);
        if (device == null) {
            log.warn("设备 {} 不存在，无法接收消息", deviceId);
            return null;
        }

        // 接收消息
        Message message = deviceService.receiveMessage(device);
        if (message != null) {
            // 队列健康检查
            if (!isQueueHealthy()) {
                log.warn("消息队列状态不健康，可能影响消息处理效率");
            }

            // 将消息加入队列
            boolean added = addToQueue(message);
            if (added) {
                log.info("消息 {} 已加入处理队列", message.getId());
            } else {
                log.error("无法将消息 {} 加入队列，队列已满", message.getId());
                // 如果队列已满，标记消息为错误状态
                message.setStatus(MessageStatus.ERROR.name());
                message.setErrorMessage("消息队列已满，无法处理");
            }
        }

        return message;
    }

    /**
     * 添加消息到队列
     *
     * @param message 待加入队列的消息
     * @return 是否成功加入
     */
    private boolean addToQueue(Message message) {
        // 检查队列是否已满
        if (messageQueue.size() >= maxQueueSize) {
            log.warn("消息队列已满 (大小: {}), 无法添加新消息", messageQueue.size());
            queueHealthy = false;
            return false;
        }

        boolean added = messageQueue.offer(message);

        // 如果队列大小低于警戒线，标记为健康
        if (messageQueue.size() < maxQueueSize / 2) {
            queueHealthy = true;
        }

        return added;
    }

    /**
     * 立即处理消息
     *
     * @param message 需要处理的消息
     * @return 处理后的消息
     */
    public Message processMessageImmediately(Message message) {
        long startTime = System.currentTimeMillis();

        // 处理消息
        Message processedMessage = messageProcessService.processMessage(message);

        if (MessageStatus.INCOMPLETE.name().equals(processedMessage.getStatus())) {
            return processedMessage;
        }

        // 发送到服务端
        boolean sent = messageProcessService.sendToServer(processedMessage);
//        boolean sent = true;
        if (sent) {
            log.info("消息 {} 已处理并发送成功", processedMessage.getId());
            successMessagesCount.incrementAndGet();
        } else {
            log.warn("消息 {} 已处理但发送失败", processedMessage.getId());
            failedMessagesCount.incrementAndGet();
        }

        // 更新处理总数和处理时间
        totalMessagesProcessed.incrementAndGet();
        totalProcessingTimeMs.addAndGet(System.currentTimeMillis() - startTime);

        // 存储处理过的消息
        processedMessages.put(processedMessage.getId(), processedMessage);

        return processedMessage;
    }

    /**
     * 异步处理消息
     *
     * @param message 需要处理的消息
     * @return 包含处理结果的CompletableFuture
     */
    public CompletableFuture<Message> processMessageAsynchronously(Message message) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            // 先处理消息
            Message processedMessage = messageProcessService.processMessage(message);

            if (!MessageStatus.INCOMPLETE.name().equals(processedMessage.getStatus())) {
                // 异步发送到服务端
                messageProcessService.sendToServerAsync(processedMessage)
                    .thenAccept(sent -> {
                        if (Boolean.TRUE.equals(sent)) {
                            log.info("异步消息 {} 已处理并发送成功", processedMessage.getId());
                            successMessagesCount.incrementAndGet();
                        } else {
                            log.warn("异步消息 {} 已处理但发送失败", processedMessage.getId());
                            failedMessagesCount.incrementAndGet();
                        }

                        // 更新处理总数和处理时间
                        totalMessagesProcessed.incrementAndGet();
                        totalProcessingTimeMs.addAndGet(System.currentTimeMillis() - startTime);

                        // 存储处理过的消息
                        processedMessages.put(processedMessage.getId(), processedMessage);
                    });
            }

            return processedMessage;
        });
    }

    /**
     * 定时处理消息队列中的消息
     */
    @Scheduled(fixedDelayString = "${" + ApplicationConstants.PropertyKeys.MESSAGE_PROCESS_INTERVAL + ":"
            + ApplicationConstants.MessageProcessing.MESSAGE_PROCESS_INTERVAL_MS + "}")
    public void processMessageQueue() {
        int queueSize = messageQueue.size();
        if (queueSize == 0) {
            return; // 队列为空，无需处理
        }

        log.debug("开始处理消息队列，当前队列中有 {} 条消息", queueSize);

        // 检查队列大小，如果超过最大限制，记录警告
        if (queueSize > maxQueueSize) {
            log.warn("消息队列大小 ({}) 超过最大限制 ({})",
                    queueSize, maxQueueSize);
            // 标记队列状态为不健康
            queueHealthy = false;
        } else if (queueSize < maxQueueSize / 2) {
            // 队列恢复到一半以下，标记为健康
            queueHealthy = true;
        }

        // 从队列中取出消息并处理
        Message message;
        int processedCount = 0;
        List<CompletableFuture<Message>> futures = new ArrayList<>();

        // 使用批量处理模式
        while ((message = messageQueue.poll()) != null && processedCount < batchSize) {
            try {
                // 使用异步处理替代同步处理
                CompletableFuture<Message> future = processMessageAsynchronously(message);
                futures.add(future);
                processedCount++;
            } catch (Exception e) {
                log.error("处理队列中的消息 {} 时发生异常: {}",
                    message.getId(), e.getMessage());
                failedMessagesCount.incrementAndGet();
            }
        }

        // 等待所有异步任务完成（可选，视实际需求调整）
        if (!futures.isEmpty()) {
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(30, TimeUnit.SECONDS);
                log.info("批量处理完成，处理了 {} 条消息", processedCount);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error("等待批量处理完成时发生异常: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        // 记录当前队列状态
        log.debug("本次处理了 {} 条消息，当前队列中剩余 {} 条消息",
                processedCount, messageQueue.size());
    }

    /**
     * 定时重试失败的消息
     */
//    @Scheduled(fixedDelayString = "${" + ApplicationConstants.PropertyKeys.MESSAGE_RETRY_INTERVAL + ":"
//            + ApplicationConstants.MessageProcessing.MESSAGE_PROCESS_INTERVAL_MS * 12 + "}")
    public void retryFailedMessages() {
        Map<String, Message> failedMessages = messageProcessService.getFailedMessages();
        if (failedMessages.isEmpty()) {
            return;
        }

        log.info("开始重试失败消息，共 {} 条", failedMessages.size());

        // 重试每一条失败的消息
        for (Message message : failedMessages.values()) {
            try {
                boolean retried = messageProcessService.retryFailedMessage(message);
                if (retried) {
                    log.info("消息 {} 重试成功", message.getId());
                    successMessagesCount.incrementAndGet();
                } else {
                    log.warn("消息 {} 重试失败", message.getId());
                    failedMessagesCount.incrementAndGet();
                }
            } catch (Exception e) {
                log.error("重试消息 {} 时发生异常: {}", message.getId(), e.getMessage());
                failedMessagesCount.incrementAndGet();
            }
        }
    }

    /**
     * 定时记录处理统计信息
     */
    @Scheduled(fixedDelay = 3600000) // 每小时记录一次
    public void logProcessingStats() {
        int total = totalMessagesProcessed.get();
        if (total > 0) {
            log.info("消息处理统计 - 总处理: {}, 成功: {}, 失败: {}, 平均处理时间: {}ms",
                    total,
                    successMessagesCount.get(),
                    failedMessagesCount.get(),
                    total > 0 ? totalProcessingTimeMs.get() / total : 0);

            // 重置统计数据
            lastStatsResetTime = LocalDateTime.now();
            totalMessagesProcessed.set(0);
            successMessagesCount.set(0);
            failedMessagesCount.set(0);
            totalProcessingTimeMs.set(0);
        }
    }

    /**
     * 获取当前队列大小
     * @return 队列大小
     */
    public int getQueueSize() {
        return messageQueue.size();
    }

    /**
     * 判断队列是否处于健康状态
     * @return 队列健康状态
     */
    public boolean isQueueHealthy() {
        return queueHealthy;
    }

    /**
     * 获取消息处理统计信息
     * @return 处理统计Map
     */
    public Map<String, Object> getProcessingStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("queueSize", messageQueue.size());
        stats.put("queueHealthy", queueHealthy);
        stats.put("totalProcessed", totalMessagesProcessed.get());
        stats.put("successCount", successMessagesCount.get());
        stats.put("failedCount", failedMessagesCount.get());

        // 计算平均处理时间
        int total = totalMessagesProcessed.get();
        long avgProcessingTime = total > 0 ? totalProcessingTimeMs.get() / total : 0;
        stats.put("avgProcessingTimeMs", avgProcessingTime);

        // 计算自上次重置以来的时间
        Duration uptime = Duration.between(lastStatsResetTime, LocalDateTime.now());
        stats.put("statsDuration", uptime.toString());

        return stats;
    }

    /**
     * 清空消息队列
     * 用于系统关闭或紧急情况
     *
     * @return 清除的消息数量
     */
    public int clearQueue() {
        int size = messageQueue.size();
        messageQueue.clear();
        log.warn("消息队列已清空，移除了 {} 条消息", size);
        return size;
    }
}
