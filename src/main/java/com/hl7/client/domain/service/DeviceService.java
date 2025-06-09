package com.hl7.client.domain.service;

import com.hl7.client.domain.model.Device;
import com.hl7.client.domain.model.DeviceStatus;
import com.hl7.client.domain.model.Message;
import com.hl7.client.domain.port.DevicePort;
import com.hl7.client.infrastructure.adapter.DeviceAdapter;
import com.hl7.client.infrastructure.factory.DeviceAdapterCache;
import com.hl7.client.infrastructure.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 设备服务
 * 实现设备端口接口，提供核心业务功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService implements DevicePort {

    // 移除设备状态常量，使用DeviceStatus枚举替代
    private static final String MESSAGE_STATUS_NEW = "NEW";

    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 100;

    // 消息类型常量
    private static final String MESSAGE_TYPE_UNKNOWN = "UNKNOWN";
    private static final String MESSAGE_TYPE_HL7 = "HL7";
    private static final String MESSAGE_TYPE_HL7_MLLP = "HL7_MLLP";
    private static final String MESSAGE_TYPE_JSON = "JSON";
    private static final String MESSAGE_TYPE_XML = "XML";
    private static final String MESSAGE_TYPE_TEXT = "TEXT";

    // 连接类型常量
    private static final String CONNECTION_TYPE_NETWORK = "NETWORK";
    private static final String CONNECTION_MODE_SERVER = "SERVER";

    // 用于JSON检测的正则表达式
    private static final Pattern JSON_PATTERN = Pattern.compile("^\\s*[{\\[].*[}\\]]\\s*$", Pattern.DOTALL);
    // 用于XML检测的正则表达式
    private static final Pattern XML_PATTERN = Pattern.compile("^\\s*<[^>]+>.*</[^>]+>\\s*$", Pattern.DOTALL);

    private final DeviceAdapterCache adapterCache;

    @Override
    public boolean connect(Device device) {
        String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
        log.debug("执行方法: {}, 设备: {}", methodName, device.getName());

        try {
            DeviceAdapter adapter = adapterCache.getAdapter(device);
            boolean connected = adapter.connect();

            if (connected) {
                device.setStatus(DeviceStatus.CONNECTED);
                log.info("设备 {} 连接成功", device.getName());
            } else {
                device.setStatus(DeviceStatus.ERROR);
                log.error("设备 {} 连接失败", device.getName());
            }

            return connected;
        } catch (Exception e) {
            device.setStatus(DeviceStatus.ERROR);
            log.error("设备 {} 连接过程中发生异常: {}", device.getName(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void disconnect(Device device) {
        String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
        log.debug("执行方法: {}, 设备: {}", methodName, device.getName());

        try {
            DeviceAdapter adapter = adapterCache.getAdapter(device);
            adapter.disconnect();
            device.setStatus(DeviceStatus.DISCONNECTED);
            log.info("设备 {} 已断开连接", device.getName());
        } catch (Exception e) {
            log.error("断开设备 {} 连接过程中发生异常: {}", device.getName(), e.getMessage(), e);
        }
    }

    @Override
    public boolean sendMessage(Device device, String message) {
        String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
        log.debug("执行方法: {}, 设备: {}", methodName, device.getName());

        if (message == null || message.isEmpty()) {
            log.error("无法发送空消息到设备 {}", device.getName());
            return false;
        }

        try {
            DeviceAdapter adapter = adapterCache.getAdapter(device);
            if (!adapter.isConnected()) {
                log.error("设备 {} 未连接，无法发送消息", device.getName());
                return false;
            }

            boolean sent = adapter.send(message);
            if (sent) {
                log.info("成功发送消息到设备 {}", device.getName());
            } else {
                log.error("向设备 {} 发送消息失败", device.getName());
            }

            return sent;
        } catch (Exception e) {
            log.error("向设备 {} 发送消息过程中发生异常: {}", device.getName(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Message receiveMessage(Device device) {
        String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
        log.debug("执行方法: {}, 设备: {}", methodName, device.getName());

        try {
            DeviceAdapter adapter = adapterCache.getAdapter(device);
            if (!adapter.isConnected()) {
                log.error("设备 {} 未连接，无法接收消息", device.getName());
                return null;
            }

            String rawContent = adapter.receive();
            if (rawContent == null || rawContent.isEmpty()) {
                log.debug("从设备 {} 接收到空消息", device.getName());
                return null;
            }

            String messageType = detectMessageType(rawContent);

            // 创建消息对象
            Message message = Message.builder()
                    .id(String.valueOf(SnowflakeIdGenerator.getInstance().nextId()))
                    .deviceId(device.getId())
                    .deviceModel(device.getModel())
                    .rawContent(rawContent)
                    .messageType(messageType)
                    .receivedTime(LocalDateTime.now())
                    .status(MESSAGE_STATUS_NEW)
                    .build();

            log.info("从设备 {} (型号: {}) 接收到消息，ID: {}, 类型: {}",
                    device.getName(), device.getModel(), message.getId(), message.getMessageType());

            return message;
        } catch (Exception e) {
            log.error("从设备 {} 接收消息过程中发生异常: {}", device.getName(), e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean isConnected(Device device) {
        String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
        log.debug("执行方法: {}, 设备: {}", methodName, device.getName());

        if (device == null) {
            log.error("无法检查空设备的连接状态");
            return false;
        }

        try {
            DeviceAdapter adapter = adapterCache.getAdapter(device);

            // 对于服务器模式的设备，直接返回连接状态
            if (isNetworkServerMode(device)) {
                boolean connected = adapter.isConnected();
                log.debug("服务器模式设备 {} 连接状态检查: {}",
                        device.getName(), connected ? "已连接" : "未连接");
                return connected;
            }

            // 对于客户端模式，添加重试逻辑
            return checkConnectionWithRetry(device, adapter);
        } catch (Exception e) {
            log.error("检查设备 {} 连接状态过程中发生异常: {}", device.getName(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 带重试机制的连接检查
     *
     * @param device 设备对象
     * @param adapter 设备适配器
     * @return 是否连接成功
     */
    private boolean checkConnectionWithRetry(Device device, DeviceAdapter adapter) {
        int retryCount = 0;
        boolean connected = false;
        Exception lastException = null;

        while (retryCount < MAX_RETRIES && !connected) {
            try {
                connected = adapter.isConnected();
                if (connected) {
                    break;
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("检查设备 {} 连接状态第 {} 次尝试失败: {}",
                        device.getName(), retryCount + 1, e.getMessage());
            }
            retryCount++;

            // 短暂延迟后重试
            if (retryCount < MAX_RETRIES && !connected) {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("重试等待被中断");
                    break;
                }
            }
        }

        // 如果所有重试都失败
        if (!connected && lastException != null) {
            log.error("经过 {} 次尝试后检查设备 {} 连接状态失败: {}",
                    MAX_RETRIES, device.getName(), lastException.getMessage());
        }

        return connected;
    }

    /**
     * 判断设备是否为网络服务器模式
     *
     * @param device 设备对象
     * @return 是否为网络服务器模式
     */
    private boolean isNetworkServerMode(Device device) {
        if (device == null || device.getConnectionType() == null) {
            return false;
        }

        // 必须是网络类型设备
        if (!CONNECTION_TYPE_NETWORK.equalsIgnoreCase(device.getConnectionType())) {
            return false;
        }

        String params = device.getConnectionParams();
        if (params == null || params.isEmpty()) {
            return false;
        }

        // 更精确的服务器模式检测逻辑
        String[] parts = params.split(":");

        // 显式标记为SERVER模式
        if ((parts.length >= 4 && CONNECTION_MODE_SERVER.equalsIgnoreCase(parts[3])) ||
                (parts.length >= 3 && CONNECTION_MODE_SERVER.equalsIgnoreCase(parts[2]))) {
            log.debug("设备 {} 通过mode参数识别为服务器模式", device.getName());
            return true;
        }

        // 参数格式为 port:protocol 的服务器模式 (没有主机名)
        if (parts.length == 2) {
            try {
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
     * 检测消息类型
     *
     * @param content 消息内容
     * @return 消息类型
     */
    private String detectMessageType(String content) {
        if (content == null || content.isEmpty()) {
            return MESSAGE_TYPE_UNKNOWN;
        }

        // 检测HL7消息
        if (content.startsWith("MSH|") || content.contains("\rMSH|")) {
            return MESSAGE_TYPE_HL7;
        }

        // 检测HL7 MLLP消息
        if (content.contains("\u000b") && content.contains("\u001c")) {
            return MESSAGE_TYPE_HL7_MLLP;
        }

        // 检测JSON格式
        if (JSON_PATTERN.matcher(content).matches()) {
            return MESSAGE_TYPE_JSON;
        }

        // 检测XML格式
        if (XML_PATTERN.matcher(content).matches()) {
            return MESSAGE_TYPE_XML;
        }

        // 默认为文本
        return MESSAGE_TYPE_TEXT;
    }
}
