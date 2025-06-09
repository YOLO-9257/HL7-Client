package com.hl7.client.domain.service;

import com.hl7.client.domain.model.Message;

import java.util.Map;

/**
 * 消息解析器接口
 * 定义解析策略
 */
public interface MessageParser {

    /**
     * 解析消息
     *
     * @param message 原始消息
     * @return 解析后的数据
     */
    Map<String, Object> parse(Message message);

    /**
     * 获取解析器类型
     *
     * @return 解析器类型
     */
    String getType();

    /**
     * 判断是否支持解析该消息
     *
     * @param message 待判断的消息
     * @return 是否支持
     */
    boolean supports(Message message);

    String checkMessageCompleteness(Message message);
}
