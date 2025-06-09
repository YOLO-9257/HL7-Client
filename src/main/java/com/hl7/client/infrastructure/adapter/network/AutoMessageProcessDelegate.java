package com.hl7.client.infrastructure.adapter.network;

import com.hl7.client.application.MessageProcessor;
import com.hl7.client.domain.model.Device;
import com.hl7.client.domain.model.Message;
import com.hl7.client.domain.model.MessageStatus;
import com.hl7.client.domain.service.MessageParserFactory;
import com.hl7.client.domain.service.MessageProcessService;
import com.hl7.client.infrastructure.adapter.network.exception.MessageHandlingException;
import com.hl7.client.infrastructure.adapter.network.message.MessageCompletionStrategyManager;
import com.hl7.client.infrastructure.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * 自动消息处理委托实现
 * 负责自动解析和转发消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoMessageProcessDelegate implements MessageHandlerDelegate {

    private final MessageProcessService messageProcessService;
    private final MessageParserFactory messageParserFactory;
    private final MessageCompletionStrategyManager strategyManager;

    @Autowired
    @Lazy
    private MessageProcessor messageProcessor;

    // 消息类型常量
    private static final String MESSAGE_TYPE_UNKNOWN = "UNKNOWN";
    private static final String MESSAGE_TYPE_HL7 = "HL7";
    private static final String MESSAGE_TYPE_HL7_MLLP = "HL7_MLLP";
    private static final String MESSAGE_TYPE_JSON = "JSON";
    private static final String MESSAGE_TYPE_XML = "XML";
    private static final String MESSAGE_TYPE_TEXT = "TEXT";

    // 用于JSON检测的正则表达式
    private static final Pattern JSON_PATTERN = Pattern.compile("^\\s*[{\\[].*[}\\]]\\s*$", Pattern.DOTALL);
    // 用于XML检测的正则表达式
    private static final Pattern XML_PATTERN = Pattern.compile("^\\s*<[^>]+>.*</[^>]+>\\s*$", Pattern.DOTALL);


    /**
     * 检测消息类型
     */
    private String detectMessageType(String content) {
        if (content == null || content.isEmpty()) {
            return MESSAGE_TYPE_UNKNOWN;
        }

        // 检测HL7消息
        if (content.startsWith("MSH|") || content.contains("\rMSH|")) {
            return MESSAGE_TYPE_HL7;
        }

        // 检测HL7 MLLP消息
        if (content.contains("\u000b") && content.contains("\u001c")) {
            return MESSAGE_TYPE_HL7_MLLP;
        }

        // 检测JSON格式
        if (JSON_PATTERN.matcher(content).matches()) {
            return MESSAGE_TYPE_JSON;
        }

        // 检测XML格式
        if (XML_PATTERN.matcher(content).matches()) {
            return MESSAGE_TYPE_XML;
        }

        // 默认为文本
        return MESSAGE_TYPE_TEXT;
    }

    @Override
    public boolean processMessage(Device device, String rawMessage) {
        if (device == null) {
            log.error("设备为null，无法处理消息");
            return false;
        }

        try {
            log.info("自动处理来自设备 {} 的消息", device.getName());

            // 创建消息对象
            Message message = Message.builder()
                    .id(String.valueOf(SnowflakeIdGenerator.getInstance().nextId()))
                    .deviceId(device.getId())
                    .deviceModel(device.getModel())
                    .rawContent(rawMessage)
                    .messageType(detectMessageType(rawMessage))
                    .receivedTime(LocalDateTime.now())
                    .status(MessageStatus.NEW.name())
                    .build();

            // 通过MessageProcessor处理消息，确保它被正确添加到队列和处理后的消息集合中
            CompletableFuture.runAsync(() -> {
                try {
                    // 使用MessageProcessor的processMessageImmediately方法
                    // 这样就能确保消息被正确处理并添加到processedMessages集合
                    messageProcessor.processMessageImmediately(message);
                    log.info("消息 {} 已通过MessageProcessor自动处理", message.getId());
                } catch (Exception e) {
                    log.error("通过MessageProcessor自动处理消息过程中发生异常: {}", e.getMessage(), e);
                }
            });

            return true;
        } catch (Exception e) {
            log.error("设备 {} 消息处理失败: {}", device.getName(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String isMessageComplete(Message message) {
        try {
            if (message == null) {
                log.warn("消息为null，不完整");
                return null;
            }

            // 使用策略管理器获取适用的策略
            String deviceModel = message.getDeviceModel();

            // 使用策略管理器获取适用的策略并执行检查
            log.debug("使用设备型号 {} 的专用策略检查消息完整性", deviceModel);
            return strategyManager.getStrategy(deviceModel).isMessageComplete(message);

        } catch (Exception e) {
            log.error("检查消息完整性时发生异常: {}", e.getMessage(), e);
            throw new MessageHandlingException("检查消息完整性失败", e);
        }
    }
}
