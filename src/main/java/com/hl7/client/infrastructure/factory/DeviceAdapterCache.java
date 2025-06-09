package com.hl7.client.infrastructure.factory;

import com.hl7.client.domain.model.Device;
import com.hl7.client.infrastructure.adapter.DeviceAdapter;
import com.hl7.client.infrastructure.event.DeviceStatusChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备适配器缓存
 * 用于缓存和管理设备适配器实例，避免重复创建
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceAdapterCache implements ApplicationListener<DeviceStatusChangeEvent> {

    private final DeviceAdapterFactory adapterFactory;

    // 使用设备ID作为缓存键
    private final Map<String, DeviceAdapter> adapterCache = new ConcurrentHashMap<>();

    /**
     * 获取设备适配器实例
     * 如果缓存中不存在，则创建新实例并缓存
     *
     * @param device 设备信息
     * @return 适配器实例
     */
    public DeviceAdapter getAdapter(Device device) {
        if (device == null || device.getId() == null) {
            throw new IllegalArgumentException("设备ID不能为空");
        }

        return adapterCache.computeIfAbsent(device.getId(), id -> {
            log.info("为设备 {} 创建新的适配器实例", device.getName());
            return adapterFactory.createAdapter(device);
        });
    }

    /**
     * 清除设备适配器缓存
     *
     * @param deviceId 设备ID
     */
    public void clearAdapter(String deviceId) {
        if (deviceId != null) {
            DeviceAdapter removed = adapterCache.remove(deviceId);
            if (removed != null) {
                try {
                    // 确保断开连接
                    if (removed.isConnected()) {
                        removed.disconnect();
                    }
                    log.info("已移除设备 {} 的适配器缓存", deviceId);
                } catch (Exception e) {
                    log.error("移除设备 {} 适配器缓存时断开连接失败: {}", deviceId, e.getMessage());
                }
            }
        }
    }

    /**
     * 清除所有适配器缓存
     */
    public void clearAllAdapters() {
        for (Map.Entry<String, DeviceAdapter> entry : adapterCache.entrySet()) {
            try {
                DeviceAdapter adapter = entry.getValue();
                if (adapter.isConnected()) {
                    adapter.disconnect();
                }
            } catch (Exception e) {
                log.error("断开设备 {} 连接时出错: {}", entry.getKey(), e.getMessage());
            }
        }
        adapterCache.clear();
        log.info("已清除所有设备适配器缓存");
    }

    /**
     * 刷新设备适配器
     * 当设备信息变更时调用此方法更新适配器
     *
     * @param device 设备信息
     */
    public void refreshAdapter(Device device) {
        if (device == null || device.getId() == null) {
            return;
        }

        // 检查缓存中是否已存在
        DeviceAdapter existingAdapter = adapterCache.get(device.getId());

        // 如果存在且状态为已断开连接，重新初始化
        if (existingAdapter != null) {
            // 检查设备配置是否有变更
            Device cachedDevice = existingAdapter.getDevice();

            // 检查关键配置项是否有变更
            boolean configChanged = !cachedDevice.getConnectionType().equals(device.getConnectionType()) ||
                                   !cachedDevice.getConnectionParams().equals(device.getConnectionParams());

            if (configChanged) {
                clearAdapter(device.getId());
                log.info("设备 {} 配置已变更，重新创建适配器", device.getName());
                getAdapter(device); // 重新创建并缓存
            } else {
                // 只更新设备信息
                existingAdapter.initialize(device);
                log.debug("设备 {} 适配器已刷新", device.getName());
            }
        }
    }

    /**
     * 处理设备状态变化事件
     * 用于在设备断开连接或配置变更时更新适配器缓存
     *
     * @param event 设备状态变化事件
     */
    @Override
    public void onApplicationEvent(DeviceStatusChangeEvent event) {
        try {
            Device device = event.getDevice();
            String oldStatus = event.getOldStatus().name();
            String newStatus = event.getNewStatus().name();

            // 如果状态没有实质性变化，则忽略此事件
            if (!event.isStatusChanged()) {
                log.debug("[onApplicationEvent] - 设备 {} 状态无变化，忽略此事件", device.getName());
                return;
            }

            log.info("[onApplicationEvent] - 收到设备状态变化事件: 设备 {} 状态从 {} 变为 {}, 验证状态: {}",
                    device.getName(), oldStatus, newStatus, event.isVerified());

            // 处理连接状态变更
            if (event.isConnected()) {
                // 设备连接成功时，将其加入缓存
                DeviceAdapter adapter = adapterFactory.createAdapter(device);
                adapterCache.put(device.getId(), adapter);
                log.info("[onApplicationEvent] - 设备 {} 已加入设备缓存", device.getName());
            } else if (event.isDisconnected()) {
                // 对于服务器模式设备，需要特殊处理断开连接事件
                if (isNetworkServerMode(device)) {
                    // 检查服务器是否仍在运行
                    DeviceAdapter adapter = adapterCache.get(device.getId());
                    if (adapter != null && adapter.isConnected()) {
                        log.info("[onApplicationEvent] - 设备 {} 为服务器模式且服务器仍在运行，忽略断开连接事件", device.getName());
                        return; // 忽略此断开连接事件
                    }
                    log.info("[onApplicationEvent] - 设备 {} 为服务器模式且已确认断开连接", device.getName());
                }

                // 设备断开连接时，将其从缓存中移除
                clearAdapter(device.getId());
                log.info("[onApplicationEvent] - 设备 {} 已从适配器缓存中移除", device.getName());
            }
        } catch (Exception e) {
            log.error("[onApplicationEvent] - 处理设备状态变化事件时发生异常", e);
        }
    }

    /**
     * 检查设备是否为网络服务器模式
     *
     * @param device 设备对象
     * @return 是否为服务器模式
     */
    private boolean isNetworkServerMode(Device device) {
        if (device == null || device.getConnectionType() == null) {
            return false;
        }

        // 必须是网络类型设备
        if (!"NETWORK".equalsIgnoreCase(device.getConnectionType())) {
            return false;
        }

        String params = device.getConnectionParams();
        if (params == null || params.isEmpty()) {
            return false;
        }

        // 更全面的服务器模式检测逻辑
        String[] parts = params.split(":");

        // 显式标记为SERVER模式
        if (parts.length >= 4 && "SERVER".equalsIgnoreCase(parts[3])) {
            log.debug("设备 {} 通过mode参数识别为服务器模式", device.getName());
            return true;
        }

        if (parts.length >= 3 && "SERVER".equalsIgnoreCase(parts[2])) {
            log.debug("设备 {} 通过mode参数识别为服务器模式", device.getName());
            return true;
        }

        // 参数格式为 port:protocol 的服务器模式 (没有主机名)
        if (parts.length == 2) {
            try {
                // 尝试解析第一个参数为端口号
                Integer.parseInt(parts[0]);
                log.debug("设备 {} 通过端口参数模式识别为服务器模式", device.getName());
                return true;
            } catch (NumberFormatException e) {
                // 不是端口号，可能是主机名，因此是客户端模式
            }
        }

        // 参数中显式包含server关键字
        if (params.toLowerCase().contains("server")) {
            log.debug("设备 {} 通过server关键字识别为服务器模式", device.getName());
            return true;
        }

        return false;
    }

    /**
     * 检查服务器是否仍在运行
     *
     * @param device 设备对象
     * @return 服务器是否仍在运行
     */
    private boolean isServerStillRunning(Device device) {
        try {
            // 检查服务器的适配器是否仍在连接状态
            DeviceAdapter adapter = adapterCache.get(device.getId());
            if (adapter != null) {
                return adapter.isConnected();
            }
            return false;
        } catch (Exception e) {
            log.error("[isServerStillRunning] - 检查服务器状态时发生异常", e);
            return false;
        }
    }
}
