package com.hl7.client.infrastructure.adapter.network.message;

import com.hl7.client.domain.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * BG800设备的消息完整性检查策略
 * 专门用于处理BG800设备的消息完整性检查
 */
@Component
@Slf4j
public class BG800MessageCompletionStrategy implements MessageCompletionStrategy {

    private static final String DEVICE_MODEL = "BG800";

    // BG800特有的消息结束标记
    private static final String BG800_MESSAGE_END = "\u001c\r";

    // ASTM消息格式的帧头
    private static final String ASTM_FRAME_START = "\u0002";

    // ASTM消息格式的帧尾
    private static final String ASTM_FRAME_END = "\u0003";

    // 消息响应
    private static final String ACK_RESPONSE = "\u0006";

    // 检测BG800特定消息格式的正则表达式
    private static final Pattern BG800_PATTERN = Pattern.compile(".*\u0003[0-9A-F]{2}\r", Pattern.DOTALL);

    @Override
    public String isMessageComplete(Message message) {
        if (message == null || message.getRawContent() == null || message.getRawContent().isEmpty()) {
            log.warn("消息为空，不完整");
            return null;
        }

        String content = message.getRawContent();

        // 检查是否是BG800特有的消息格式
        if (content.contains(ASTM_FRAME_START) && content.contains(ASTM_FRAME_END)) {
            // 检查是否是完整的ASTM帧
            if (BG800_PATTERN.matcher(content).matches()) {
                log.debug("BG800消息完整，发送ACK响应");
                return ACK_RESPONSE;
            } else {
                log.debug("BG800消息不完整，等待更多数据");
                return null;
            }
        }

        // 检查是否是ENQ请求
        if (content.equals("\u0005")) {
            log.debug("接收到ENQ请求，发送ACK响应");
            return ACK_RESPONSE;
        }

        // 检查是否是EOT结束
        if (content.endsWith("\u0004")) {
            log.debug("接收到EOT结束，消息传输完成");
            return null;
        }

        // 如果不是BG800特有的格式，使用通用的检查逻辑
        if (content.endsWith("\r") || content.endsWith("\r\n") || content.endsWith(BG800_MESSAGE_END)) {
            log.debug("消息以标准结束符结尾，完整");
            return null;
        }

        log.debug("消息不完整，等待更多数据");
        return null;
    }

    @Override
    public boolean supports(String deviceModel) {
        return DEVICE_MODEL.equalsIgnoreCase(deviceModel);
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
