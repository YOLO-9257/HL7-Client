package com.hl7.client.infrastructure.adapter;

import com.hl7.client.domain.model.Device;

/**
 * 设备适配器接口
 * 不同类型设备连接的基础接口
 */
public interface DeviceAdapter {
    /**
     * 初始化适配器
     *
     * @param device 设备信息
     */
    void initialize(Device device);

    /**
     * 连接设备
     *
     * @return 连接是否成功
     */
    boolean connect();

    /**
     * 断开连接
     */
    void disconnect();

    /**
     * 发送数据
     *
     * @param data 要发送的数据
     * @return 发送是否成功
     */
    boolean send(String data);

    /**
     * 接收数据
     *
     * @return 接收到的数据
     */
    String receive();

    /**
     * 检查连接状态
     *
     * @return 是否连接
     */
    boolean isConnected();

    /**
     * 获取设备信息
     *
     * @return 设备信息
     */
    Device getDevice();

    /**
     * 处理接收到的原始数据
     * @param rawData 接收到的原始数据字符串
     * @return 如果需要响应，返回响应内容；否则返回null
     */
    String processReceivedData(String rawData);
}
