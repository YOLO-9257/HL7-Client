package com.hl7.client.domain.constants;

/**
 * 应用常量类
 * 用于集中管理应用中的各种常量值
 */
public final class ApplicationConstants {

    /**
     * 禁止实例化
     */
    private ApplicationConstants() {
        throw new AssertionError("Constants class should not be instantiated");
    }

    /**
     * 通用超时和重试相关常量
     */
    public static final class Timeout {
        /**
         * 设备连接超时时间（毫秒）
         */
        public static final int DEVICE_CONNECTION_TIMEOUT_MS = 5000;

        /**
         * 设备读取超时时间（毫秒）
         */
        public static final int DEVICE_READ_TIMEOUT_MS = 10000;

        /**
         * 设备写入超时时间（毫秒）
         */
        public static final int DEVICE_WRITE_TIMEOUT_MS = 3000;

        /**
         * 服务器连接超时时间（毫秒）
         */
        public static final int SERVER_CONNECTION_TIMEOUT_MS = 8000;

        /**
         * 重试间隔时间（毫秒）
         */
        public static final int RETRY_INTERVAL_MS = 60000;

        /**
         * 最大重试次数
         */
        public static final int MAX_RETRY_COUNT = 3;

        /**
         * 消息缓冲区超时时间（毫秒）
         * 超过此时间的未完成消息将被清除
         */
        public static final int MESSAGE_BUFFER_TIMEOUT_MS = 60000;
    }

    /**
     * 消息处理相关常量
     */
    public static final class MessageProcessing {
        /**
         * 消息处理周期间隔（毫秒）
         */
        public static final int MESSAGE_PROCESS_INTERVAL_MS = 5000;

        /**
         * 消息队列最大大小
         */
        public static final int MAX_QUEUE_SIZE = 1000;

        /**
         * 消息接收队列最大大小
         */
        public static final int MAX_RECEIVED_QUEUE_SIZE = 500;

        /**
         * 消息处理超时时间（毫秒）
         */
        public static final int MESSAGE_PROCESS_TIMEOUT_MS = 30000;

        /**
         * 消息缓冲区最大大小（字节）
         */
        public static final int MAX_BUFFER_SIZE_BYTES = 1048576; // 1MB

        /**
         * 消息处理批次大小
         */
        public static final int BATCH_SIZE = 50;
    }

    /**
     * UI相关常量
     */
    public static final class UI {
        /**
         * 全局字体大小
         */
        public static final int GLOBAL_FONT_SIZE = 16;

        /**
         * 状态刷新间隔（毫秒）
         */
        public static final int STATUS_REFRESH_INTERVAL_MS = 1000;
    }

    /**
     * 网络相关常量
     */
    public static final class Network {
        /**
         * 单实例检测服务端口
         */
        public static final int SINGLE_INSTANCE_PORT = 9999;

        /**
         * 本地主机地址
         */
        public static final String LOCALHOST = "127.0.0.1";
    }

    /**
     * 属性键常量
     */
    public static final class PropertyKeys {
        /**
         * 消息处理间隔属性键
         */
        public static final String MESSAGE_PROCESS_INTERVAL = "hl7.message.process.interval";

        /**
         * 消息重试间隔属性键
         */
        public static final String MESSAGE_RETRY_INTERVAL = "hl7.message.retry.interval";

        /**
         * 消息队列最大大小属性键
         */
        public static final String MESSAGE_QUEUE_MAX_SIZE = "hl7.message.queue.max-size";

        /**
         * 消息缓冲区最大大小属性键
         */
        public static final String MESSAGE_BUFFER_MAX_SIZE = "hl7.message.buffer.max-size";

        /**
         * 消息处理批次大小属性键
         */
        public static final String MESSAGE_BATCH_SIZE = "hl7.message.batch.size";
    }
}
