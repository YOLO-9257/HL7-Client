package com.hl7.client.infrastructure.util;

import com.hl7.client.domain.model.DeviceStatus;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * UI工具类
 * 提供字体设置和文本本地化功能
 */
@Slf4j
public class UIHelper {

    // 网络模式文本映射
    private static final Map<String, String> NETWORK_MODE_MAP = new HashMap<>();

    // 连接类型文本映射
    private static final Map<String, String> CONNECTION_TYPE_MAP = new HashMap<>();

    // 设备状态文本映射
    private static final Map<DeviceStatus, String> DEVICE_STATUS_MAP = new HashMap<>();

    static {
        // 初始化网络模式映射
        NETWORK_MODE_MAP.put("CLIENT", "客户端模式");
        NETWORK_MODE_MAP.put("SERVER", "服务器模式");
        NETWORK_MODE_MAP.put("CONNECTED", "已连接");
        NETWORK_MODE_MAP.put("DISCONNECTED", "未连接");
        NETWORK_MODE_MAP.put("ERROR", "错误");

        // 初始化连接类型映射
        CONNECTION_TYPE_MAP.put("NETWORK", "网络连接");
        CONNECTION_TYPE_MAP.put("SERIAL", "串口连接");
        CONNECTION_TYPE_MAP.put("FILE", "文件连接");
        CONNECTION_TYPE_MAP.put("DATABASE", "数据库连接");
        
        // 初始化设备状态映射
        DEVICE_STATUS_MAP.put(DeviceStatus.CONNECTED, "已连接");
        DEVICE_STATUS_MAP.put(DeviceStatus.DISCONNECTED, "未连接");
        DEVICE_STATUS_MAP.put(DeviceStatus.ERROR, "错误");
    }

    /**
     * 设置全局字体大小
     *
     * @param size 字体大小
     */
    public static void setGlobalFontSize(int size) {
        log.info("设置全局字体大小: {}", size);
        setUIFont(new FontUIResource(Font.DIALOG, Font.PLAIN, size));
    }

    /**
     * 设置UI字体
     *
     * @param font 字体
     */
    private static void setUIFont(FontUIResource font) {
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, font);
            }
        }
    }

    /**
     * 获取网络模式的本地化文本
     *
     * @param mode 模式名称
     * @return 本地化文本
     */
    public static String getLocalizedNetworkMode(String mode) {
        if (mode == null) {
            return "未知";
        }

        return NETWORK_MODE_MAP.getOrDefault(mode, mode);
    }

    /**
     * 获取设备状态的本地化文本
     *
     * @param status 设备状态
     * @return 本地化文本
     */
    public static String getLocalizedNetworkMode(DeviceStatus status) {
        if (status == null) {
            return "未知";
        }

        return DEVICE_STATUS_MAP.getOrDefault(status, status.name());
    }

    /**
     * 获取连接类型的本地化文本
     *
     * @param type 连接类型名称
     * @return 本地化文本
     */
    public static String getLocalizedConnectionType(String type) {
        if (type == null) {
            return "未知";
        }

        return CONNECTION_TYPE_MAP.getOrDefault(type, type);
    }

    /**
     * 应用字体到特定组件
     *
     * @param component 组件
     * @param size 字体大小
     */
    public static void applyFontToComponent(JComponent component, int size) {
        Font currentFont = component.getFont();
        Font newFont = new Font(currentFont.getName(), currentFont.getStyle(), size);
        component.setFont(newFont);
    }

    /**
     * 应用字体到表格
     *
     * @param table 表格
     * @param size 字体大小
     */
    public static void applyFontToTable(JTable table, int size) {
        // 设置表格字体
        Font tableFont = new Font(Font.DIALOG, Font.PLAIN, size);
        table.setFont(tableFont);

        // 设置表头字体
        Font headerFont = new Font(Font.DIALOG, Font.BOLD, size);
        table.getTableHeader().setFont(headerFont);

        // 设置行高
        table.setRowHeight(size + 8);
    }
}
