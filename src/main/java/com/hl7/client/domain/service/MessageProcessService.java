package com.hl7.client.domain.service;

import cn.hutool.core.map.MapUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.hl7.client.domain.constants.ApplicationConstants;
import com.hl7.client.domain.model.Message;
import com.hl7.client.domain.model.MessageStatus;
import com.hl7.client.domain.port.MessageProcessPort;
import com.hl7.client.infrastructure.exception.MessageProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 消息处理服务
 * 负责消息的解析、处理和发送
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProcessService implements MessageProcessPort {

    private final MessageParserFactory messageParserFactory;

    // 存储失败消息及其重试信息，因为大多数情况下失败消息不会太多
    private final Map<String, RetryInfo> failedMessages = new ConcurrentHashMap<>(64);

    // 注入RestTemplate用于HTTP通信
    private final RestTemplate restTemplate;

    // 共享的随机数生成器，用于指数退避的随机抖动
    private static final Random RANDOM = new Random();

    // 服务器地址
    @Value("${hl7.server.address}")
    private String serverAddress;

    // 最大重试次数，默认值为10
    @Value("${hl7.fail.retry:10}")
    private int maxRetryCount;

    // HTTP请求超时时间（毫秒）
    private static final int HTTP_TIMEOUT_MS = 15000;

    /**
     * 重试信息类，记录失败消息的重试相关信息
     */
    private static class RetryInfo {
        final Message message;
        int retryCount = 0;
        LocalDateTime lastRetryTime;
        LocalDateTime nextRetryTime;
        String lastErrorMessage;

        // 共享的随机数生成器，避免每次创建新实例
        private static final Random RANDOM = new Random();

        public RetryInfo(Message message) {
            this.message = message;
            this.lastRetryTime = LocalDateTime.now();
            calculateNextRetryTime();
        }

        /**
         * 计算下次重试时间，使用指数退避算法
         */
        void calculateNextRetryTime() {
            // 基础重试间隔（秒）
            int baseDelay = ApplicationConstants.Timeout.RETRY_INTERVAL_MS / 1000;

            // 使用指数退避算法: 基础延迟 * 2^重试次数 + 随机抖动
            // 使用位移操作代替幂运算，提高性能
            long delaySeconds = (long) baseDelay << Math.min(retryCount, 10);

            // 添加随机抖动，避免多个失败同时重试造成雪崩
            // 使用共享的随机数生成器
            delaySeconds = delaySeconds + RANDOM.nextInt((int) (delaySeconds * 0.2) + 1);

            // 最大延迟不超过30分钟
            delaySeconds = Math.min(delaySeconds, 1800);

            this.nextRetryTime = LocalDateTime.now().plusSeconds(delaySeconds);
        }

        /**
         * 是否可以重试
         */
        boolean canRetry() {
            return retryCount < ApplicationConstants.Timeout.MAX_RETRY_COUNT &&
                LocalDateTime.now().isAfter(nextRetryTime);
        }

        /**
         * 记录重试
         */
        void recordRetry(String errorMessage) {
            retryCount++;
            lastRetryTime = LocalDateTime.now();
            lastErrorMessage = errorMessage;
            calculateNextRetryTime();
        }

        /**
         * 获取重试信息
         */
        Map<String, Object> getInfo() {
            // 预分配容量，避免动态扩容
            Map<String, Object> info = new HashMap<>(5);
            info.put("messageId", message.getId());
            info.put("retryCount", retryCount);
            info.put("lastRetryTime", lastRetryTime);
            info.put("nextRetryTime", nextRetryTime);
            info.put("lastErrorMessage", lastErrorMessage);
            return info;
        }
    }

    /**
     * 处理消息
     * 解析消息内容并设置处理结果
     *
     * @param message 需要处理的消息
     * @return 处理后的消息
     * @throws MessageProcessingException 处理过程中发生错误
     */
    @Override
    public Message processMessage(Message message) {
        try {
            log.info("开始处理消息，ID: {}, 类型: {}", message.getId(), message.getMessageType());

            // 更新消息状态为处理中
            message.setStatus(MessageStatus.PROCESSING.name());

            // 解析消息
            Map<String, Object> parsedData = messageParserFactory.parseMessage(message);
            if (parsedData.containsKey("error") && (boolean) parsedData.get("error")) {
                String errorMessage = (String) parsedData.get("errorMessage");
                message.setStatus(MessageStatus.ERROR.name());
                message.setProcessResult("解析失败: " + errorMessage);
                message.setErrorMessage(errorMessage);
                log.error("消息 {} 解析失败: {}", message.getId(), errorMessage);

                // 添加到失败消息列表
                addToFailedMessages(message, errorMessage);
                return message;
            }

            // 判断消息是否完整
            if (parsedData.containsKey("INCOMPLETE") && !(boolean) parsedData.get("INCOMPLETE")) {
                message.setStatus(MessageStatus.INCOMPLETE.name());
                log.debug("消息 {} 不完整，等待后续数据", message.getId());
                return message;
            }

            // 设置处理结果
            message.setProcessResult(JSONUtil.toJsonStr(parsedData));
            message.setStatus(MessageStatus.PROCESSED.name());
            log.info("消息 {} 处理完成", message.getId());

            return message;
        } catch (Exception e) {
            log.error("处理消息 {} 过程中发生异常: {}", message.getId(), e.getMessage());
            message.setStatus(MessageStatus.ERROR.name());
            message.setProcessResult("处理异常: " + e.getMessage());
            message.setErrorMessage(e.getMessage());

            // 添加到失败消息列表
            addToFailedMessages(message, e.getMessage());

            // 抛出业务异常
            throw new MessageProcessingException("001", "处理消息失败: " + message.getId(), e);
        }
    }

    /**
     * 添加消息到失败列表
     *
     * @param message 失败的消息
     * @param errorMessage 错误信息
     */
    private void addToFailedMessages(Message message, String errorMessage) {
        RetryInfo retryInfo = failedMessages.computeIfAbsent(message.getId(), k -> new RetryInfo(message));
        retryInfo.lastErrorMessage = errorMessage;
    }

    /**
     * 异步处理消息
     *
     * @param message 需要处理的消息
     * @return 包含处理结果的CompletableFuture
     */
    @Async
    public CompletableFuture<Message> processMessageAsync(Message message) {

        try {
            Message result = processMessage(message);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            CompletableFuture<Message> future = new CompletableFuture<>();
            future.completeExceptionally(
                new MessageProcessingException("002", "异步处理消息失败: " + message.getId(), e));
            return future;
        }
    }

    /**
     * 发送消息到服务端
     *
     * @param message 需要发送的消息
     * @return 是否发送成功
     */
    @Override
    public boolean sendToServer(Message message) {
        Map<String, Object> requestBody = MapUtil.builder(new HashMap<String, Object>(5))
            .put("messageId", message.getId())
            .put("deviceId", message.getDeviceId())
            .put("deviceModel", message.getDeviceModel())
            .put("content", message.getRawContent())
            .put("processResult", message.getProcessResult())
            .put("type", message.getMessageType())
            .build();

        HttpResponse response = null;
        try {
            log.debug("开始发送消息到服务端ID: {}", message.getId());

            // 使用try-with-resources确保资源释放
            response = HttpRequest.post(serverAddress)
                .body(JSONUtil.toJsonStr(requestBody))
                .header("Content-Type", "application/json;charset=UTF-8") // 显式设置Content-Type
                .header("Connection", "close") // 使用短连接，避免连接泄漏
                .timeout(HTTP_TIMEOUT_MS) // 设置超时
                .execute();

            if (response.isOk()) {
                log.info("消息 {} 发送成功", message.getId());
                log.debug("服务端响应体: {}", response.body());
                // 从失败消息列表中移除（如果存在）
                failedMessages.remove(message.getId());
                return true;
            } else {
                String errorMessage = String.format("发送失败，服务端响应状态: %d, 响应体: %s",
                    response.getStatus(), response.body());
                log.warn("消息 {} {}", message.getId(), errorMessage);
                message.setErrorMessage(errorMessage);
                addToFailedMessages(message, errorMessage);
                return false;
            }
        } catch (HttpException e) {
            // 单独处理HTTP异常，提供更精确的错误信息
            String errorMessage = "发送HTTP异常: " + e.getMessage();
            log.error("发送消息 {} 到服务端过程中发生 HTTP 异常: {}", message.getId(), e.getMessage());
            message.setErrorMessage(errorMessage);
            addToFailedMessages(message, errorMessage);
            throw new MessageProcessingException("003", "发送消息到服务端失败: " + message.getId(), e);
        } catch (Exception e) {
            // 处理其他异常
            String errorMessage = "发送异常: " + e.getMessage();
            log.error("发送消息 {} 到服务端过程中发生异常: {}", message.getId(), e.getMessage(), e);
            message.setErrorMessage(errorMessage);
            addToFailedMessages(message, errorMessage);
            throw new MessageProcessingException("003", "发送消息到服务端失败: " + message.getId(), e);
        } finally {
            // 确保关闭响应资源
            if (response != null) {
                try {
                    response.close();
                } catch (Exception e) {
                    log.warn("关闭响应资源失败: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 使用退避重试算法发送消息
     *
     * @param message 需要发送的消息
     * @param maxRetries 最大重试次数
     * @param initialDelayMs 初始延迟（毫秒）
     * @return 是否发送成功
     */
    public boolean sendWithRetry(Message message, int maxRetries, long initialDelayMs) {
        int retries = 0;
        long delayMs = initialDelayMs;

        while (retries < maxRetries) {
            try {
                // 尝试发送
                boolean result = sendToServer(message);
                if (result) {
                    return true;
                }

                // 发送失败，增加重试计数
                retries++;
                if (retries >= maxRetries) {
                    break;
                }

                // 计算下次重试延迟：基础延迟 * 2^重试次数 + 随机抖动
                delayMs = (long) (initialDelayMs * Math.pow(2, retries))
                    + RANDOM.nextInt((int) (initialDelayMs * 0.1));

                // 最大延迟不超过30秒
                delayMs = Math.min(delayMs, 30000);

                log.info("消息 {} 发送失败，将在 {}ms 后进行第 {} 次重试",
                        message.getId(), delayMs, retries + 1);

                // 等待重试
                TimeUnit.MILLISECONDS.sleep(delayMs);

            } catch (Exception e) {
                log.error("消息 {} 发送重试过程中发生异常: {}", message.getId(), e.getMessage());
                retries++;
                if (retries >= maxRetries) {
                    break;
                }

                try {
                    // 发生异常时也使用指数退避
                    TimeUnit.MILLISECONDS.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.warn("消息 {} 在重试 {} 次后仍然发送失败", message.getId(), retries);
        return false;
    }

    /**
     * 异步发送消息到服务端
     *
     * @param message 需要发送的消息
     * @return 包含发送结果的CompletableFuture
     */
    @Async
    public CompletableFuture<Boolean> sendToServerAsync(Message message) {
        try {
            // 使用重试机制发送
            boolean result = sendWithRetry(message, 3, 1000);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("异步发送消息 {} 失败: {}", message.getId(), e.getMessage());
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(
                new MessageProcessingException("004", "异步发送消息失败: " + message.getId(), e));
            return future;
        }
    }

    /**
     * 重试发送失败的消息
     *
     * @param message 需要重试的消息
     * @return 重试是否成功
     */
    @Override
    public boolean retryFailedMessage(Message message) {
        if (message == null) {
            log.error("重试失败：消息对象为空");
            return false;
        }

        // 使用computeIfAbsent原子操作获取或创建重试信息
        RetryInfo retryInfo = failedMessages.computeIfAbsent(message.getId(),
            k -> {
                log.debug("创建新的重试信息记录，消息ID: {}", message.getId());
                return new RetryInfo(message);
            });

        // 检查是否可以重试
        if (!retryInfo.canRetry()) {
            log.warn("消息 {} 重试次数已达上限 ({}次) 或还未到重试时间 (下次: {})",
                    message.getId(), retryInfo.retryCount, retryInfo.nextRetryTime);
            return false;
        }

        log.info("尝试重新发送失败消息，ID: {}, 第 {} 次重试",
                message.getId(), retryInfo.retryCount + 1);

        try {
            // 使用退避重试算法发送，而不是直接调用sendToServer
            // 这样可以在单次重试失败时自动进行多次尝试
            boolean success = sendWithRetry(message, 2, 500);
            if (success) {
                // 从失败消息列表中移除
                failedMessages.remove(message.getId());
                log.info("失败消息 {} 重发成功", message.getId());
            } else {
                String errorMessage = "重试发送失败";
                log.warn("失败消息 {} 重发失败", message.getId());
                retryInfo.recordRetry(errorMessage);
            }

            return success;
        } catch (Exception e) {
            log.error("重试消息 {} 时发生异常: {}", message.getId(), e.getMessage());
            retryInfo.recordRetry(e.getMessage());
            return false;
        }
    }

    /**
     * 获取所有失败消息
     *
     * @return 失败消息列表
     */
    public Map<String, Message> getFailedMessages() {
        Map<String, Message> messages = new HashMap<>(failedMessages.size());
        failedMessages.forEach((id, retryInfo) -> messages.put(id, retryInfo.message));
        return messages;
    }

    /**
     * 获取失败消息的详细重试信息
     *
     * @return 重试信息列表
     */
    public Map<String, Map<String, Object>> getFailedMessagesDetails() {
        Map<String, Map<String, Object>> details = new HashMap<>(failedMessages.size());
        failedMessages.forEach((id, retryInfo) -> details.put(id, retryInfo.getInfo()));
        return details;
    }

    /**
     * 清理过期的失败消息
     * 移除超过最大重试次数且最后重试时间超过1天的消息
     * 或者最后重试时间超过7天的消息（无论重试次数）
     *
     * @return 清理的消息数量
     */
    public int cleanupExpiredFailedMessages() {
        if (failedMessages.isEmpty()) {
            return 0;
        }

        int countBefore = failedMessages.size();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneDayAgo = now.minusDays(1);
        LocalDateTime sevenDaysAgo = now.minusDays(7);

        // 移除过期的失败消息
        failedMessages.entrySet().removeIf(entry -> {
            RetryInfo info = entry.getValue();
            // 条件1: 超过最大重试次数且最后重试时间超过1天
            boolean condition1 = info.retryCount >= ApplicationConstants.Timeout.MAX_RETRY_COUNT &&
                info.lastRetryTime.isBefore(oneDayAgo);

            // 条件2: 最后重试时间超过7天（无论重试次数）
            boolean condition2 = info.lastRetryTime.isBefore(sevenDaysAgo);

            return condition1 || condition2;
        });

        int removed = countBefore - failedMessages.size();
        if (removed > 0) {
            log.info("已清理 {} 条过期的失败消息", removed);
        }

        return removed;
    }
}
