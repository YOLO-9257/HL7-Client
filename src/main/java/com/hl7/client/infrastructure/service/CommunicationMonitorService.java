package com.hl7.client.infrastructure.service;

import com.hl7.client.infrastructure.adapter.DeviceAdapter;
import com.hl7.client.infrastructure.adapter.common.DeviceAutoConfigurator;
import com.hl7.client.infrastructure.config.CommunicationConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 通信监控服务
 * 自动管理设备连接和监控
 */
@Slf4j
@Service
public class CommunicationMonitorService {

    @Autowired
    private DeviceAutoConfigurator configurator;

    @Autowired
    private CommunicationConfig config;

    private final List<DeviceAdapter> deviceAdapters = new ArrayList<>();
    private ScheduledExecutorService monitorExecutor;

    /**
     * 服务启动时自动初始化设备并连接
     */
    @PostConstruct
    public void init() {
        if (!config.isAutoStart()) {
            log.info("自动启动已禁用，跳过设备初始化");
            return;
        }

        // 创建设备适配器
        deviceAdapters.addAll(configurator.createDeviceAdapters());

        if (deviceAdapters.isEmpty()) {
            log.warn("没有找到可用的设备配置");
            return;
        }

        // 连接所有设备
        connectAllDevices();

        // 启动监控
        startMonitoring();
    }

    /**
     * 服务关闭时断开所有连接
     */
    @PreDestroy
    public void destroy() {
        stopMonitoring();
        disconnectAllDevices();
    }

    /**
     * 连接所有设备
     */
    public void connectAllDevices() {
        log.info("开始连接所有配置的设备...");
        for (DeviceAdapter adapter : deviceAdapters) {
            try {
                if (!adapter.isConnected()) {
                    boolean success = adapter.connect();
                    if (success) {
                        log.info("成功连接设备: {}", adapter.getDevice().getName());
                    } else {
                        log.error("连接设备失败: {}", adapter.getDevice().getName());
                    }
                } else {
                    log.info("设备 {} 已连接，跳过", adapter.getDevice().getName());
                }
            } catch (Exception e) {
                log.error("连接设备 {} 时发生异常: {}", adapter.getDevice().getName(), e.getMessage(), e);
            }
        }
    }

    /**
     * 断开所有设备连接
     */
    public void disconnectAllDevices() {
        log.info("断开所有设备连接...");
        for (DeviceAdapter adapter : deviceAdapters) {
            try {
                if (adapter.isConnected()) {
                    adapter.disconnect();
                    log.info("已断开设备 {} 的连接", adapter.getDevice().getName());
                }
            } catch (Exception e) {
                log.error("断开设备 {} 连接时发生异常: {}", adapter.getDevice().getName(), e.getMessage(), e);
            }
        }
    }

    /**
     * 启动监控
     */
    private void startMonitoring() {
        if (monitorExecutor == null || monitorExecutor.isShutdown()) {
            monitorExecutor = Executors.newSingleThreadScheduledExecutor();
            monitorExecutor.scheduleAtFixedRate(this::checkConnections, 30, 30, TimeUnit.SECONDS);
            log.info("设备连接监控已启动");
        }
    }

    /**
     * 停止监控
     */
    private void stopMonitoring() {
        if (monitorExecutor != null && !monitorExecutor.isShutdown()) {
            try {
                monitorExecutor.shutdown();
                if (!monitorExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    monitorExecutor.shutdownNow();
                }
                log.info("设备连接监控已停止");
            } catch (InterruptedException e) {
                monitorExecutor.shutdownNow();
                Thread.currentThread().interrupt();
                log.warn("停止设备监控时被中断");
            }
        }
    }

    /**
     * 检查所有连接状态并自动重连断开的设备
     */
    private void checkConnections() {
        for (DeviceAdapter adapter : deviceAdapters) {
            try {
                if (!adapter.isConnected()) {
                    log.info("设备 {} 连接已断开，尝试重新连接", adapter.getDevice().getName());
                    boolean success = adapter.connect();
                    if (success) {
                        log.info("成功重新连接设备: {}", adapter.getDevice().getName());
                    } else {
                        log.error("重新连接设备失败: {}", adapter.getDevice().getName());
                    }
                }
            } catch (Exception e) {
                log.error("检查设备 {} 连接时发生异常: {}", adapter.getDevice().getName(), e.getMessage());
            }
        }
    }
}
