package com.hl7.client.domain.service;

import com.hl7.client.domain.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 消息解析器工厂
 * 管理不同的解析策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageParserFactory {

    private final List<MessageParser> parsers;

    @Autowired(required = false)
    @Qualifier("deviceModelParserMap")
    private Map<String, MessageParser> deviceModelParserMap;

    /**
     * 获取适合处理消息的解析器
     * 优先根据设备型号选择解析器，如果没有匹配则根据消息类型选择
     *
     * @param message 待处理的消息
     * @return 解析器
     */
    public MessageParser getParser(Message message) {
        // 首先尝试根据设备型号获取解析器
        if (deviceModelParserMap != null &&
            message.getDeviceModel() != null &&
            deviceModelParserMap.containsKey(message.getDeviceModel())) {

            MessageParser parser = deviceModelParserMap.get(message.getDeviceModel());
            log.info("为设备型号 {} 选择了 {} 解析器", message.getDeviceModel(), parser.getType());
            return parser;
        }

        // 否则使用原有的解析器选择逻辑
        Optional<MessageParser> parser = parsers.stream()
                .filter(p -> p.supports(message))
                .findFirst();

        if (parser.isPresent()) {
            log.info("为消息类型 {} 选择了 {} 解析器", message.getMessageType(), parser.get().getType());
            return parser.get();
        } else {
            log.warn("未找到适合处理消息类型 {} 的解析器", message.getMessageType());
            throw new IllegalArgumentException("不支持的消息类型: " + message.getMessageType());
        }
    }

    /**
     * 解析消息
     *
     * @param message 待解析的消息
     * @return 解析结果
     */
    public Map<String, Object> parseMessage(Message message) {
        return getParser(message).parse(message);
    }

    public String checkMessageCompleteness(Message message) {
        return getParser(message).checkMessageCompleteness(message);
    }
}
