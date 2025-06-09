package com.hl7.client.infrastructure.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 配置变更事件
 * 用于通知系统配置已经更改
 */
@Getter
public class ConfigurationChangeEvent extends ApplicationEvent {

    /**
     * 配置变更事件类型
     */
    public enum Type {
        /** 设备配置变更 */
        DEVICE_CHANGED,

        /** 系统配置变更 */
        SYSTEM_CHANGED
    }

    /** 事件类型
     * -- GETTER --
     *  获取事件类型
     *
     * @return 事件类型
     */
    private final Type type;

    /**
     * 创建配置变更事件
     *
     * @param source 事件源
     * @param type 事件类型
     */
    public ConfigurationChangeEvent(Object source, Type type) {
        super(source);
        this.type = type;
    }

}
