package com.hl7.client.infrastructure.adapter.network.message;

import com.hl7.client.domain.model.Message;
import com.hl7.client.domain.service.MessageParserFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 默认消息完整性检查策略
 * 适用于大多数设备的通用实现
 */
@Component
@RequiredArgsConstructor
public class DefaultMessageCompletionStrategy implements MessageCompletionStrategy {

    private final MessageParserFactory messageParserFactory;

    @Override
    public String isMessageComplete(Message message) {
        // 使用现有的解析工厂检查消息完整性
        return messageParserFactory.checkMessageCompleteness(message);
    }

    @Override
    public boolean supports(String deviceModel) {
        // 默认策略作为兜底策略，支持所有没有特定策略的设备型号
        return true;
    }
}
