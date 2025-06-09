package com.hl7.client.interfaces.window;

import com.hl7.client.application.DeviceManager;
import com.hl7.client.application.MessageProcessor;
import com.hl7.client.domain.model.Device;
import com.hl7.client.domain.model.DeviceStatus;
import com.hl7.client.domain.model.Message;
import com.hl7.client.infrastructure.event.DeviceStatusChangeEvent;
import com.hl7.client.infrastructure.util.UIHelper;
import com.hl7.client.infrastructure.factory.DeviceAdapterCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;

/**
 * 主窗口类
 * 提供设备管理和消息处理的用户界面
 */
@Slf4j
@EnableScheduling
public class MainFrame extends JFrame implements ApplicationListener<DeviceStatusChangeEvent> {

    // 自动连接相关常量
    private static final String AUTO_CONNECT_ON_TEXT = "关闭自动连接";
    private static final String AUTO_CONNECT_OFF_TEXT = "开启自动连接";

    // 状态指示符常量
    private static final String STATUS_INDICATOR_ON = "●";  // 实心圆点表示开启
    private static final String STATUS_INDICATOR_OFF = "○"; // 空心圆点表示关闭

    // 自动连接状态，默认为开启
    private volatile boolean isAutoConnectEnabled = true;

    private final DeviceManager deviceManager;
    private final MessageProcessor messageProcessor;
    private final DeviceAdapterCache adapterCache;

    // 设备管理相关组件
    private JTable deviceTable;
    private DefaultTableModel deviceTableModel;
    private JButton addDeviceButton;
    private JButton editDeviceButton;
    private JButton connectButton;
    private JButton autoConnectToggleButton;
    private JButton disconnectButton;
    private JButton removeDeviceButton;

    // 消息处理相关组件
    private JTable messageTable;
    private DefaultTableModel messageTableModel;
    private JButton receiveButton;
    private JButton processButton;
    private JButton retryButton;
    private JTextArea messageContentArea;

    // 状态相关组件
    private JLabel statusLabel;
    private Timer refreshTimer;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 字体大小设置
    private static final int FONT_SIZE = 16;

    /**
     * 构造函数
     *
     * @param deviceManager 设备管理器
     * @param messageProcessor 消息处理器
     * @param adapterCache 设备适配器缓存
     */
    public MainFrame(DeviceManager deviceManager,
                     MessageProcessor messageProcessor,
                     DeviceAdapterCache adapterCache) {
        this.deviceManager = deviceManager;
        this.messageProcessor = messageProcessor;
        this.adapterCache = adapterCache;

        // 设置全局字体大小
        UIHelper.setGlobalFontSize(FONT_SIZE);

        initUI();
        setupRefreshTimer();
    }

    /**
     * 初始化UI
     */
    private void initUI() {
        setTitle("HL7客户端");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 设置菜单
        setupMenu();

        // 创建分割面板
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        JSplitPane bottomSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // 设备管理面板
        JPanel devicePanel = createDevicePanel();

        // 消息管理面板
        JPanel messagePanel = createMessagePanel();

        // 消息内容面板
        JPanel contentPanel = createContentPanel();

        // 设置分割面板
        bottomSplitPane.setLeftComponent(messagePanel);
        bottomSplitPane.setRightComponent(contentPanel);
        bottomSplitPane.setDividerLocation(500);

        mainSplitPane.setTopComponent(devicePanel);
        mainSplitPane.setBottomComponent(bottomSplitPane);
        mainSplitPane.setDividerLocation(250);

        // 状态栏
        statusLabel = new JLabel("就绪");
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEtchedBorder());
        statusPanel.add(statusLabel, BorderLayout.WEST);

        // 添加到主窗口
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainSplitPane, BorderLayout.CENTER);
        getContentPane().add(statusPanel, BorderLayout.SOUTH);

        // 窗口关闭时的处理
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // 断开所有设备连接
                for (Map.Entry<String, Device> entry : deviceManager.getAllDevices().entrySet()) {
                    if ("CONNECTED".equals(entry.getValue().getStatus())) {
                        deviceManager.disconnectDevice(entry.getKey());
                    }
                }

                // 清理所有适配器缓存
                adapterCache.clearAllAdapters();
                log.info("已清理所有设备适配器缓存");

                // 停止定时器
                if (refreshTimer != null && refreshTimer.isRunning()) {
                    refreshTimer.stop();
                }
            }
        });
    }

    /**
     * 设置菜单
     */
    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();

        // 文件菜单
        JMenu fileMenu = new JMenu("文件");
        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        // 设备菜单
        JMenu deviceMenu = new JMenu("设备");
        JMenuItem refreshItem = new JMenuItem("刷新设备状态");
        refreshItem.addActionListener(e -> refreshDeviceList());
        deviceMenu.add(refreshItem);

        // 添加自动连接菜单项
        JMenuItem autoConnectMenuItem = new JMenuItem();
        updateAutoConnectMenuItem(autoConnectMenuItem);
        autoConnectMenuItem.addActionListener(e -> {
            toggleAutoConnect(e);
            updateAutoConnectMenuItem(autoConnectMenuItem);
        });
        deviceMenu.addSeparator();
        deviceMenu.add(autoConnectMenuItem);

        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助");
        JMenuItem configGuideItem = new JMenuItem("设备配置指南");
        configGuideItem.addActionListener(e -> showDeviceConfigGuide());
        helpMenu.add(configGuideItem);

        JMenuItem aboutItem = new JMenuItem("关于");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
            "HL7客户端 - 新架构版\n版本: 1.0.0",
            "关于", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(deviceMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    /**
     * 更新菜单中的自动连接项
     *
     * @param menuItem 菜单项
     */
    private void updateAutoConnectMenuItem(JMenuItem menuItem) {
        String text = isAutoConnectEnabled ? AUTO_CONNECT_ON_TEXT : AUTO_CONNECT_OFF_TEXT;
        String indicator = isAutoConnectEnabled ? STATUS_INDICATOR_ON : STATUS_INDICATOR_OFF;
        menuItem.setText(indicator + " " + text);
    }

    /**
     * 创建设备管理面板
     *
     * @return 设备管理面板
     */
    private JPanel createDevicePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("设备管理"));

        // 创建表格
        String[] columnNames = {"ID", "名称", "类型", "连接参数", "状态"};
        deviceTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        deviceTable = new JTable(deviceTableModel);
        deviceTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 应用字体设置
        UIHelper.applyFontToTable(deviceTable, FONT_SIZE);

        // 应用表格渲染器
        applyTableRenderers();

        // 按钮面板
        JPanel buttonPanel = new JPanel();
        addDeviceButton = new JButton("添加设备");
        editDeviceButton = new JButton("编辑设备");
        connectButton = new JButton("连接");

        // 初始化自动连接切换按钮 - 保持与其他按钮一致的外观
        autoConnectToggleButton = new JButton();
        updateAutoConnectButtonAppearance();

        disconnectButton = new JButton("断开");
        removeDeviceButton = new JButton("移除设备");

        // 应用字体设置
        UIHelper.applyFontToComponent(addDeviceButton, FONT_SIZE);
        UIHelper.applyFontToComponent(editDeviceButton, FONT_SIZE);
        UIHelper.applyFontToComponent(connectButton, FONT_SIZE);
        UIHelper.applyFontToComponent(autoConnectToggleButton, FONT_SIZE);
        UIHelper.applyFontToComponent(disconnectButton, FONT_SIZE);
        UIHelper.applyFontToComponent(removeDeviceButton, FONT_SIZE);

        buttonPanel.add(addDeviceButton);
        buttonPanel.add(editDeviceButton);
        buttonPanel.add(connectButton);
        buttonPanel.add(autoConnectToggleButton);
        buttonPanel.add(disconnectButton);
        buttonPanel.add(removeDeviceButton);

        // 添加到面板
        panel.add(new JScrollPane(deviceTable), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // 添加事件处理
        addDeviceButton.addActionListener(this::addDevice);
        editDeviceButton.addActionListener(this::editDevice);
        connectButton.addActionListener(this::connectDevice);
        autoConnectToggleButton.addActionListener(this::toggleAutoConnect);
        disconnectButton.addActionListener(this::disconnectDevice);
        removeDeviceButton.addActionListener(this::removeDevice);

        // 刷新设备列表
        refreshDeviceList();

        return panel;
    }

    /**
     * 切换自动连接状态
     *
     * @param e 事件
     */
    private void toggleAutoConnect(ActionEvent e) {
        try {
            // 切换状态
            isAutoConnectEnabled = !isAutoConnectEnabled;

            // 更新按钮外观
            updateAutoConnectButtonAppearance();

            // 记录状态变更
            String statusMessage = isAutoConnectEnabled ? "自动连接已开启" : "自动连接已关闭";
            log.info("用户{}，当前状态: {}", statusMessage, isAutoConnectEnabled);

            // 更新状态栏显示
            updateStatus();

            // 显示状态变更提示（温和的状态栏提示）
            showAutoConnectStatusNotification();

        } catch (Exception ex) {
            log.error("切换自动连接状态时出错: {}", ex.getMessage(), ex);
            JOptionPane.showMessageDialog(this,
                "切换自动连接状态时出错: " + ex.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 显示自动连接状态通知
     */
    private void showAutoConnectStatusNotification() {
        String notificationMessage = isAutoConnectEnabled ?
            "自动连接功能已开启，系统将定时尝试连接断开的设备" :
            "自动连接功能已关闭，需要手动连接设备";

        // 使用状态栏显示提示，颜色变化提供视觉反馈
        statusLabel.setText(notificationMessage);
        statusLabel.setForeground(isAutoConnectEnabled ? new Color(0, 128, 0) : new Color(255, 140, 0)); // 绿色或橙色

        // 3秒后恢复正常状态栏显示
        Timer resetStatusTimer = new Timer(3000, resetEvent -> updateStatus());
        resetStatusTimer.setRepeats(false);
        resetStatusTimer.start();
    }

    /**
     * 更新自动连接按钮的外观和文字
     * 保持与其他按钮一致的UI风格，仅通过文字变化体现状态
     */
    private void updateAutoConnectButtonAppearance() {
        SwingUtilities.invokeLater(() -> {
            // 根据状态设置不同的文字
            if (isAutoConnectEnabled) {
                // 当前是开启状态，按钮显示"关闭"选项
                autoConnectToggleButton.setText(AUTO_CONNECT_ON_TEXT);
                autoConnectToggleButton.setToolTipText("点击关闭自动连接功能。当前状态：已开启");
            } else {
                // 当前是关闭状态，按钮显示"开启"选项
                autoConnectToggleButton.setText(AUTO_CONNECT_OFF_TEXT);
                autoConnectToggleButton.setToolTipText("点击开启自动连接功能。当前状态：已关闭");
            }

            // 移除自定义颜色设置，让按钮保持与其他按钮一致的外观
            // 使用系统默认的UI外观
            autoConnectToggleButton.setBackground(null);
            autoConnectToggleButton.setForeground(null);

            // 可选：添加细微的视觉提示（通过字体样式）
            Font currentFont = autoConnectToggleButton.getFont();
            if (isAutoConnectEnabled) {
                // 开启状态使用普通字体
                autoConnectToggleButton.setFont(currentFont.deriveFont(Font.PLAIN));
            } else {
                // 关闭状态使用斜体，提供subtle的视觉区别
                autoConnectToggleButton.setFont(currentFont.deriveFont(Font.ITALIC));
            }

            // 确保按钮重绘
            autoConnectToggleButton.repaint();
        });
    }

    /**
     * 获取当前自动连接状态
     *
     * @return 是否启用自动连接
     */
    public boolean isAutoConnectEnabled() {
        return isAutoConnectEnabled;
    }

    /**
     * 设置自动连接状态（编程方式调用）
     *
     * @param enabled 是否启用自动连接
     */
    public void setAutoConnectEnabled(boolean enabled) {
        if (this.isAutoConnectEnabled != enabled) {
            this.isAutoConnectEnabled = enabled;
            updateAutoConnectButtonAppearance();
            updateStatus();
            log.info("程序设置自动连接状态为: {}", enabled);

            // 同步更新菜单项（如果需要）
            JMenuBar menuBar = getJMenuBar();
            if (menuBar != null) {
                // 查找并更新设备菜单中的自动连接项
                for (int i = 0; i < menuBar.getMenuCount(); i++) {
                    JMenu menu = menuBar.getMenu(i);
                    if ("设备".equals(menu.getText())) {
                        for (int j = 0; j < menu.getItemCount(); j++) {
                            JMenuItem item = menu.getItem(j);
                            if (item != null && (item.getText().contains("自动连接") ||
                                item.getText().contains(STATUS_INDICATOR_ON) ||
                                item.getText().contains(STATUS_INDICATOR_OFF))) {
                                updateAutoConnectMenuItem(item);
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * 创建消息管理面板
     *
     * @return 消息管理面板
     */
    private JPanel createMessagePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("消息管理"));

        // 创建表格
        String[] columnNames = {"ID", "设备", "类型", "接收时间", "状态"};
        messageTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        messageTable = new JTable(messageTableModel);
        messageTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 应用字体设置
        UIHelper.applyFontToTable(messageTable, FONT_SIZE);

        // 选择事件
        messageTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && messageTable.getSelectedRow() != -1) {
                String messageId = (String) messageTable.getValueAt(messageTable.getSelectedRow(), 0);
                displayMessageContent(messageId);
            }
        });

        // 按钮面板
        JPanel buttonPanel = new JPanel();
        receiveButton = new JButton("接收消息");
        processButton = new JButton("处理消息");
        retryButton = new JButton("重试失败");

        // 应用字体设置
        UIHelper.applyFontToComponent(receiveButton, FONT_SIZE);
        UIHelper.applyFontToComponent(processButton, FONT_SIZE);
        UIHelper.applyFontToComponent(retryButton, FONT_SIZE);

        buttonPanel.add(receiveButton);
        buttonPanel.add(processButton);
        buttonPanel.add(retryButton);

        // 添加到面板
        panel.add(new JScrollPane(messageTable), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // 添加事件处理
        receiveButton.addActionListener(this::receiveMessage);
        processButton.addActionListener(this::processMessage);
        retryButton.addActionListener(this::retryFailedMessage);

        // 刷新消息列表
        refreshMessageList();

        return panel;
    }

    /**
     * 创建消息内容面板
     *
     * @return 消息内容面板
     */
    private JPanel createContentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("消息内容"));

        messageContentArea = new JTextArea();
        messageContentArea.setEditable(false);

        // 应用字体设置
        UIHelper.applyFontToComponent(messageContentArea, FONT_SIZE);

        panel.add(new JScrollPane(messageContentArea), BorderLayout.CENTER);

        return panel;
    }

    /**
     * 设置定时刷新
     */
    private void setupRefreshTimer() {
        refreshTimer = new Timer(5000, e -> {
            refreshDeviceList();
            refreshMessageList();
            updateStatus();
        });
        refreshTimer.start();
    }

    /**
     * 刷新设备列表
     */
    private void refreshDeviceList() {
        try {
            // 清空表格
            deviceTableModel.setRowCount(0);

            // 添加设备，不主动检查状态（依靠定时任务和事件驱动）
            for (Map.Entry<String, Device> entry : deviceManager.getAllDevices().entrySet()) {
                Device device = entry.getValue();

                Vector<Object> row = new Vector<>();
                row.add(device.getId());
                row.add(device.getName());
                row.add(UIHelper.getLocalizedConnectionType(device.getConnectionType()));
                row.add(device.getConnectionParams());
                row.add(UIHelper.getLocalizedNetworkMode(device.getStatus()));

                deviceTableModel.addRow(row);
            }
        } catch (Exception e) {
            log.error("刷新设备列表时出错: {}", e.getMessage());
            JOptionPane.showMessageDialog(this, "刷新设备列表时出错: " + e.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 刷新消息列表
     */
    private void refreshMessageList() {
        try {
            // 清空表格
            messageTableModel.setRowCount(0);

            // 添加处理过的消息
            for (Map.Entry<String, Message> entry : messageProcessor.getProcessedMessages().entrySet()) {
                Message message = entry.getValue();

                Vector<Object> row = new Vector<>();
                row.add(message.getId());
                row.add(message.getDeviceId());
                row.add(message.getMessageType());
                row.add(message.getReceivedTime().format(DATE_FORMATTER));
                row.add(message.getStatus());

                messageTableModel.addRow(row);
            }
        } catch (Exception e) {
            log.error("刷新消息列表时出错: {}", e.getMessage());
            JOptionPane.showMessageDialog(this, "刷新消息列表时出错: " + e.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 更新状态栏
     */
    private void updateStatus() {
        int deviceCount = deviceManager.getAllDevices().size();
        int connectedCount = 0;

        for (Device device : deviceManager.getAllDevices().values()) {
            if (Objects.equals(device.getStatus(), DeviceStatus.CONNECTED)) {
                connectedCount++;
            }
        }

        int messageCount = messageProcessor.getProcessedMessages().size();
        int queueSize = messageProcessor.getQueueSize();

        // 添加自动连接状态显示，使用简洁的指示符
        String autoConnectStatus = isAutoConnectEnabled ?
            "自动连接:" + STATUS_INDICATOR_ON + "开启" :
            "自动连接:" + STATUS_INDICATOR_OFF + "关闭";

        statusLabel.setText(String.format("设备总数: %d | 已连接: %d | 消息总数: %d | 队列中: %d | %s",
            deviceCount, connectedCount, messageCount, queueSize, autoConnectStatus));

        // 恢复正常颜色
        statusLabel.setForeground(Color.BLACK);
    }

    /**
     * 显示消息内容
     *
     * @param messageId 消息ID
     */
    private void displayMessageContent(String messageId) {
        try {
            Message message = messageProcessor.getProcessedMessages().get(messageId);
            if (message != null) {
                StringBuilder content = new StringBuilder();
                content.append("消息ID: ").append(message.getId()).append("\n");
                content.append("设备ID: ").append(message.getDeviceId()).append("\n");
                content.append("消息类型: ").append(message.getMessageType()).append("\n");
                content.append("接收时间: ").append(message.getReceivedTime().format(DATE_FORMATTER)).append("\n");
                content.append("状态: ").append(message.getStatus()).append("\n");
                content.append("处理结果: ").append(message.getProcessResult()).append("\n");
                content.append("原始内容: \n").append(message.getRawContent());

                messageContentArea.setText(content.toString());
            } else {
                messageContentArea.setText("");
            }
        } catch (Exception e) {
            log.error("显示消息内容时出错: {}", e.getMessage());
            messageContentArea.setText("显示消息内容时出错: " + e.getMessage());
        }
    }

    // ... 其他设备操作方法保持不变，这里省略以节省篇幅 ...

    /**
     * 添加设备
     */
    private void addDevice(ActionEvent e) {
        try {
            DeviceDialog dialog = new DeviceDialog(this, "添加设备");

            if (dialog.showDialog()) {
                Device device = dialog.createDeviceFromForm();
                deviceManager.registerDevice(device);
                refreshDeviceList();
                updateStatus();
                JOptionPane.showMessageDialog(this, "设备添加成功", "成功", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            log.error("添加设备时出错: {}", ex.getMessage());
            JOptionPane.showMessageDialog(this, "添加设备时出错: " + ex.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 编辑设备
     */
    private void editDevice(ActionEvent e) {
        try {
            int selectedRow = deviceTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "请先选择一个设备", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String deviceId = (String) deviceTable.getValueAt(selectedRow, 0);
            Device device = deviceManager.getDevice(deviceId);

            if (device == null) {
                JOptionPane.showMessageDialog(this, "无法找到选中的设备", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            DeviceDialog dialog = new DeviceDialog(this, "编辑设备");
            dialog.setDeviceInfo(device);

            if (dialog.showDialog()) {
                Device updatedDevice = Device.builder()
                    .id(device.getId())
                    .name(dialog.createDeviceFromForm().getName())
                    .model(dialog.createDeviceFromForm().getModel())
                    .manufacturer(dialog.createDeviceFromForm().getManufacturer())
                    .connectionType(dialog.createDeviceFromForm().getConnectionType())
                    .connectionParams(dialog.createDeviceFromForm().getConnectionParams())
                    .status(device.getStatus())
                    .build();

                deviceManager.registerDevice(updatedDevice);
                refreshDeviceList();
                updateStatus();
                JOptionPane.showMessageDialog(this, "设备信息已更新", "成功", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            log.error("编辑设备时出错: {}", ex.getMessage());
            JOptionPane.showMessageDialog(this, "编辑设备时出错: " + ex.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 连接设备
     */
    private void connectDevice(ActionEvent e) {
        try {
            int selectedRow = deviceTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "请先选择一个设备", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String deviceId = (String) deviceTable.getValueAt(selectedRow, 0);
            boolean connected = deviceManager.connectDevice(deviceId);

            if (connected) {
                JOptionPane.showMessageDialog(this, "设备连接成功", "成功", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "设备连接失败", "错误", JOptionPane.ERROR_MESSAGE);
            }

            refreshDeviceList();
            updateStatus();
        } catch (Exception ex) {
            log.error("连接设备时出错: {}", ex.getMessage());
            JOptionPane.showMessageDialog(this, "连接设备时出错: " + ex.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 断开设备连接
     */
    private void disconnectDevice(ActionEvent e) {
        try {
            int selectedRow = deviceTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "请先选择一个设备", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String deviceId = (String) deviceTable.getValueAt(selectedRow, 0);
            deviceManager.disconnectDevice(deviceId);

            refreshDeviceList();
            updateStatus();
            JOptionPane.showMessageDialog(this, "设备已断开连接", "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            log.error("断开设备连接时出错: {}", ex.getMessage());
            JOptionPane.showMessageDialog(this, "断开设备连接时出错: " + ex.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 移除设备
     */
    private void removeDevice(ActionEvent e) {
        try {
            int selectedRow = deviceTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "请先选择一个设备", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String deviceId = (String) deviceTable.getValueAt(selectedRow, 0);
            int result = JOptionPane.showConfirmDialog(this, "确定要移除此设备吗？",
                "确认", JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                boolean removed = deviceManager.removeDevice(deviceId);

                if (removed) {
                    JOptionPane.showMessageDialog(this, "设备已移除", "成功", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "设备移除失败", "错误", JOptionPane.ERROR_MESSAGE);
                }

                refreshDeviceList();
                updateStatus();
            }
        } catch (Exception ex) {
            log.error("移除设备时出错: {}", ex.getMessage());
            JOptionPane.showMessageDialog(this, "移除设备时出错: " + ex.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ... 消息处理方法保持不变 ...

    /**
     * 接收消息
     */
    private void receiveMessage(ActionEvent e) {
        try {
            int selectedRow = deviceTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "请先选择一个设备", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            final String deviceId = (String) deviceTable.getValueAt(selectedRow, 0);

            // 禁用接收按钮，防止重复点击
            receiveButton.setEnabled(false);

            // 更新状态提示
            statusLabel.setText("正在接收消息...");
            statusLabel.setForeground(Color.BLUE);

            // 使用SwingWorker实现异步接收消息
            SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
                @Override
                protected Message doInBackground() throws Exception {
                    return messageProcessor.receiveMessage(deviceId);
                }

                @Override
                protected void done() {
                    try {
                        Message message = get();
                        if (message != null) {
                            // 成功接收
                            JOptionPane.showMessageDialog(MainFrame.this,
                                "成功接收消息", "成功", JOptionPane.INFORMATION_MESSAGE);
                            refreshMessageList();
                        } else {
                            // 未接收到消息
                            JOptionPane.showMessageDialog(MainFrame.this,
                                "没有接收到消息", "提示", JOptionPane.WARNING_MESSAGE);
                        }
                    } catch (Exception ex) {
                        log.error("异步接收消息时出错: {}", ex.getMessage());
                        JOptionPane.showMessageDialog(MainFrame.this,
                            "接收消息时出错: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        // 恢复按钮状态
                        receiveButton.setEnabled(true);
                        // 更新界面
                        updateStatus();
                    }
                }
            };

            // 启动异步任务
            worker.execute();

        } catch (Exception ex) {
            log.error("接收消息时出错: {}", ex.getMessage());
            JOptionPane.showMessageDialog(this, "接收消息时出错: " + ex.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
            // 恢复按钮状态
            receiveButton.setEnabled(true);
            updateStatus();
        }
    }

    /**
     * 处理消息
     */
    private void processMessage(ActionEvent e) {
        try {
            // 禁用处理按钮
            processButton.setEnabled(false);

            // 更新状态
            statusLabel.setText("正在处理消息队列...");
            statusLabel.setForeground(Color.BLUE);

            // 使用SwingWorker异步处理消息队列
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    messageProcessor.processMessageQueue();
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get(); // 检查是否有异常
                        JOptionPane.showMessageDialog(MainFrame.this,
                            "消息队列处理已完成", "成功", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        log.error("异步处理消息队列时出错: {}", ex.getMessage());
                        JOptionPane.showMessageDialog(MainFrame.this,
                            "处理消息时出错: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        // 恢复按钮状态
                        processButton.setEnabled(true);
                        // 刷新消息列表和状态
                        refreshMessageList();
                        updateStatus();
                    }
                }
            };

            // 启动异步任务
            worker.execute();

        } catch (Exception ex) {
            log.error("处理消息时出错: {}", ex.getMessage());
            JOptionPane.showMessageDialog(this, "处理消息时出错: " + ex.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
            // 恢复按钮状态
            processButton.setEnabled(true);
            updateStatus();
        }
    }

    /**
     * 重试失败的消息
     */
    private void retryFailedMessage(ActionEvent e) {
        try {
            // 禁用重试按钮
            retryButton.setEnabled(false);

            // 更新状态
            statusLabel.setText("正在重试失败消息...");
            statusLabel.setForeground(Color.BLUE);

            // 使用SwingWorker异步重试失败消息
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    messageProcessor.retryFailedMessages();
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get(); // 检查是否有异常
                        JOptionPane.showMessageDialog(MainFrame.this,
                            "失败消息重试已完成", "成功", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        log.error("异步重试失败消息时出错: {}", ex.getMessage());
                        JOptionPane.showMessageDialog(MainFrame.this,
                            "重试失败消息时出错: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        // 恢复按钮状态
                        retryButton.setEnabled(true);
                        // 刷新消息列表和状态
                        refreshMessageList();
                        updateStatus();
                    }
                }
            };

            // 启动异步任务
            worker.execute();

        } catch (Exception ex) {
            log.error("重试失败消息时出错: {}", ex.getMessage());
            JOptionPane.showMessageDialog(this, "重试失败消息时出错: " + ex.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
            // 恢复按钮状态
            retryButton.setEnabled(true);
            updateStatus();
        }
    }

    /**
     * 显示设备配置指南
     */
    private void showDeviceConfigGuide() {
        try {
            java.awt.Desktop.getDesktop().browse(
                getClass().getResource("/static/help/device_config_guide.html").toURI());
        } catch (Exception ex) {
            log.error("打开设备配置指南时出错: {}", ex.getMessage());
            JOptionPane.showMessageDialog(this, "无法打开设备配置指南: " + ex.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 处理设备状态变化事件
     */
    @Override
    public void onApplicationEvent(DeviceStatusChangeEvent event) {
        log.info("收到设备状态变化事件: 设备 {} 状态从 {} 变为 {}",
            event.getDevice().getName(), event.getOldStatus(), event.getNewStatus());

        // 使用SwingUtilities.invokeLater确保在EDT线程中更新UI
        SwingUtilities.invokeLater(this::updateDeviceTable);
    }

    /**
     * 更新设备表格
     */
    public void updateDeviceTable() {
        try {
            // 记录当前选中的行
            int selectedRow = deviceTable.getSelectedRow();
            String selectedDeviceId = null;
            if (selectedRow >= 0) {
                selectedDeviceId = (String) deviceTable.getValueAt(selectedRow, 0);
            }

            // 清空表格
            deviceTableModel.setRowCount(0);

            // 重新加载设备数据
            for (Device device : deviceManager.getAllDevices().values()) {
                boolean connected = Objects.equals(device.getStatus(), DeviceStatus.CONNECTED);
                Object[] row = new Object[]{
                    device.getId(),
                    device.getName(),
                    UIHelper.getLocalizedConnectionType(device.getConnectionType()),
                    device.getConnectionParams(),
                    connected ? "已连接" : "未连接"
                };
                deviceTableModel.addRow(row);

                // 如果当前设备是之前选择的设备，重新选中它
                if (device.getId().equals(selectedDeviceId)) {
                    deviceTable.setRowSelectionInterval(deviceTableModel.getRowCount() - 1,
                        deviceTableModel.getRowCount() - 1);
                }
            }

            // 应用表格渲染，让状态列显示颜色
            applyTableRenderers();

            // 重绘表格
            deviceTable.repaint();

        } catch (Exception e) {
            log.error("更新设备表格时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 应用表格渲染器
     */
    private void applyTableRenderers() {
        // 为状态列添加自定义渲染器
        deviceTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

                if ("已连接".equals(value)) {
                    setForeground(new Color(0, 128, 0)); // 绿色
                } else {
                    setForeground(Color.RED);
                }

                return c;
            }
        });
    }

    /**
     * 定时刷新设备状态
     */
    @Scheduled(fixedDelay = 10000) // 每10秒刷新一次
    public void refreshDeviceStatus() {
        // 使用SwingUtilities.invokeLater确保在EDT线程中更新UI
        SwingUtilities.invokeLater(() -> {
            try {
                log.debug("开始定时刷新设备状态");

                // 检查所有设备的状态
                for (Device device : deviceManager.getAllDevices().values()) {
                    // 刷新设备状态，现在包含防抖和验证机制
                    deviceManager.checkDeviceStatus(device.getId());
                }

                // 更新UI
                updateDeviceTable();

                log.debug("设备状态刷新完成");
            } catch (Exception e) {
                log.error("刷新设备状态时出错: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * 自动连接设备的定时任务
     * 只有在自动连接开启时才执行
     * 每3秒刷新一次
     */
    @Scheduled(fixedDelay = 3000)
    public void autoConnectDevices() {
        // 只有在自动连接开启时才执行
        if (!isAutoConnectEnabled) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                log.debug("开始自动连接检查");

                // 获取所有未连接的设备并尝试连接
                long disconnectedCount = deviceManager.getAllDevices().values().stream()
                    .filter(device -> !Objects.equals(DeviceStatus.CONNECTED, device.getStatus()))
                    .peek(device -> {
                        log.debug("尝试自动连接设备: {}", device.getName());
                        deviceManager.connectDevice(device.getId());
                    })
                    .count();

                if (disconnectedCount > 0) {
                    log.debug("自动连接检查完成，尝试连接了 {} 个设备", disconnectedCount);
                }
            } catch (Exception e) {
                log.error("自动连接设备时出错: {}", e.getMessage(), e);
            }
        });
    }
}
