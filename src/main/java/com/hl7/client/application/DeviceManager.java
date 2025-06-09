package com.hl7.client.application;

import com.hl7.client.domain.model.Device;
import com.hl7.client.domain.model.DeviceStatus;
import com.hl7.client.domain.service.DeviceService;
import com.hl7.client.infrastructure.event.DeviceStatusChangeEvent;
import com.hl7.client.infrastructure.persist.ConfigurationService;
import com.hl7.client.infrastructure.util.SnowflakeIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 设备管理器
 * 负责设备生命周期管理，包括注册、连接、断开、状态监控等功能
 */
@Slf4j
@Component
public class DeviceManager {

    // 常量定义
    private static final long STATUS_CHANGE_DEBOUNCE_MS = 3000L; // 状态变更防抖时间（毫秒）
    private static final int STATUS_VERIFICATION_COUNT = 3; // 状态验证次数
    private static final int STATUS_CHANGE_CONFIRMATION_THRESHOLD = 2; // 状态变更确认阈值
    private static final long STATUS_CHECK_DELAY_MS = 200L; // 状态检查间隔时间（毫秒）
    private static final double SERVER_MODE_DISCONNECT_RATIO = 2.0/3.0; // 服务器模式断开判定比例
    private static final double CLIENT_MODE_CONNECT_RATIO = 2.0/3.0; // 客户端模式连接判定比例

    // 连接类型常量
    private static final String CONNECTION_TYPE_NETWORK = "NETWORK";
    private static final String SERVER_MODE = "SERVER";

    private final DeviceService deviceService;
    private final ConfigurationService configurationService;
    private final ApplicationEventPublisher eventPublisher;

    // 设备存储
    private final Map<String, Device> devices = new ConcurrentHashMap<>();

    // 设备状态变更防抖机制
    private final Map<String, Long> lastStatusChangeTime = new ConcurrentHashMap<>();

    // 状态变更确认次数计数器
    private final Map<String, Integer> statusChangeConfirmations = new ConcurrentHashMap<>();

    @Autowired
    public DeviceManager(DeviceService deviceService,
                         ConfigurationService configurationService,
                         ApplicationEventPublisher eventPublisher) {
        this.deviceService = deviceService;
        this.configurationService = configurationService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 初始化方法，加载已保存的设备配置
     */
    @PostConstruct
    public void init() {
        try {
            loadDevices();
            log.info("设备管理器初始化完成");
        } catch (Exception e) {
            log.error("设备管理器初始化失败", e);
        }
    }

    /**
     * 加载保存的设备配置
     */
    public void loadDevices() {
        try {
            List<Device> deviceList = configurationService.loadDevices();
            devices.clear();

            for (Device device : deviceList) {
                if (!StringUtils.hasText(device.getId())) {
                    device.setId(String.valueOf(SnowflakeIdGenerator.getInstance().nextId()));
                }
                devices.put(device.getId(), device);
            }

            log.info("已加载 {} 个设备配置", devices.size());
            connectAllDevices();
        } catch (Exception e) {
            log.error("加载设备配置失败", e);
        }
    }

    /**
     * 保存设备配置
     */
    public void saveDevices() {
        try {
            List<Device> deviceList = new ArrayList<>(devices.values());
            configurationService.saveDevices(deviceList);
            log.info("已保存 {} 个设备配置", deviceList.size());
        } catch (Exception e) {
            log.error("保存设备配置失败", e);
        }
    }

    /**
     * 注册设备
     *
     * @param device 设备信息
     * @return 注册后的设备
     * @throws IllegalArgumentException 当设备信息无效时抛出
     */
    public Device registerDevice(Device device) {
        if (device == null) {
            throw new IllegalArgumentException("设备信息不能为空");
        }

        if (!StringUtils.hasText(device.getName())) {
            throw new IllegalArgumentException("设备名称不能为空");
        }

        log.info("注册设备: {}", device.getName());

        try {
            // 如果设备ID为空，生成一个新的ID
            if (!StringUtils.hasText(device.getId())) {
                device.setId(String.valueOf(SnowflakeIdGenerator.getInstance().nextId()));
            }

            // 检查设备是否已存在
            if (devices.containsKey(device.getId())) {
                log.warn("设备 {} 已存在，更新设备信息", device.getName());
            }

            // 存储设备信息
            devices.put(device.getId(), device);

            // 保存配置
            saveDevices();

            log.info("设备 {} 注册成功，ID: {}", device.getName(), device.getId());
            return device;
        } catch (Exception e) {
            log.error("注册设备 {} 失败", device.getName(), e);
            throw new RuntimeException("注册设备失败", e);
        }
    }

    /**
     * 移除设备
     *
     * @param deviceId 设备ID
     * @return 操作是否成功
     */
    public boolean removeDevice(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            log.warn("设备ID不能为空");
            return false;
        }

        Device device = devices.get(deviceId);
        if (device == null) {
            log.warn("设备 {} 不存在，无法移除", deviceId);
            return false;
        }

        try {
            // 先断开连接
            if (DeviceStatus.CONNECTED == device.getStatus()) {
                deviceService.disconnect(device);
            }

            // 移除设备
            devices.remove(deviceId);
            // 清除状态变更记录
            cleanupDeviceStateRecords(deviceId);

            log.info("设备 {} 已移除", device.getName());

            // 保存配置
            saveDevices();

            return true;
        } catch (Exception e) {
            log.error("移除设备 {} 失败", device.getName(), e);
            return false;
        }
    }

    /**
     * 连接设备
     *
     * @param deviceId 设备ID
     * @return 操作是否成功
     */
    public boolean connectDevice(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            log.warn("设备ID不能为空");
            return false;
        }

        Device device = devices.get(deviceId);
        if (device == null) {
            log.warn("设备 {} 不存在，无法连接", deviceId);
            return false;
        }

        return connectSingleDevice(device);
    }

    /**
     * 连接所有设备
     */
    public void connectAllDevices() {
        Collection<Device> deviceCollection = devices.values();
        log.info("开始连接所有设备，共 {} 个设备", deviceCollection.size());

        int successCount = 0;
        int failureCount = 0;

        for (Device device : deviceCollection) {
            try {
                boolean connected = connectSingleDevice(device);
                if (connected) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                log.error("连接设备 {} 时发生异常", device.getName(), e);
                failureCount++;
            }
        }

        log.info("批量连接设备完成，成功: {} 个，失败: {} 个", successCount, failureCount);
    }

    /**
     * 连接单个设备的具体实现
     *
     * @param device 设备对象
     * @return 连接是否成功
     */
    private boolean connectSingleDevice(Device device) {
        // 记录旧状态
        DeviceStatus oldStatus = device.getStatus();

        try {
            // 连接设备
            boolean connected = deviceService.connect(device);
            log.info("设备 {} 连接{}", device.getName(), connected ? "成功" : "失败");

            // 更新设备状态并保存
            if (connected) {
                updateDeviceStatus(device, DeviceStatus.CONNECTED, oldStatus);
            }

            return connected;
        } catch (Exception e) {
            log.error("连接设备 {} 时发生异常", device.getName(), e);
            return false;
        }
    }

    /**
     * 断开设备连接
     *
     * @param deviceId 设备ID
     */
    public void disconnectDevice(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            log.warn("设备ID不能为空");
            return;
        }

        Device device = devices.get(deviceId);
        if (device == null) {
            log.warn("设备 {} 不存在，无法断开连接", deviceId);
            return;
        }

        // 记录旧状态
        DeviceStatus oldStatus = device.getStatus();

        try {
            // 断开设备连接
            deviceService.disconnect(device);

            // 更新设备状态并保存
            updateDeviceStatus(device, DeviceStatus.DISCONNECTED, oldStatus);

            log.info("设备 {} 已断开连接", device.getName());
        } catch (Exception e) {
            log.error("断开设备 {} 连接时发生异常", device.getName(), e);
        }
    }

    /**
     * 获取设备信息
     *
     * @param deviceId 设备ID
     * @return 设备信息
     */
    public Device getDevice(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return null;
        }
        return devices.get(deviceId);
    }

    /**
     * 获取所有设备
     *
     * @return 所有设备的只读视图
     */
    public Map<String, Device> getAllDevices() {
        return new ConcurrentHashMap<>(devices);
    }

    /**
     * 检查设备连接状态
     *
     * @param deviceId 设备ID
     * @return 是否连接
     */
    public boolean checkDeviceStatus(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return false;
        }

        Device device = devices.get(deviceId);
        if (device == null) {
            log.warn("设备 {} 不存在", deviceId);
            return false;
        }

        // 检查是否在防抖期内
        if (isInDebounceWindow(deviceId)) {
            log.debug("设备 {} 在状态变更防抖期内，跳过状态检查", device.getName());
            return DeviceStatus.CONNECTED == device.getStatus();
        }

        try {
            // 执行状态验证 - 多次检查以确保状态稳定
            boolean connected = verifyConnectionStatus(device);

            // 处理状态变更
            handleStatusChange(device, connected);

            return connected;
        } catch (Exception e) {
            log.error("检查设备 {} 状态时发生异常", device.getName(), e);
            return false;
        }
    }

    /**
     * 检查是否在防抖窗口期内
     *
     * @param deviceId 设备ID
     * @return 是否在防抖期内
     */
    private boolean isInDebounceWindow(String deviceId) {
        Long lastChangeTime = lastStatusChangeTime.get(deviceId);
        long currentTime = System.currentTimeMillis();

        return lastChangeTime != null &&
            (currentTime - lastChangeTime) < STATUS_CHANGE_DEBOUNCE_MS;
    }

    /**
     * 处理状态变更逻辑
     *
     * @param device 设备对象
     * @param connected 当前检测到的连接状态
     */
    private void handleStatusChange(Device device, boolean connected) {
        String deviceId = device.getId();
        DeviceStatus oldStatus = device.getStatus();
        DeviceStatus newStatus = connected ? DeviceStatus.CONNECTED : DeviceStatus.DISCONNECTED;

        // 如果状态发生变化，进行确认计数
        if (oldStatus == null || !oldStatus.equals(newStatus)) {
            // 获取当前确认计数
            Integer confirmCount = statusChangeConfirmations.getOrDefault(deviceId, 0);

            // 状态变更与前一次检测相同，确认计数+1
            statusChangeConfirmations.put(deviceId, confirmCount + 1);

            // 只有在达到确认阈值后，才真正变更状态
            if (confirmCount + 1 >= STATUS_CHANGE_CONFIRMATION_THRESHOLD) {
                // 重置确认计数
                statusChangeConfirmations.put(deviceId, 0);

                // 更新设备状态
                updateDeviceStatus(device, newStatus, oldStatus);

                log.info("设备 {} 状态从 {} 变为 {} (已确认 {} 次)",
                    device.getName(), oldStatus, newStatus, STATUS_CHANGE_CONFIRMATION_THRESHOLD);
            } else {
                log.debug("设备 {} 状态从 {} 变为 {} 的变更正在确认中 ({}/{})",
                    device.getName(), oldStatus, newStatus, confirmCount + 1, STATUS_CHANGE_CONFIRMATION_THRESHOLD);
            }
        } else {
            // 状态没有变化，重置确认计数
            statusChangeConfirmations.put(deviceId, 0);
        }
    }

    /**
     * 更新设备状态并发布事件
     *
     * @param device 设备对象
     * @param newStatus 新状态
     * @param oldStatus 旧状态
     */
    private void updateDeviceStatus(Device device, DeviceStatus newStatus, DeviceStatus oldStatus) {
        // 设置新状态
        device.setStatus(newStatus);

        // 保存配置
        saveDevices();

        // 记录状态变更时间
        lastStatusChangeTime.put(device.getId(), System.currentTimeMillis());

        // 发布设备状态变化事件
        if (!newStatus.equals(oldStatus)) {
            eventPublisher.publishEvent(new DeviceStatusChangeEvent(
                this, device, oldStatus, newStatus));
        }
    }

    /**
     * 验证设备连接状态
     * 多次检查确保状态稳定
     *
     * @param device 设备对象
     * @return 是否连接
     */
    private boolean verifyConnectionStatus(Device device) {
        // 首先判断当前状态
        DeviceStatus currentStatus = device.getStatus();
        boolean isCurrentlyConnected = DeviceStatus.CONNECTED == currentStatus;

        int connectedCount = 0;
        int disconnectedCount = 0;

        // 是否为服务器模式设备
        boolean isServerMode = isNetworkServerMode(device);

        log.debug("正在验证设备 {} 的连接状态，模式: {}",
            device.getName(), isServerMode ? "服务器" : "客户端");

        // 执行多次状态检查
        for (int i = 0; i < STATUS_VERIFICATION_COUNT; i++) {
            try {
                if (deviceService.isConnected(device)) {
                    connectedCount++;
                } else {
                    disconnectedCount++;
                }

                // 延长等待时间，提高检测稳定性
                TimeUnit.MILLISECONDS.sleep(STATUS_CHECK_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("设备状态验证线程被中断");
                break;
            } catch (Exception e) {
                log.warn("检查设备 {} 连接状态时发生异常: {}", device.getName(), e.getMessage());
                disconnectedCount++; // 异常视为断开
            }
        }

        // 根据设备模式返回状态判断结果
        return isServerMode ?
            determineServerModeStatus(isCurrentlyConnected, connectedCount, disconnectedCount) :
            determineClientModeStatus(isCurrentlyConnected, connectedCount, disconnectedCount);
    }

    /**
     * 判断服务器模式设备的状态
     *
     * @param isCurrentlyConnected 当前是否连接
     * @param connectedCount 连接次数
     * @param disconnectedCount 断开次数
     * @return 判断结果
     */
    private boolean determineServerModeStatus(boolean isCurrentlyConnected,
                                              int connectedCount, int disconnectedCount) {
        if (isCurrentlyConnected) {
            // 如果当前状态是已连接，需要至少2/3次检查显示断开才改变状态
            return disconnectedCount < (STATUS_VERIFICATION_COUNT * SERVER_MODE_DISCONNECT_RATIO);
        } else {
            // 如果当前状态是未连接，只需一次检查显示已连接，就转为连接状态
            return connectedCount > 0;
        }
    }

    /**
     * 判断客户端模式设备的状态
     *
     * @param isCurrentlyConnected 当前是否连接
     * @param connectedCount 连接次数
     * @param disconnectedCount 断开次数
     * @return 判断结果
     */
    private boolean determineClientModeStatus(boolean isCurrentlyConnected,
                                              int connectedCount, int disconnectedCount) {
        if (isCurrentlyConnected) {
            // 当前已连接的设备，需要至少2/3的失败才判定为断开
            return disconnectedCount < (STATUS_VERIFICATION_COUNT * CLIENT_MODE_CONNECT_RATIO);
        } else {
            // 当前未连接的设备，需要至少2/3的成功才判定为连接
            return connectedCount >= (STATUS_VERIFICATION_COUNT * CLIENT_MODE_CONNECT_RATIO);
        }
    }

    /**
     * 判断设备是否为网络服务器模式
     *
     * @param device 设备对象
     * @return 是否为网络服务器模式
     */
    private boolean isNetworkServerMode(Device device) {
        if (!CONNECTION_TYPE_NETWORK.equalsIgnoreCase(device.getConnectionType())) {
            return false;
        }

        String params = device.getConnectionParams();
        if (!StringUtils.hasText(params)) {
            return false;
        }

        try {
            String[] parts = params.split(":");

            // 检查参数格式，判断是否有服务器模式标志
            if (parts.length >= 3) {
                // 可能的格式：host:port:protocol:mode
                if (parts.length >= 4 && SERVER_MODE.equalsIgnoreCase(parts[3])) {
                    return true;
                }
                // 或者 port:protocol:mode
                if (SERVER_MODE.equalsIgnoreCase(parts[2])) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("解析设备 {} 连接参数出错: {}", device.getName(), e.getMessage());
        }

        return false;
    }

    /**
     * 清理设备状态记录
     *
     * @param deviceId 设备ID
     */
    private void cleanupDeviceStateRecords(String deviceId) {
        lastStatusChangeTime.remove(deviceId);
        statusChangeConfirmations.remove(deviceId);
    }

    /**
     * 获取设备数量
     *
     * @return 设备总数
     */
    public int getDeviceCount() {
        return devices.size();
    }

    /**
     * 获取已连接设备数量
     *
     * @return 已连接设备数量
     */
    public long getConnectedDeviceCount() {
        return devices.values().stream()
            .filter(device -> DeviceStatus.CONNECTED == device.getStatus())
            .count();
    }
}
