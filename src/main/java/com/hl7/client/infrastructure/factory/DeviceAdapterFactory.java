package com.hl7.client.infrastructure.factory;

import com.hl7.client.domain.model.Device;
import com.hl7.client.infrastructure.adapter.DeviceAdapter;
import com.hl7.client.infrastructure.adapter.file.FileAdapter;
import com.hl7.client.infrastructure.adapter.network.NettyServerAdapter;
import com.hl7.client.infrastructure.adapter.network.NettySocketAdapter;
import com.hl7.client.infrastructure.adapter.network.NetworkMode;
import com.hl7.client.infrastructure.adapter.serial.SerialPortAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 设备适配器工厂
 * 根据设备类型创建对应的适配器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceAdapterFactory {

    private final NettySocketAdapter nettySocketAdapter;
    private final NettyServerAdapter nettyServerAdapter;
    private final SerialPortAdapter serialPortAdapter;
    private final FileAdapter fileAdapter;

    /**
     * 创建设备适配器
     *
     * @param device 设备信息
     * @return 适配器实例
     */
    public DeviceAdapter createAdapter(Device device) {
        String connectionType = device.getConnectionType();
        DeviceAdapter adapter;

        switch (connectionType.toUpperCase()) {
            case "NETWORK":
                // 根据连接参数判断是客户端模式还是服务器模式
                adapter = determineNetworkAdapter(device);
                break;
            case "SERIAL":
                adapter = serialPortAdapter;
                break;
            case "FILE":
                adapter = fileAdapter;
                break;
            default:
                log.error("不支持的连接类型: {}", connectionType);
                throw new IllegalArgumentException("不支持的连接类型: " + connectionType);
        }

        // 初始化适配器
        adapter.initialize(device);
        log.info("为设备 {} 创建了 {} 类型的适配器", device.getName(), connectionType);

        return adapter;
    }

    /**
     * 根据连接参数确定网络适配器类型
     *
     * @param device 设备信息
     * @return 网络适配器
     */
    private DeviceAdapter determineNetworkAdapter(Device device) {
        String params = device.getConnectionParams();
        NetworkMode mode = NetworkMode.CLIENT; // 默认为客户端模式

        // 解析模式
        String[] parts = params.split(":");
        if (parts.length >= 4) {
            // 可能的格式：host:port:protocol:mode
            try {
                mode = NetworkMode.fromString(parts[3]);
            } catch (Exception e) {
                log.warn("无法解析网络模式，使用默认客户端模式: {}", e.getMessage());
            }
        } else if (parts.length == 3) {
            // 可能的格式：port:protocol:mode
            try {
                mode = NetworkMode.fromString(parts[2]);
            } catch (Exception e) {
                log.warn("无法解析网络模式，使用默认客户端模式: {}", e.getMessage());
            }
        }

        log.info("设备 {} 使用网络模式: {}", device.getName(), mode);

        // 根据模式返回不同的适配器
        return mode == NetworkMode.SERVER ? nettyServerAdapter : nettySocketAdapter;
    }
}
