package com.hl7.client.infrastructure.adapter.serial;

import com.hl7.client.domain.model.Device;
import com.hl7.client.infrastructure.adapter.common.AbstractCommunicationAdapter;
import com.hl7.client.infrastructure.config.CommunicationConfig;
import gnu.io.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;
import java.util.concurrent.TimeUnit;

/**
 * 串口适配器
 * 使用RXTX库实现串口通信
 */
@Slf4j
@Component
public class SerialPortAdapter extends AbstractCommunicationAdapter {

    private String portName;
    private int baudRate;
    private int dataBits;
    private int stopBits;
    private int parity;

    private SerialPort serialPort;
    private InputStream inputStream;
    private OutputStream outputStream;

    @Autowired
    public SerialPortAdapter() {
        super();
    }

    /**
     * 从配置初始化串口
     * @param config 串口配置
     * @param device 设备信息
     */
    public void initializeFromConfig(CommunicationConfig.SerialConfig config, Device device) {
        this.portName = config.getPortName();
        this.baudRate = config.getBaudRate();
        this.dataBits = config.getDataBits();
        this.stopBits = config.getStopBits();
        this.parity = config.getParity();

        initialize(device);
    }

    @Override
    public void initialize(Device device) {
        super.initialize(device);

        // 如果已经通过配置初始化，则跳过
        if (portName != null) {
            return;
        }

        try {
            // 从连接参数中解析串口配置 (格式: COM1:9600:8:1:0)
            String[] params = device.getConnectionParams().split(":");
            this.portName = params[0];
            this.baudRate = Integer.parseInt(params[1]);
            this.dataBits = Integer.parseInt(params[2]);
            this.stopBits = Integer.parseInt(params[3]);
            this.parity = Integer.parseInt(params[4]);
        } catch (Exception e) {
            log.error("初始化串口适配器失败: {}", e.getMessage());
            throw new IllegalArgumentException("连接参数格式错误，应为portName:baudRate:dataBits:stopBits:parity");
        }
    }

    @Override
    public boolean connect() {
        try {
            // 获取端口标识符
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);

            // 打开串口
            CommPort commPort = portIdentifier.open(this.getClass().getName(), 2000);

            // 判断是否是串口
            if (!(commPort instanceof SerialPort)) {
                log.error("{}不是串口", portName);
                return false;
            }

            serialPort = (SerialPort) commPort;

            // 设置串口参数
            serialPort.setSerialPortParams(baudRate, dataBits, stopBits, parity);

            // 获取输入输出流
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();

            // 添加监听器
            serialPort.addEventListener(new SerialPortListener());
            serialPort.notifyOnDataAvailable(true);

            log.info("成功连接到设备 {} 的串口 {}", device.getName(), portName);
            return true;
        } catch (NoSuchPortException e) {
            log.error("串口 {} 不存在", portName);
        } catch (PortInUseException e) {
            log.error("串口 {} 已被占用", portName);
        } catch (UnsupportedCommOperationException e) {
            log.error("不支持的串口操作");
        } catch (IOException e) {
            log.error("获取串口 {} 的输入输出流失败", portName);
        } catch (TooManyListenersException e) {
            log.error("添加串口监听器失败");
        }
        return false;
    }

    @Override
    public void disconnect() {
        if (serialPort != null) {
            serialPort.removeEventListener();
            serialPort.close();
            serialPort = null;
        }

        try {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
        } catch (IOException e) {
            log.error("关闭串口流时出错: {}", e.getMessage());
        }

        log.info("已断开设备 {} 的串口连接", device.getName());
    }

    @Override
    public boolean send(String data) {
        if (!isConnected()) {
            log.error("发送失败：设备未连接");
            return false;
        }

        try {
            outputStream.write(data.getBytes());
            outputStream.flush();
            log.debug("成功发送数据到设备 {} 的串口: {}", device.getName(), data);
            return true;
        } catch (IOException e) {
            log.error("发送数据到设备 {} 的串口失败: {}", device.getName(), e.getMessage());
            return false;
        }
    }

    @Override
    public String receive() {
        try {
            String message = receivedMessages.poll(10, TimeUnit.SECONDS);
            if (message != null) {
                log.debug("从设备 {} 的串口接收到数据: {}", device.getName(), message);
            }
            return message;
        } catch (InterruptedException e) {
            log.error("接收数据被中断: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public boolean isConnected() {
        return serialPort != null && inputStream != null && outputStream != null;
    }

    /**
     * 串口监听器
     * 负责接收串口数据并进行处理
     */
    private class SerialPortListener implements SerialPortEventListener {

        @Override
        public void serialEvent(SerialPortEvent event) {
            if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
                try {
                    byte[] readBuffer = new byte[1024];
                    int numBytes = inputStream.read(readBuffer);

                    if (numBytes > 0) {
                        String data = new String(readBuffer, 0, numBytes);

                        // 使用统一的处理方法处理接收到的数据
                        String response = processReceivedData(data);

                        // 如果需要回应，则发送回应
                        if (response != null) {
                            send(response);
                        }
                    }
                } catch (IOException e) {
                    log.error("读取串口数据时出错: {}", e.getMessage());
                }
            }
        }
    }
}
