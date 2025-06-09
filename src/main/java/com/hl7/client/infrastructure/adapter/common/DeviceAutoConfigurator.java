package com.hl7.client.infrastructure.adapter.common;

import com.hl7.client.domain.model.Device;
import com.hl7.client.infrastructure.adapter.DeviceAdapter;
import com.hl7.client.infrastructure.adapter.network.NettyServerAdapter;
import com.hl7.client.infrastructure.adapter.network.NettySocketAdapter;
import com.hl7.client.infrastructure.adapter.serial.SerialPortAdapter;
import com.hl7.client.infrastructure.config.CommunicationConfig;
import com.hl7.client.infrastructure.util.SnowflakeIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备自动配置组件
 * 用于根据配置自动创建和初始化设备
 */
@Slf4j
@Component
public class DeviceAutoConfigurator {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private CommunicationConfig config;

    /**
     * 根据配置自动创建设备适配器
     * @return 初始化好的设备适配器列表
     */
    public List<DeviceAdapter> createDeviceAdapters() {
        List<DeviceAdapter> adapters = new ArrayList<>();

        // 配置串口设备
        for (CommunicationConfig.SerialConfig serialConfig : config.getSerialPorts()) {
            if (!serialConfig.isEnabled()) {
                log.info("串口 {} 配置已禁用，跳过", serialConfig.getPortName());
                continue;
            }

            try {
                // 获取设备配置
                String deviceModel = serialConfig.getDeviceModel();
                CommunicationConfig.DeviceConfig deviceConfig = config.getDevices().get(deviceModel);

                // 创建设备对象
                Device device = createDevice(deviceModel, deviceConfig, "SERIAL");

                // 创建和初始化串口适配器
                SerialPortAdapter adapter = context.getBean(SerialPortAdapter.class);
                adapter.initializeFromConfig(serialConfig, device);

                adapters.add(adapter);
                log.info("成功创建串口设备适配器: {} ({})", device.getName(), serialConfig.getPortName());
            } catch (Exception e) {
                log.error("创建串口设备适配器时出错: {}", e.getMessage(), e);
            }
        }

        // 配置网络设备
        for (CommunicationConfig.NetworkConfig networkConfig : config.getNetworks()) {
            if (!networkConfig.isEnabled()) {
                log.info("网络 {}:{} 配置已禁用，跳过", networkConfig.getHost(), networkConfig.getPort());
                continue;
            }

            try {
                // 获取设备配置
                String deviceModel = networkConfig.getDeviceModel();
                CommunicationConfig.DeviceConfig deviceConfig = config.getDevices().get(deviceModel);

                // 创建设备对象
                Device device = createDevice(deviceModel, deviceConfig, "NETWORK");

                // 根据网络模式创建不同的适配器
                if ("SERVER".equalsIgnoreCase(networkConfig.getMode())) {
                    // 创建和初始化网络服务器适配器
                    NettyServerAdapter adapter = context.getBean(NettyServerAdapter.class);
                    adapter.initializeFromConfig(networkConfig, device);

                    adapters.add(adapter);
                    log.info("成功创建网络服务器适配器: {} ({}:{})", device.getName(),
                            networkConfig.getHost(), networkConfig.getPort());
                } else if ("CLIENT".equalsIgnoreCase(networkConfig.getMode())) {
                    // 创建和初始化网络客户端适配器
                    NettySocketAdapter adapter = context.getBean(NettySocketAdapter.class);
                    adapter.initializeFromConfig(networkConfig, device);

                    adapters.add(adapter);
                    log.info("成功创建网络客户端适配器: {} ({}:{})", device.getName(),
                            networkConfig.getHost(), networkConfig.getPort());
                } else {
                    log.warn("不支持的网络模式: {}", networkConfig.getMode());
                }
            } catch (Exception e) {
                log.error("创建网络设备适配器时出错: {}", e.getMessage(), e);
            }
        }

        return adapters;
    }

    /**
     * 创建设备对象
     * @param model 设备型号
     * @param deviceConfig 设备配置
     * @param type 设备类型
     * @return 设备对象
     */
    private Device createDevice(String model, CommunicationConfig.DeviceConfig deviceConfig, String type) {
        Device device = new Device();
        device.setId(String.valueOf(SnowflakeIdGenerator.getInstance().nextId()));
        device.setModel(model);

        // 使用配置的设备名称或型号作为默认值
        String deviceName = (deviceConfig != null && deviceConfig.getName() != null) ?
                deviceConfig.getName() : model;
        device.setName(deviceName);

        // 设置设备描述
        String description = (deviceConfig != null && deviceConfig.getDescription() != null) ?
                deviceConfig.getDescription() : (type + " " + model);
        device.setDescription(description);

        // 设置消息类型
        if (deviceConfig != null && deviceConfig.getMessageType() != null) {
            device.setMessageType(deviceConfig.getMessageType());
        }

        return device;
    }
}
