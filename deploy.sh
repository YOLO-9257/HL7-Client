#!/bin/bash

# HL7客户端部署脚本
# 用法: ./deploy.sh [安装目录]

# 默认安装目录
INSTALL_DIR=${1:-/opt/hl7-client}
SERVICE_USER="hl7user"
SERVICE_GROUP="hl7user"
JAR_FILE="target/hl7-client-0.0.1-SNAPSHOT.jar"
SERVICE_FILE="hl7-client.service"

echo "==================== HL7客户端部署脚本 ===================="
echo "安装目录: $INSTALL_DIR"

# 检查是否为root用户
if [ "$EUID" -ne 0 ]; then
  echo "请使用root权限运行此脚本"
  exit 1
fi

# 创建安装目录
echo "创建安装目录..."
mkdir -p $INSTALL_DIR
if [ $? -ne 0 ]; then
  echo "创建安装目录失败"
  exit 1
fi

# 检查服务用户是否存在，不存在则创建
if ! id "$SERVICE_USER" &>/dev/null; then
  echo "创建服务用户 $SERVICE_USER..."
  useradd -r -s /bin/false $SERVICE_USER
  if [ $? -ne 0 ]; then
    echo "创建服务用户失败"
    exit 1
  fi
fi

# 检查JAR文件是否存在
if [ ! -f "$JAR_FILE" ]; then
  echo "JAR文件不存在: $JAR_FILE"
  echo "请先构建项目"
  exit 1
fi

# 复制JAR文件到安装目录
echo "复制应用程序到安装目录..."
cp $JAR_FILE $INSTALL_DIR/hl7-client.jar
if [ $? -ne 0 ]; then
  echo "复制应用程序失败"
  exit 1
fi

# 复制配置文件到安装目录
if [ -d "config" ]; then
  echo "复制配置文件..."
  cp -r config $INSTALL_DIR/
fi

# 设置权限
echo "设置权限..."
chown -R $SERVICE_USER:$SERVICE_GROUP $INSTALL_DIR
chmod -R 750 $INSTALL_DIR

# 安装systemd服务
echo "安装系统服务..."
cp $SERVICE_FILE /etc/systemd/system/
if [ $? -ne 0 ]; then
  echo "安装系统服务失败"
  exit 1
fi

# 重新加载systemd配置
echo "重新加载systemd配置..."
systemctl daemon-reload

# 启用并启动服务
echo "启用并启动服务..."
systemctl enable hl7-client.service
systemctl start hl7-client.service

# 检查服务状态
echo "检查服务状态..."
systemctl status hl7-client.service

echo "==================== 部署完成 ===================="
echo "服务已安装并启动"
echo "使用以下命令管理服务:"
echo "  启动: systemctl start hl7-client.service"
echo "  停止: systemctl stop hl7-client.service"
echo "  重启: systemctl restart hl7-client.service"
echo "  状态: systemctl status hl7-client.service"
echo "  日志: journalctl -u hl7-client.service"
echo "==========================================" 