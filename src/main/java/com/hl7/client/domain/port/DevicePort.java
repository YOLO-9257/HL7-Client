package com.hl7.client.domain.port;

import com.hl7.client.domain.model.Device;
import com.hl7.client.domain.model.Message;

/**
 * 设备端口接口 - 领域核心端口
 * 定义领域核心与外部适配器的交互接口
 */
public interface DevicePort {
    /**
     * 连接设备
     *
     * @param device 设备信息
     * @return 连接是否成功
     */
    boolean connect(Device device);

    /**
     * 断开设备连接
     *
     * @param device 设备信息
     */
    void disconnect(Device device);

    /**
     * 发送消息到设备
     *
     * @param device 设备信息
     * @param message 要发送的消息
     * @return 发送是否成功
     */
    boolean sendMessage(Device device, String message);

    /**
     * 接收来自设备的消息
     *
     * @param device 设备信息
     * @return 接收到的消息
     */
    Message receiveMessage(Device device);

    /**
     * 检查设备连接状态
     *
     * @param device 设备信息
     * @return 设备是否连接
     */
    boolean isConnected(Device device);
}
