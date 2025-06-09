package com.hl7.client.domain.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息领域模型
 * 描述从设备接收的数据消息
 */
@Data
@Builder
public class Message {
    /** 消息ID */
    private String id;

    /** 关联设备ID */
    private String deviceId;

    /** 关联设备型号 */
    private String deviceModel;

    /**
     * 关联设备处理器
     */
    private String messageProcessor;

    /** 原始消息内容 */
    private String rawContent;

    /** 消息类型 (HL7, CUSTOM, etc.) */
    private String messageType;

    /** 消息接收时间 */
    private LocalDateTime receivedTime;

    /** 处理状态 (NEW,INCOMPLETE, PROCESSING, PROCESSED, ERROR) */
    private String status;

    /**
     * 处理结果
     */
    private String processResult;

    /**
     * 错误信息
     * 当消息处理失败时记录详细错误信息
     */
    private String errorMessage;
}
