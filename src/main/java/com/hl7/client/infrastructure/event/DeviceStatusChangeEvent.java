package com.hl7.client.infrastructure.event;

import com.hl7.client.domain.model.Device;
import com.hl7.client.domain.model.DeviceStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 设备状态变化事件
 * 当设备连接状态变化时触发，用于通知UI界面更新
 */
@Getter
public class DeviceStatusChangeEvent extends ApplicationEvent {

    /**
     * -- GETTER --
     *  获取设备对象
     *
     * @return 设备对象
     */
    private final Device device;
    /**
     * -- GETTER --
     *  获取旧状态
     *
     * @return 旧状态
     */
    private final DeviceStatus oldStatus;
    /**
     * -- GETTER --
     *  获取新状态
     *
     * @return 新状态
     */
    private final DeviceStatus newStatus;
    private final boolean verified; // 标记此状态变化是否经过验证

    /**
     * 创建设备状态变化事件
     *
     * @param source 事件源
     * @param device 设备对象
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     */
    public DeviceStatusChangeEvent(Object source, Device device, DeviceStatus oldStatus, DeviceStatus newStatus) {
        this(source, device, oldStatus, newStatus, true);
    }

    /**
     * 创建设备状态变化事件
     *
     * @param source 事件源
     * @param device 设备
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     * @param verified 状态变化是否经过验证
     */
    public DeviceStatusChangeEvent(Object source, Device device, DeviceStatus oldStatus, DeviceStatus newStatus, boolean verified) {
        super(source);
        this.device = device;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.verified = verified;
    }

    /**
     * 判断此事件是否表示设备已连接
     *
     * @return 设备是否已连接
     */
    public boolean isConnected() {
        return DeviceStatus.CONNECTED == newStatus;
    }

    /**
     * 判断此事件是否表示设备已断开连接
     *
     * @return 设备是否已断开连接
     */
    public boolean isDisconnected() {
        return DeviceStatus.DISCONNECTED == newStatus;
    }

    /**
     * 判断此事件是否表示设备状态发生了实质性变化
     *
     * @return 设备状态是否发生了实质性变化
     */
    public boolean isStatusChanged() {
        return oldStatus == null || oldStatus != newStatus;
    }
}
