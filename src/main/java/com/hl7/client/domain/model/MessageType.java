package com.hl7.client.domain.model;

/**
 * 消息类型枚举
 */
public enum MessageType {

    HL7,

    CUSTOM,

    ETC,
    ;

    /**
     * 将字符串转换为状态枚举
     *
     * @param status 状态字符串
     * @return 状态枚举
     */
    public static MessageType fromString(String status) {
        if (status == null) {
            return CUSTOM;
        }

        try {
            return MessageType.valueOf(status);
        } catch (IllegalArgumentException e) {
            return CUSTOM;
        }
    }
}
