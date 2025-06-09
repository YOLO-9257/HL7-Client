# HL7 测试工具套件

本测试套件提供了模拟医疗仪器发送HL7消息的工具，用于测试HL7客户端的接收和解析功能。

## 主要组件

### 1. HL7DeviceSimulator

模拟医疗仪器，发送HL7格式的消息。

- 支持单条和批量发送
- 支持长连接和短连接模式
- 提供标准HL7消息生成工具

### 2. HL7MockServer

模拟HL7服务器，接收并处理HL7消息。

- 支持多客户端并发连接
- 记录接收到的所有消息
- 提供消息统计功能

### 3. HL7TestScenarios

包含多种测试场景和测试用例。

- 基础消息测试
- 批量消息测试
- 多类型消息测试
- 错误消息测试
- 压力测试

### 4. HL7IntegrationTest

集成测试工具，启动服务器和客户端进行完整测试。

## 使用方法

### 模拟服务器

启动模拟HL7服务器:

```java
// 创建并启动服务器（监听8088端口）
HL7MockServer server = new HL7MockServer(8088);
server.start();

// 查看接收到的消息数量
int count = server.getMessageCount();

// 获取接收到的所有消息
List<String> messages = server.getReceivedMessages();

// 清除接收到的消息
server.clearMessages();

// 停止服务器
server.stop();
```

### 模拟仪器发送消息

```java
// 创建模拟仪器（连接到localhost:8088）
HL7DeviceSimulator simulator = new HL7DeviceSimulator("localhost", 8088);

// 设置为长连接模式
simulator.setLongConnection(true);

// 连接到服务器
simulator.connect();

// 生成并发送单条HL7消息
String message = HL7DeviceSimulator.generateORU_R01("P10001", "张三", "GLU", "110");
simulator.sendMessage(message);

// 批量发送消息
List<String> messages = new ArrayList<>();
messages.add(HL7DeviceSimulator.generateORU_R01("P10002", "李四", "GLU", "120"));
messages.add(HL7DeviceSimulator.generateORU_R01("P10003", "王五", "GLU", "130"));
simulator.sendMessages(messages);

// 断开连接
simulator.disconnect();
```

### 运行测试场景

```java
// 基础测试
HL7TestScenarios.basicTest("localhost", 8088);

// 批量测试（发送10条消息）
HL7TestScenarios.batchTest("localhost", 8088, 10);

// 多种类型消息测试
HL7TestScenarios.multiTypeMessageTest("localhost", 8088);

// 错误消息测试
HL7TestScenarios.errorMessageTest("localhost", 8088);

// 压力测试（5个设备，每个设备10条消息）
HL7TestScenarios.stressTest("localhost", 8088, 5, 10);

// 运行所有测试
HL7TestScenarios.runAllTests("localhost", 8088);
```

### 运行集成测试

```java
// 运行集成测试
HL7IntegrationTest.main(new String[0]);

// 或者运行完整测试套件
HL7IntegrationTest.runFullTestSuite();
```

## 示例用法

以下是一个完整的测试示例：

```java
// 启动模拟服务器
HL7MockServer server = new HL7MockServer(8088);
server.start();

// 等待服务器启动
Thread.sleep(1000);

// 运行测试场景
HL7TestScenarios.basicTest("localhost", 8088);

// 查看接收到的消息
System.out.println("接收到消息数: " + server.getMessageCount());

// 停止服务器
server.stop();
```

## 定制消息

您可以创建自定义的HL7消息:

```java
// 手动创建血气分析结果消息
String customMessage = 
        "MSH|^~\\&|ANALYZER|LAB|LIS|HOSPITAL|" + HL7DeviceSimulator.getCurrentTimeStamp() + 
        "||ORU^R01|BG12345|P|2.5\r" +
        "PID|||P10002||李医生||19750315|F\r" +
        "OBR|1|ORD123456||BG Panel|\r" +
        "OBX|1|NM|pH||7.35|pH|7.35-7.45|N|||F\r" +
        "OBX|2|NM|pCO2||45|mmHg|35-45|N|||F\r" +
        "OBX|3|NM|pO2||80|mmHg|80-100|N|||F\r";

// 发送自定义消息        
simulator.sendMessage(customMessage);
``` 