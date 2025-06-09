package com.hl7.client.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 医疗设备领域模型
 * 描述与系统交互的医疗设备信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Device {
    /** 设备唯一标识 */
    private String id;

    /** 设备名称 */
    private String name;

    /** 设备型号 */
    private String model;

    /** 设备厂商 */
    private String manufacturer;

    /** 连接类型 (SERIAL, NETWORK, FILE, DATABASE) */
    private String connectionType;

    /** 设备状态 */
    private DeviceStatus status = DeviceStatus.DISCONNECTED;

    /** 连接参数 (JSON格式存储具体连接信息) */
    private String connectionParams;

    /** 设备描述 */
    private String description;

    /** 消息类型 */
    private String messageType;
}
