package com.hl7.client.domain.model;

/**
 * 设备状态枚举
 */
public enum DeviceStatus {
    /** 已连接 */
    CONNECTED,

    /** 未连接 */
    DISCONNECTED,

    /** 错误状态 */
    ERROR;

    /**
     * 将字符串转换为状态枚举
     *
     * @param status 状态字符串
     * @return 状态枚举
     */
    public static DeviceStatus fromString(String status) {
        if (status == null) {
            return DISCONNECTED;
        }

        try {
            return DeviceStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return DISCONNECTED;
        }
    }
}
