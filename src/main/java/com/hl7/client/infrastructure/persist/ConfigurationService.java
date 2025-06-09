package com.hl7.client.infrastructure.persist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hl7.client.domain.model.Device;
import com.hl7.client.infrastructure.event.ConfigurationChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置持久化服务
 * 负责将配置信息保存到本地文件，并在系统启动时加载
 */
@Slf4j
@Service
public class ConfigurationService {

    private static final String CONFIG_DIR = "config";
    private static final String DEVICES_FILE = "devices.json";

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Autowired
    public ConfigurationService(ApplicationEventPublisher eventPublisher, ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @PostConstruct
    public void init() {
        // 确保配置目录存在
        try {
            Path configDir = Paths.get(CONFIG_DIR);
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
                log.info("创建配置目录: {}", configDir.toAbsolutePath());
            } else {
                log.info("配置目录已存在: {}", configDir.toAbsolutePath());
            }

            // 确保设备配置文件存在
            File deviceFile = new File(CONFIG_DIR, DEVICES_FILE);
            if (!deviceFile.exists()) {
                // 创建空的设备列表文件
                objectMapper.writeValue(deviceFile, new ArrayList<Device>());
                log.info("创建空的设备配置文件: {}", deviceFile.getAbsolutePath());
            } else {
                log.info("设备配置文件已存在: {}", deviceFile.getAbsolutePath());
            }
        } catch (IOException e) {
            log.error("初始化配置目录失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 保存设备配置到文件
     *
     * @param devices 设备列表
     */
    public void saveDevices(List<Device> devices) {
        try {
            File file = new File(CONFIG_DIR, DEVICES_FILE);
            objectMapper.writeValue(file, devices);
            log.info("设备配置已保存到: {}", file.getAbsolutePath());

            // 发布配置变更事件
            eventPublisher.publishEvent(new ConfigurationChangeEvent(this,
                ConfigurationChangeEvent.Type.DEVICE_CHANGED));
        } catch (IOException e) {
            log.error("保存设备配置失败: {}", e.getMessage());
        }
    }

    /**
     * 从文件加载设备配置
     *
     * @return 设备列表
     */
    public List<Device> loadDevices() {
        try {
            File file = new File(CONFIG_DIR, DEVICES_FILE);
            if (file.exists()) {
                return objectMapper.readValue(file,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Device.class));
            }
        } catch (IOException e) {
            log.error("加载设备配置失败: {}", e.getMessage());
        }
        return new ArrayList<>();
    }
}
