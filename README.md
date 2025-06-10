# HL7-Client 通信框架

<div align="center">

![Version](https://img.shields.io/badge/version-0.0.1--SNAPSHOT-blue)
![Java](https://img.shields.io/badge/Java-8%2B-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.3.4-brightgreen)
![License](https://img.shields.io/badge/license-MIT-green)

</div>

## 📋 项目简介

HL7-Client是一个用于医疗设备通信的Java框架，支持串口和网络通信，能够自动处理HL7消息。该框架旨在简化医疗设备与信息系统之间的通信过程，只需增加配置即可实现自动监听、解析和发送数据。框架采用优良的设计模式和类型安全的编码实践，提高了系统的稳定性和可维护性。

## ✨ 主要功能

- 🔌 **多通信方式**: 支持串口通信（基于RXTX）和TCP网络通信（基于Netty）
- 📊 **多消息格式**: 支持HL7（基于HAPI）和其他自定义格式消息的解析
- ⚙️ **配置化管理**: 基于YAML的简洁配置，快速集成设备
- 🔄 **自动重连机制**: 设备连接监控和自动重连
- 🧩 **可扩展架构**: 支持不同设备的消息完整性检查策略
- 🛠️ **灵活定制**: 可自定义消息处理流程

## 🚀 快速开始

### 安装

在你的Maven项目中添加依赖:

```xml
<dependency>
    <groupId>com.hl7.client</groupId>
    <artifactId>hl7-client</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 步骤1: 配置通信参数

在`application.yml`文件中添加通信配置：

```yaml
communication:
  # 是否自动启动通信
  auto-start: true
  
  # 串口配置
  serial-ports:
    - port-name: COM1
      baud-rate: 9600
      data-bits: 8
      stop-bits: 1
      parity: 0
      device-model: BG800
      enabled: true
      
  # 网络配置
  networks:
    - host: 0.0.0.0
      port: 8888
      mode: SERVER
      device-model: DEVICE_A
      enabled: true
    - host: 192.168.1.100
      port: 9000
      mode: CLIENT
      device-model: DEVICE_B
      enabled: true
      
  # 设备配置
  devices:
    BG800:
      name: 凝血仪
      description: 凝血检测分析仪
      message-type: HL7
      parameters:
        param1: value1
    DEVICE_A:
      name: 设备A
      description: 网络服务器设备
      message-type: HL7
      parameters:
        param1: value1
    DEVICE_B:
      name: 设备B
      description: 网络客户端设备
      message-type: JSON
      parameters:
        param1: value1
```

### 步骤2: 实现消息完整性检查策略

为特定设备创建消息完整性检查策略，例如：

```java
@Component
public class MyDeviceCompletionStrategy implements MessageCompletionStrategy {

    @Override
    public String isMessageComplete(Message message) {
        // 实现消息完整性检查逻辑
        // 返回null表示消息完整
        // 返回非null表示需要发送的响应
        return null;
    }

    @Override
    public boolean supports(String deviceModel) {
        return "MY_DEVICE".equals(deviceModel);
    }
}
```

### 步骤3: 启动应用

启动应用后，系统会自动：
- 根据配置创建设备适配器
- 连接所有配置的设备
- 监听设备数据并自动处理
- 定期检查连接状态并自动重连

## 🔄 消息处理流程

1. 接收数据并添加到缓冲区
2. 使用消息完整性检查策略检查消息是否完整
3. 如果消息不完整，返回响应（如果需要）
4. 如果消息完整，处理消息并清空缓冲区
5. 如果配置了自动处理，将消息发送到服务器

## 🔌 扩展功能

### 添加新设备支持

1. 在`devices`配置中添加设备信息
2. 实现该设备的`MessageCompletionStrategy`
3. 如果需要特殊消息解析，实现对应的`MessageParser`接口

```java
@Component
public class CustomMessageParser implements MessageParser<CustomMessage> {
    
    @Override
    public CustomMessage parse(byte[] data) {
        // 自定义解析逻辑
        return new CustomMessage();
    }
    
    @Override
    public boolean supports(String messageType) {
        return "CUSTOM".equals(messageType);
    }
}
```

### 自定义消息处理

通过修改`MessageHandlerDelegate`的实现可以自定义消息处理逻辑：

```java
@Component
public class CustomMessageHandler implements MessageHandlerDelegate {
    
    @Override
    public void handleMessage(Message message, DeviceInfo deviceInfo) {
        // 自定义消息处理逻辑
    }
}
```

### 设备状态管理

框架使用`DeviceStatus`枚举替代字符串常量来表示设备状态：

```java
public enum DeviceStatus {
    /** 已连接 */
    CONNECTED,

    /** 未连接 */
    DISCONNECTED,

    /** 错误状态 */
    ERROR;
    
    // 工具方法...
}
```

在使用时，可以直接比较枚举值而不是字符串：

```java
// 推荐的用法
if (device.getStatus() == DeviceStatus.CONNECTED) {
    // 设备已连接
}

// 不再需要字符串比较
// if ("CONNECTED".equals(device.getStatus()))
```

## 💻 系统要求

- Java 8+
- Spring Boot 2.3.4+
- 对于串口通信：RXTX库 2.2
- 对于网络通信：Netty 4.1.48+
- HAPI HL7 2.3（用于HL7消息处理）

## 🔍 故障排除

如果遇到问题，请查看日志文件了解详细信息。常见问题包括：

- **串口不存在或被占用**：检查设备连接和权限设置
- **网络连接失败**：确认IP地址和端口设置，检查网络防火墙
- **消息解析错误**：验证消息格式是否符合预期

## 📝 日志记录

系统使用SLF4J进行日志记录，可以通过调整日志级别来控制日志输出：

```yaml
logging:
  level:
    com.hl7.client: INFO  # 设置为DEBUG可获取更详细信息
``` 

## 🤝 贡献指南

欢迎为项目做出贡献！以下是贡献流程：

1. Fork该项目
2. 创建您的特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交您的更改 (`git commit -m 'Add some amazing feature'`)
4. 将您的更改推送到分支 (`git push origin feature/amazing-feature`)
5. 创建一个Pull Request

## 📄 许可证

该项目采用MIT许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📞 联系方式

如有问题或建议，请通过以下方式联系：

- 提交GitHub Issues
- 项目维护者: [YOLO]

---

<div align="center">
  <sub>构建于 ❤️ 和 ☕</sub>
</div> 
