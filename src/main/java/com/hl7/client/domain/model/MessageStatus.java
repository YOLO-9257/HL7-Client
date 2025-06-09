package com.hl7.client.domain.model;

import lombok.Getter;

/**
 * 消息状态枚举
 * 定义消息在处理过程中的各种状态
 */
@Getter
public enum MessageStatus {
    /**
     * 新消息，尚未开始处理
     */
    NEW("新消息"),

    /**
     * 不完整的消息，需要等待更多数据
     */
    INCOMPLETE("不完整"),

    /**
     * 处理中的消息
     */
    PROCESSING("处理中"),

    /**
     * 已处理完成的消息
     */
    PROCESSED("已处理"),

    /**
     * 处理失败的消息
     */
    ERROR("错误");

    private final String description;

    MessageStatus(String description) {
        this.description = description;
    }

}
