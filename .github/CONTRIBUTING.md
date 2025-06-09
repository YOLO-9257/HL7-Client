# 贡献指南

感谢您对HL7-Client项目的关注！我们欢迎各种形式的贡献，包括但不限于代码贡献、文档改进、问题报告和功能请求。

## 如何贡献

### 报告问题

1. 使用GitHub Issues提交问题
2. 清晰描述问题，包括复现步骤
3. 提供环境信息（操作系统、Java版本等）
4. 如可能，附上日志文件或错误截图

### 提交代码

1. Fork本仓库
2. 创建您的特性分支 (`git checkout -b feature/amazing-feature`)
3. 编写代码并测试
4. 提交您的更改 (`git commit -m '添加某功能'`)
5. 推送到分支 (`git push origin feature/amazing-feature`)
6. 创建Pull Request

### Pull Request指南

- 确保PR标题清晰表达变更内容
- 详细描述实现方法和解决的问题
- 包含适当的测试
- 遵循项目的代码风格和约定
- 更新相关文档

## 开发环境设置

1. 克隆仓库
2. 确保安装JDK 8+和Maven
3. 导入IDE（推荐IntelliJ IDEA或Eclipse）
4. 执行`mvn clean install`安装依赖

## 代码规范

- 遵循Java代码规范
- 类、方法和变量使用有意义的命名
- 添加必要的注释和文档
- 遵循现有的代码风格

## 分支策略

- `main`: 稳定版本分支
- `dev`: 开发分支，新功能合并到此分支
- `feature/*`: 新功能开发分支
- `bugfix/*`: 缺陷修复分支

## 发布流程

版本号遵循[语义化版本](https://semver.org/lang/zh-CN/)规范：

- 主版本号：不兼容的API变更
- 次版本号：向下兼容的功能性新增
- 修订号：向下兼容的问题修复

## 联系方式

有任何问题，请随时通过GitHub Issues与我们联系！ 