package com.hl7.client.interfaces.window;

import com.hl7.client.domain.model.Device;
import com.hl7.client.infrastructure.util.UIHelper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 设备配置面板
 * 根据不同的连接类型显示不同的参数配置界面
 */
@Slf4j
public class DeviceConfigPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(DeviceConfigPanel.class.getName());

    // 连接类型值
    private static final String[] CONNECTION_TYPE_VALUES = {"NETWORK", "SERIAL", "FILE", "DATABASE"};
    // 连接类型显示文本
    private static final String[] CONNECTION_TYPE_LABELS = {"网络连接", "串口连接", "文件连接", "数据库连接"};

    // 字体大小设置
    private static final int FONT_SIZE = 16;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel paramPanel = new JPanel(cardLayout);
    private final Map<String, ConnectionParamPanel> paramPanels = new HashMap<>();

    @Getter
    private final JTextField nameField = new JTextField(20);

    @Getter
    private final JTextField modelField = new JTextField(20);

    @Getter
    private final JTextField manufacturerField = new JTextField(20);

    @Getter
    private final JComboBox<String> connectionTypeCombo = new JComboBox<>(CONNECTION_TYPE_LABELS);

    /**
     * 构造函数
     */
    public DeviceConfigPanel() {
        setLayout(new BorderLayout(5, 5));

        // 创建基本信息面板
        JPanel basicInfoPanel = createBasicInfoPanel();
        add(basicInfoPanel, BorderLayout.NORTH);

        // 创建连接参数面板
        initParamPanels();
        add(paramPanel, BorderLayout.CENTER);

        // 添加连接类型切换监听器
        connectionTypeCombo.addActionListener(e -> {
            String selectedLabel = (String) connectionTypeCombo.getSelectedItem();
            if (selectedLabel != null) {
                String selectedType = getConnectionTypeFromLabel(selectedLabel);
                cardLayout.show(paramPanel, selectedType);
            }
        });

        // 默认显示网络连接参数面板
        cardLayout.show(paramPanel, "NETWORK");

        // 设置工具提示
        nameField.setToolTipText("输入设备的名称");
        modelField.setToolTipText("输入设备的型号");
        manufacturerField.setToolTipText("输入设备的厂商");
        connectionTypeCombo.setToolTipText("选择连接类型");
    }

    /**
     * 创建基本信息面板
     *
     * @return 基本信息面板
     */
    private JPanel createBasicInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("基本信息"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 设备名称
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel nameLabel = new JLabel("设备名称:", SwingConstants.RIGHT);
        UIHelper.applyFontToComponent(nameLabel, FONT_SIZE);
        panel.add(nameLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        UIHelper.applyFontToComponent(nameField, FONT_SIZE);
        panel.add(nameField, gbc);

        // 设备型号
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        JLabel modelLabel = new JLabel("设备型号:", SwingConstants.RIGHT);
        UIHelper.applyFontToComponent(modelLabel, FONT_SIZE);
        panel.add(modelLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        UIHelper.applyFontToComponent(modelField, FONT_SIZE);
        panel.add(modelField, gbc);

        // 设备厂商
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        JLabel manufacturerLabel = new JLabel("设备厂商:", SwingConstants.RIGHT);
        UIHelper.applyFontToComponent(manufacturerLabel, FONT_SIZE);
        panel.add(manufacturerLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        UIHelper.applyFontToComponent(manufacturerField, FONT_SIZE);
        panel.add(manufacturerField, gbc);

        // 连接类型
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        JLabel connectionTypeLabel = new JLabel("连接类型:", SwingConstants.RIGHT);
        UIHelper.applyFontToComponent(connectionTypeLabel, FONT_SIZE);
        panel.add(connectionTypeLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        UIHelper.applyFontToComponent(connectionTypeCombo, FONT_SIZE);
        panel.add(connectionTypeCombo, gbc);

        return panel;
    }

    /**
     * 初始化各种连接参数面板
     */
    private void initParamPanels() {
        // 网络连接参数面板
        NetworkParamPanel networkPanel = new NetworkParamPanel();
        paramPanels.put("NETWORK", networkPanel);
        paramPanel.add(networkPanel, "NETWORK");

        // 串口连接参数面板
        SerialPortParamPanel serialPanel = new SerialPortParamPanel();
        paramPanels.put("SERIAL", serialPanel);
        paramPanel.add(serialPanel, "SERIAL");

        // 文件连接参数面板
        FileParamPanel filePanel = new FileParamPanel();
        paramPanels.put("FILE", filePanel);
        paramPanel.add(filePanel, "FILE");

        // 数据库连接参数面板
        DatabaseParamPanel databasePanel = new DatabaseParamPanel();
        paramPanels.put("DATABASE", databasePanel);
        paramPanel.add(databasePanel, "DATABASE");
    }

    /**
     * 获取当前选择的连接类型
     *
     * @return 连接类型
     */
    public String getSelectedConnectionType() {
        String selectedLabel = (String) connectionTypeCombo.getSelectedItem();
        return getConnectionTypeFromLabel(selectedLabel);
    }

    /**
     * 根据显示文本获取连接类型值
     *
     * @param label 显示文本
     * @return 连接类型值
     */
    private String getConnectionTypeFromLabel(String label) {
        for (int i = 0; i < CONNECTION_TYPE_LABELS.length; i++) {
            if (CONNECTION_TYPE_LABELS[i].equals(label)) {
                return CONNECTION_TYPE_VALUES[i];
            }
        }
        return "NETWORK"; // 默认返回网络连接
    }

    /**
     * 根据连接类型值设置下拉框选中项
     *
     * @param type 连接类型值
     */
    public void setConnectionType(String type) {
        for (int i = 0; i < CONNECTION_TYPE_VALUES.length; i++) {
            if (CONNECTION_TYPE_VALUES[i].equals(type)) {
                connectionTypeCombo.setSelectedItem(CONNECTION_TYPE_LABELS[i]);
                break;
            }
        }
    }

    /**
     * 获取当前连接参数
     *
     * @return 连接参数字符串
     */
    public String getConnectionParams() {
        String type = getSelectedConnectionType();
        ConnectionParamPanel panel = paramPanels.get(type);
        return panel != null ? panel.getConnectionParams() : "";
    }

    /**
     * 设置设备信息到表单
     *
     * @param device 设备对象
     */
    public void setDeviceInfo(Device device) {
        if (device == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            nameField.setText(device.getName());
            modelField.setText(device.getModel());
            manufacturerField.setText(device.getManufacturer());
            connectionTypeCombo.setSelectedItem(getConnectionTypeFromLabel(device.getConnectionType()));

            // 设置连接参数
            String type = getSelectedConnectionType();
            ConnectionParamPanel panel = paramPanels.get(type);
            if (panel != null) {
                panel.setConnectionParams(device.getConnectionParams());
            }
        });
    }

    /**
     * 验证输入是否有效
     *
     * @return 是否有效
     */
    public boolean validateInput() {
        if (nameField.getText().trim().isEmpty()) {
            showValidationError("设备名称不能为空");
            nameField.requestFocus();
            return false;
        }

        if (modelField.getText().trim().isEmpty()) {
            showValidationError("设备型号不能为空");
            modelField.requestFocus();
            return false;
        }

        String type = getSelectedConnectionType();
        ConnectionParamPanel panel = paramPanels.get(type);
        if (panel != null) {
            return panel.validateInput();
        }

        return true;
    }

    /**
     * 显示验证错误消息
     *
     * @param message 错误消息
     */
    private void showValidationError(String message) {
        JOptionPane.showMessageDialog(this, message, "验证失败", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * 连接参数面板接口
     */
    public interface ConnectionParamPanel {
        /**
         * 获取连接参数字符串
         *
         * @return 连接参数字符串
         */
        String getConnectionParams();

        /**
         * 设置连接参数
         *
         * @param params 连接参数字符串
         */
        void setConnectionParams(String params);

        /**
         * 验证输入是否有效
         *
         * @return 是否有效
         */
        boolean validateInput();
    }

    /**
     * 网络连接参数面板
     */
    public static class NetworkParamPanel extends JPanel implements ConnectionParamPanel {
        private static final String[] PROTOCOLS = {"TCP", "UDP"};
        // 使用本地化的模式标签
        private static final String[] MODE_VALUES = {"CLIENT", "SERVER"};
        private static final String[] MODE_LABELS = {"客户端模式", "服务器模式"};

        private final JTextField hostField = new JTextField(15);
        private final JTextField portField = new JTextField(5);
        private final JComboBox<String> protocolCombo = new JComboBox<>(PROTOCOLS);
        private final JComboBox<String> modeCombo = new JComboBox<>(MODE_LABELS);
        private final JCheckBox longConnectionBox = new JCheckBox("保持长连接");
        private final JPanel clientPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        private final JPanel serverPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        private final CardLayout modeCardLayout = new CardLayout();
        private final JPanel modePanel = new JPanel(modeCardLayout);
        private final JTextField serverPortField = new JTextField(5);

        // 在类内部定义实例变量，而不是静态变量
        private boolean isUpdatingMode = false;
        private boolean isUpdatingPort = false;

        public NetworkParamPanel() {
            setBorder(BorderFactory.createTitledBorder("网络连接参数"));
            setLayout(new GridBagLayout());
            initComponents();
            initListeners();
            setDefaultValues();
        }

        private void initComponents() {
            // 应用字体设置
            UIHelper.applyFontToComponent(hostField, FONT_SIZE);
            UIHelper.applyFontToComponent(portField, FONT_SIZE);
            UIHelper.applyFontToComponent(protocolCombo, FONT_SIZE);
            UIHelper.applyFontToComponent(modeCombo, FONT_SIZE);
            UIHelper.applyFontToComponent(longConnectionBox, FONT_SIZE);
            UIHelper.applyFontToComponent(serverPortField, FONT_SIZE);

            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4, 4, 4, 4);
            c.fill = GridBagConstraints.HORIZONTAL;

            // 基本设置
            JLabel modeLabel = new JLabel("连接模式:", SwingConstants.RIGHT);
            UIHelper.applyFontToComponent(modeLabel, FONT_SIZE);
            c.gridx = 0;
            c.gridy = 0;
            add(modeLabel, c);

            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 1.0;
            add(modeCombo, c);

            JLabel protocolLabel = new JLabel("协议:", SwingConstants.RIGHT);
            UIHelper.applyFontToComponent(protocolLabel, FONT_SIZE);
            c.gridx = 0;
            c.gridy = 1;
            c.weightx = 0.0;
            add(protocolLabel, c);

            c.gridx = 1;
            c.gridy = 1;
            c.weightx = 1.0;
            add(protocolCombo, c);

            // 设置工具提示
            hostField.setToolTipText("输入服务器主机名或IP地址");
            portField.setToolTipText("输入端口号(1-65535)");
            serverPortField.setToolTipText("输入监听端口号(1-65535)");
            modeCombo.setToolTipText("选择客户端或服务器模式");
            protocolCombo.setToolTipText("选择通信协议");
            longConnectionBox.setToolTipText("保持与服务器的连接不断开");

            // 客户端特有设置
            setupClientPanel();

            // 服务器特有设置
            setupServerPanel();

            // 将模式面板添加到卡片布局中
            modePanel.add(clientPanel, "CLIENT");
            modePanel.add(serverPanel, "SERVER");

            c.gridx = 0;
            c.gridy = 2;
            c.gridwidth = 2;
            c.weightx = 1.0;
            add(modePanel, c);
        }

        private void setupClientPanel() {
            clientPanel.setLayout(new GridBagLayout());
            GridBagConstraints cc = new GridBagConstraints();
            cc.insets = new Insets(4, 4, 4, 4);
            cc.fill = GridBagConstraints.HORIZONTAL;

            JLabel hostLabel = new JLabel("主机地址:", SwingConstants.RIGHT);
            UIHelper.applyFontToComponent(hostLabel, FONT_SIZE);
            cc.gridx = 0;
            cc.gridy = 0;
            clientPanel.add(hostLabel, cc);

            cc.gridx = 1;
            cc.gridy = 0;
            cc.weightx = 1.0;
            clientPanel.add(hostField, cc);

            JLabel portLabel = new JLabel("端口:", SwingConstants.RIGHT);
            UIHelper.applyFontToComponent(portLabel, FONT_SIZE);
            cc.gridx = 0;
            cc.gridy = 1;
            cc.weightx = 0.0;
            clientPanel.add(portLabel, cc);

            cc.gridx = 1;
            cc.gridy = 1;
            cc.weightx = 1.0;
            clientPanel.add(portField, cc);

            cc.gridx = 0;
            cc.gridy = 2;
            cc.gridwidth = 2;
            clientPanel.add(longConnectionBox, cc);
        }

        private void setupServerPanel() {
            serverPanel.setLayout(new GridBagLayout());
            GridBagConstraints sc = new GridBagConstraints();
            sc.insets = new Insets(4, 4, 4, 4);
            sc.fill = GridBagConstraints.HORIZONTAL;

            JLabel serverPortLabel = new JLabel("监听端口:", SwingConstants.RIGHT);
            UIHelper.applyFontToComponent(serverPortLabel, FONT_SIZE);
            sc.gridx = 0;
            sc.gridy = 0;
            serverPanel.add(serverPortLabel, sc);

            sc.gridx = 1;
            sc.gridy = 0;
            sc.weightx = 1.0;
            serverPanel.add(serverPortField, sc);
        }

        private void initListeners() {
            // 端口字段同步 (优化后的双向绑定逻辑)
            portField.getDocument().addDocumentListener(new DocumentAdapter() {
                @Override
                protected void update(DocumentEvent e) {
                    if (!isUpdatingPort) {
                        isUpdatingPort = true;
                        try {
                            serverPortField.setText(portField.getText());
                        } finally {
                            isUpdatingPort = false;
                        }
                    }
                }
            });

            serverPortField.getDocument().addDocumentListener(new DocumentAdapter() {
                @Override
                protected void update(DocumentEvent e) {
                    if (!isUpdatingPort) {
                        isUpdatingPort = true;
                        try {
                            portField.setText(serverPortField.getText());
                        } finally {
                            isUpdatingPort = false;
                        }
                    }
                }
            });

            // 添加模式切换监听器 (优化后的模式切换逻辑)
            modeCombo.addActionListener(e -> {
                if (isUpdatingMode) return; // 避免循环调用

                isUpdatingMode = true;
                try {
                    String selectedMode = (String) modeCombo.getSelectedItem();
                    if (selectedMode != null) {
                        modeCardLayout.show(modePanel, selectedMode);
                    }
                } finally {
                    isUpdatingMode = false;
                }
            });
        }

        private void setDefaultValues() {
            // 默认设置
            hostField.setText("localhost");
            portField.setText("8088");
            serverPortField.setText("8088"); // 确保两个端口字段初始值相同
            protocolCombo.setSelectedItem("TCP");
            modeCombo.setSelectedItem("CLIENT");
            longConnectionBox.setSelected(false);
        }

        @Override
        public String getConnectionParams() {
            String protocol = (String) protocolCombo.getSelectedItem();
            // 获取真实模式值
            String modeDisplayText = (String) modeCombo.getSelectedItem();
            String mode = getModeValueFromDisplay(modeDisplayText);

            if ("CLIENT".equals(mode)) {
                String host = hostField.getText().trim();
                String port = portField.getText().trim();
                boolean longConnection = longConnectionBox.isSelected();

                // 格式: host:port:protocol:CLIENT[:longConnection]
                return String.format("%s:%s:%s:%s:%s", host, port, protocol, mode, longConnection);
            } else {
                // 服务器模式
                String port = serverPortField.getText().trim();

                // 格式: port:protocol:SERVER
                return String.format("%s:%s:%s", port, protocol, mode);
            }
        }

        @Override
        public void setConnectionParams(String params) {
            if (params == null || params.isEmpty()) {
                setDefaultValues();
                return;
            }

            try {
                String[] parts = params.split(":");

                if (parts.length >= 3) {
                    // 至少有3部分，可能是服务器模式
                    if (parts.length >= 4 && "CLIENT".equals(parts[3])) {
                        // 客户端模式: host:port:protocol:CLIENT[:longConnection]
                        hostField.setText(parts[0]);
                        portField.setText(parts[1]);
                        protocolCombo.setSelectedItem(parts[2]);

                        // 设置模式（使用显示文本）
                        setModeComboByValue("CLIENT");

                        // 设置长连接
                        boolean longConnection = parts.length >= 5 && Boolean.parseBoolean(parts[4]);
                        longConnectionBox.setSelected(longConnection);
                    } else if (parts.length >= 3 && "SERVER".equals(parts[2])) {
                        // 服务器模式: port:protocol:SERVER
                        serverPortField.setText(parts[0]);
                        protocolCombo.setSelectedItem(parts[1]);

                        // 设置模式（使用显示文本）
                        setModeComboByValue("SERVER");
                    } else {
                        // 旧格式或未知格式，尽量解析
                        hostField.setText(parts[0]);
                        portField.setText(parts[1]);
                        if (parts.length >= 3) {
                            protocolCombo.setSelectedItem(parts[2]);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("解析连接参数时出错: {}", e.getMessage());
            }
        }

        /**
         * 根据模式值设置下拉框选中项
         *
         * @param modeValue 模式值 (CLIENT/SERVER)
         */
        private void setModeComboByValue(String modeValue) {
            // 找到对应的显示文本
            for (int i = 0; i < MODE_VALUES.length; i++) {
                if (MODE_VALUES[i].equals(modeValue)) {
                    modeCombo.setSelectedItem(MODE_LABELS[i]);
                    break;
                }
            }
        }

        /**
         * 根据显示文本获取模式值
         *
         * @param displayText 显示文本
         * @return 模式值 (CLIENT/SERVER)
         */
        private String getModeValueFromDisplay(String displayText) {
            // 找到对应的模式值
            for (int i = 0; i < MODE_LABELS.length; i++) {
                if (MODE_LABELS[i].equals(displayText)) {
                    return MODE_VALUES[i];
                }
            }
            // 默认返回客户端模式
            return "CLIENT";
        }

        @Override
        public boolean validateInput() {
            String mode = (String) modeCombo.getSelectedItem();

            if ("CLIENT".equals(mode)) {
                // 客户端模式验证
                if (hostField.getText().trim().isEmpty()) {
                    showValidationError("主机地址不能为空");
                    hostField.requestFocus();
                    return false;
                }
            }

            // 使用正确的端口字段进行验证
            String portText = "CLIENT".equals(mode) ? portField.getText().trim() : serverPortField.getText().trim();

            // 通用验证
            if (portText.isEmpty()) {
                showValidationError("端口不能为空");
                if ("CLIENT".equals(mode)) {
                    portField.requestFocus();
                } else {
                    serverPortField.requestFocus();
                }
                return false;
            }

            try {
                int port = Integer.parseInt(portText);
                if (port <= 0 || port > 65535) {
                    showValidationError("端口必须在1-65535之间");
                    return false;
                }
            } catch (NumberFormatException e) {
                showValidationError("端口必须是有效的数字");
                return false;
            }

            return true;
        }

        private void showValidationError(String message) {
            JOptionPane.showMessageDialog(this, message, "验证失败", JOptionPane.ERROR_MESSAGE);
        }

        /**
         * 文档监听适配器
         */
        private abstract static class DocumentAdapter implements DocumentListener {
            @Override
            public void insertUpdate(DocumentEvent e) {
                update(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update(e);
            }

            protected abstract void update(DocumentEvent e);
        }
    }

    /**
     * 串口连接参数面板
     */
    public static class SerialPortParamPanel extends JPanel implements ConnectionParamPanel {
        private static final String[] BAUD_RATES = {"1200", "2400", "4800", "9600", "19200", "38400", "57600", "115200"};
        private static final String[] DATA_BITS = {"5", "6", "7", "8"};
        private static final String[] STOP_BITS = {"1", "1.5", "2"};
        private static final String[] PARITY_OPTIONS = {"NONE", "ODD", "EVEN", "MARK", "SPACE"};

        private final JTextField portNameField = new JTextField(20);
        private final JComboBox<String> baudRateCombo = new JComboBox<>(BAUD_RATES);
        private final JComboBox<String> dataBitsCombo = new JComboBox<>(DATA_BITS);
        private final JComboBox<String> stopBitsCombo = new JComboBox<>(STOP_BITS);
        private final JComboBox<String> parityCombo = new JComboBox<>(PARITY_OPTIONS);

        public SerialPortParamPanel() {
            setBorder(BorderFactory.createTitledBorder("串口连接参数"));
            initComponents();
            setDefaultValues();
        }

        private void initComponents() {
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(4, 4, 4, 4);

            // 端口名称
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.EAST;
            add(new JLabel("端口名称:", SwingConstants.RIGHT), gbc);

            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            add(portNameField, gbc);

            // 波特率
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 0.0;
            add(new JLabel("波特率:", SwingConstants.RIGHT), gbc);

            gbc.gridx = 1;
            gbc.gridy = 1;
            gbc.weightx = 1.0;
            add(baudRateCombo, gbc);

            // 数据位
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.weightx = 0.0;
            add(new JLabel("数据位:", SwingConstants.RIGHT), gbc);

            gbc.gridx = 1;
            gbc.gridy = 2;
            gbc.weightx = 1.0;
            add(dataBitsCombo, gbc);

            // 停止位
            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.weightx = 0.0;
            add(new JLabel("停止位:", SwingConstants.RIGHT), gbc);

            gbc.gridx = 1;
            gbc.gridy = 3;
            gbc.weightx = 1.0;
            add(stopBitsCombo, gbc);

            // 校验位
            gbc.gridx = 0;
            gbc.gridy = 4;
            gbc.weightx = 0.0;
            add(new JLabel("校验位:", SwingConstants.RIGHT), gbc);

            gbc.gridx = 1;
            gbc.gridy = 4;
            gbc.weightx = 1.0;
            add(parityCombo, gbc);

            // 设置工具提示
            portNameField.setToolTipText("输入串口名称，如COM1");
            baudRateCombo.setToolTipText("选择串口波特率");
            dataBitsCombo.setToolTipText("选择数据位");
            stopBitsCombo.setToolTipText("选择停止位");
            parityCombo.setToolTipText("选择校验方式");
        }

        private void setDefaultValues() {
            portNameField.setText("COM1");
            baudRateCombo.setSelectedItem("9600");
            dataBitsCombo.setSelectedItem("8");
            stopBitsCombo.setSelectedItem("1");
            parityCombo.setSelectedItem("NONE");
        }

        @Override
        public String getConnectionParams() {
            return String.format("%s:%s:%s:%s:%s",
                    portNameField.getText().trim(),
                    baudRateCombo.getSelectedItem(),
                    dataBitsCombo.getSelectedItem(),
                    stopBitsCombo.getSelectedItem(),
                    getParityValue());
        }

        private String getParityValue() {
            String parity = (String) parityCombo.getSelectedItem();
            switch (parity) {
                case "NONE": return "0";
                case "ODD": return "1";
                case "EVEN": return "2";
                case "MARK": return "3";
                case "SPACE": return "4";
                default: return "0";
            }
        }

        @Override
        public void setConnectionParams(String params) {
            if (params == null || params.isEmpty()) {
                setDefaultValues();
                return;
            }

            try {
                String[] parts = params.split(":");
                if (parts.length >= 1) portNameField.setText(parts[0]);
                if (parts.length >= 2) baudRateCombo.setSelectedItem(parts[1]);
                if (parts.length >= 3) dataBitsCombo.setSelectedItem(parts[2]);
                if (parts.length >= 4) stopBitsCombo.setSelectedItem(parts[3]);
                if (parts.length >= 5) {
                    switch (parts[4]) {
                        case "0": parityCombo.setSelectedItem("NONE"); break;
                        case "1": parityCombo.setSelectedItem("ODD"); break;
                        case "2": parityCombo.setSelectedItem("EVEN"); break;
                        case "3": parityCombo.setSelectedItem("MARK"); break;
                        case "4": parityCombo.setSelectedItem("SPACE"); break;
                        default: parityCombo.setSelectedItem("NONE");
                    }
                }
            } catch (Exception e) {
                // 解析失败，记录异常并使用默认值
                LOGGER.log(Level.WARNING, "解析串口连接参数失败: " + params, e);
                setDefaultValues();
            }
        }

        @Override
        public boolean validateInput() {
            if (portNameField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "端口名称不能为空", "验证失败", JOptionPane.ERROR_MESSAGE);
                portNameField.requestFocus();
                return false;
            }
            return true;
        }
    }

    /**
     * 文件连接参数面板
     */
    public static class FileParamPanel extends JPanel implements ConnectionParamPanel {
        private static final String[] CHARSET_OPTIONS = {"UTF-8", "GBK", "GB2312", "ISO-8859-1"};

        private final JTextField directoryField = new JTextField(20);
        private final JTextField filePatternField = new JTextField(10);
        private final JComboBox<String> charsetCombo = new JComboBox<>(CHARSET_OPTIONS);
        private final JCheckBox deleteAfterProcessBox = new JCheckBox("处理后删除");
        private final JButton browseButton = new JButton("浏览...");

        public FileParamPanel() {
            setBorder(BorderFactory.createTitledBorder("文件连接参数"));
            initComponents();
            initListeners();
            setDefaultValues();
        }

        private void initComponents() {
            setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4, 4, 4, 4);
            c.fill = GridBagConstraints.HORIZONTAL;

            // 监视目录
            c.gridx = 0;
            c.gridy = 0;
            add(new JLabel("监视目录:", SwingConstants.RIGHT), c);

            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 1.0;
            add(directoryField, c);

            c.gridx = 2;
            c.gridy = 0;
            c.weightx = 0;
            add(browseButton, c);

            // 文件模式
            c.gridx = 0;
            c.gridy = 1;
            add(new JLabel("文件模式:", SwingConstants.RIGHT), c);

            c.gridx = 1;
            c.gridy = 1;
            c.weightx = 1.0;
            c.gridwidth = 2;
            add(filePatternField, c);

            // 字符编码
            c.gridx = 0;
            c.gridy = 2;
            c.gridwidth = 1;
            c.weightx = 0;
            add(new JLabel("字符编码:", SwingConstants.RIGHT), c);

            c.gridx = 1;
            c.gridy = 2;
            c.weightx = 1.0;
            add(charsetCombo, c);

            // 文件处理
            c.gridx = 0;
            c.gridy = 3;
            add(new JLabel("文件处理:", SwingConstants.RIGHT), c);

            c.gridx = 1;
            c.gridy = 3;
            add(deleteAfterProcessBox, c);

            // 设置工具提示
            directoryField.setToolTipText("选择或输入监视文件的目录路径");
            filePatternField.setToolTipText("输入文件名匹配模式，如*.hl7");
            charsetCombo.setToolTipText("选择文件字符编码");
            deleteAfterProcessBox.setToolTipText("选中后将在处理完文件后删除源文件");
            browseButton.setToolTipText("浏览选择目录");
        }

        private void initListeners() {
            // 添加浏览按钮事件
            browseButton.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("选择监视目录");
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                // 如果已有目录，设为初始目录
                String currentDir = directoryField.getText().trim();
                if (!currentDir.isEmpty()) {
                    File dir = new File(currentDir);
                    if (dir.exists() && dir.isDirectory()) {
                        fileChooser.setCurrentDirectory(dir);
                    }
                }

                int result = fileChooser.showOpenDialog(this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    directoryField.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }
            });
        }

        private void setDefaultValues() {
            directoryField.setText("");
            filePatternField.setText("*.hl7");
            charsetCombo.setSelectedItem("UTF-8");
            deleteAfterProcessBox.setSelected(false);
        }

        @Override
        public String getConnectionParams() {
            return String.format("%s:%s:%s:%b",
                    directoryField.getText().trim(),
                    filePatternField.getText().trim(),
                    charsetCombo.getSelectedItem(),
                    deleteAfterProcessBox.isSelected());
        }

        @Override
        public void setConnectionParams(String params) {
            if (params == null || params.isEmpty()) {
                setDefaultValues();
                return;
            }

            try {
                String[] parts = params.split(":");
                if (parts.length >= 1) directoryField.setText(parts[0]);
                if (parts.length >= 2) filePatternField.setText(parts[1]);
                if (parts.length >= 3) charsetCombo.setSelectedItem(parts[2]);
                if (parts.length >= 4) deleteAfterProcessBox.setSelected(Boolean.parseBoolean(parts[3]));
            } catch (Exception e) {
                // 解析失败，记录异常并使用默认值
                LOGGER.log(Level.WARNING, "解析文件连接参数失败: " + params, e);
                setDefaultValues();
            }
        }

        @Override
        public boolean validateInput() {
            if (directoryField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "监视目录不能为空", "验证失败", JOptionPane.ERROR_MESSAGE);
                directoryField.requestFocus();
                return false;
            }

            // 检查目录是否存在
            File directory = new File(directoryField.getText().trim());
            if (!directory.exists() || !directory.isDirectory()) {
                JOptionPane.showMessageDialog(this, "监视目录不存在或不是有效目录", "验证失败", JOptionPane.ERROR_MESSAGE);
                directoryField.requestFocus();
                return false;
            }

            if (filePatternField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "文件模式不能为空", "验证失败", JOptionPane.ERROR_MESSAGE);
                filePatternField.requestFocus();
                return false;
            }

            return true;
        }
    }

    /**
     * 数据库连接参数面板
     */
    public static class DatabaseParamPanel extends JPanel implements ConnectionParamPanel {
        private static final String[] DB_TYPES = {"MySQL", "Oracle", "SQL Server", "Access"};

        private final JComboBox<String> dbTypeCombo = new JComboBox<>(DB_TYPES);
        private final JTextField hostField = new JTextField(15);
        private final JTextField portField = new JTextField(5);
        private final JTextField databaseField = new JTextField(15);
        private final JTextField usernameField = new JTextField(15);
        private final JPasswordField passwordField = new JPasswordField(15);
        private final JCheckBox enableScheduleBox = new JCheckBox("启用定时查询");
        private final JTextField scheduleIntervalField = new JTextField(5);
        private final JPanel intervalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        private final JButton testConnectionButton = new JButton("测试连接");
        private final JButton browseButton = new JButton("浏览...");
        private final CardLayout dbConfigLayout = new CardLayout();
        private final JPanel dbConfigPanel = new JPanel(dbConfigLayout);
        private final JPanel standardDbPanel = new JPanel(new GridBagLayout());
        private final JPanel accessDbPanel = new JPanel(new GridBagLayout());

        public DatabaseParamPanel() {
            setBorder(BorderFactory.createTitledBorder("数据库连接参数"));
            initComponents();
            initListeners();
            setDefaultValues();
        }

        private void initComponents() {
            setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4, 4, 4, 4);
            c.fill = GridBagConstraints.HORIZONTAL;

            // 数据库类型
            c.gridx = 0;
            c.gridy = 0;
            add(new JLabel("数据库类型:", SwingConstants.RIGHT), c);

            c.gridx = 1;
            c.gridy = 0;
            c.gridwidth = 2;
            add(dbTypeCombo, c);

            // 配置面板 (Access与标准数据库)
            setupStandardDbPanel();
            setupAccessDbPanel();

            dbConfigPanel.add(standardDbPanel, "STANDARD");
            dbConfigPanel.add(accessDbPanel, "ACCESS");

            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 3;
            c.weightx = 1.0;
            add(dbConfigPanel, c);

            // 定时查询
            c.gridx = 0;
            c.gridy = 2;
            c.gridwidth = 3;
            add(enableScheduleBox, c);

            // 查询间隔
            intervalPanel.add(new JLabel("查询间隔(秒):"));
            intervalPanel.add(scheduleIntervalField);

            c.gridx = 0;
            c.gridy = 3;
            c.gridwidth = 3;
            add(intervalPanel, c);

            // 测试连接按钮
            c.gridx = 0;
            c.gridy = 4;
            c.gridwidth = 3;
            c.anchor = GridBagConstraints.CENTER;
            add(testConnectionButton, c);

            // 设置工具提示
            dbTypeCombo.setToolTipText("选择数据库类型");
            hostField.setToolTipText("输入数据库服务器主机名或IP地址");
            portField.setToolTipText("输入数据库服务器端口");
            databaseField.setToolTipText("输入数据库名称或Access文件路径");
            usernameField.setToolTipText("输入数据库用户名");
            passwordField.setToolTipText("输入数据库密码");
            enableScheduleBox.setToolTipText("是否启用定时查询数据库");
            scheduleIntervalField.setToolTipText("输入查询间隔秒数");
            testConnectionButton.setToolTipText("测试数据库连接是否正常");
            browseButton.setToolTipText("浏览选择Access数据库文件");
        }

        private void setupStandardDbPanel() {
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4, 4, 4, 4);
            c.fill = GridBagConstraints.HORIZONTAL;

            // 主机
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 1;
            standardDbPanel.add(new JLabel("主机:", SwingConstants.RIGHT), c);

            c.gridx = 1;
            c.gridy = 0;
            c.gridwidth = 2;
            c.weightx = 1.0;
            standardDbPanel.add(hostField, c);

            // 端口
            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 1;
            c.weightx = 0.0;
            standardDbPanel.add(new JLabel("端口:", SwingConstants.RIGHT), c);

            c.gridx = 1;
            c.gridy = 1;
            c.gridwidth = 2;
            c.weightx = 1.0;
            standardDbPanel.add(portField, c);

            // 数据库名
            c.gridx = 0;
            c.gridy = 2;
            c.gridwidth = 1;
            c.weightx = 0.0;
            standardDbPanel.add(new JLabel("数据库:", SwingConstants.RIGHT), c);

            c.gridx = 1;
            c.gridy = 2;
            c.gridwidth = 2;
            c.weightx = 1.0;
            standardDbPanel.add(databaseField, c);

            // 用户名
            c.gridx = 0;
            c.gridy = 3;
            c.gridwidth = 1;
            c.weightx = 0.0;
            standardDbPanel.add(new JLabel("用户名:", SwingConstants.RIGHT), c);

            c.gridx = 1;
            c.gridy = 3;
            c.gridwidth = 2;
            c.weightx = 1.0;
            standardDbPanel.add(usernameField, c);

            // 密码
            c.gridx = 0;
            c.gridy = 4;
            c.gridwidth = 1;
            c.weightx = 0.0;
            standardDbPanel.add(new JLabel("密码:", SwingConstants.RIGHT), c);

            c.gridx = 1;
            c.gridy = 4;
            c.gridwidth = 2;
            c.weightx = 1.0;
            standardDbPanel.add(passwordField, c);
        }

        private void setupAccessDbPanel() {
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4, 4, 4, 4);
            c.fill = GridBagConstraints.HORIZONTAL;

            // 数据库文件
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 1;
            accessDbPanel.add(new JLabel("数据库文件:", SwingConstants.RIGHT), c);

            c.gridx = 1;
            c.gridy = 0;
            c.gridwidth = 1;
            c.weightx = 1.0;
            accessDbPanel.add(databaseField, c);

            c.gridx = 2;
            c.gridy = 0;
            c.gridwidth = 1;
            c.weightx = 0.0;
            accessDbPanel.add(browseButton, c);
        }

        private void initListeners() {
            // 根据数据库类型设置默认端口和显示不同面板
            dbTypeCombo.addActionListener(e -> {
                String dbType = (String) dbTypeCombo.getSelectedItem();
                if (dbType != null) {
                    if ("Access".equals(dbType)) {
                        dbConfigLayout.show(dbConfigPanel, "ACCESS");
                    } else {
                        dbConfigLayout.show(dbConfigPanel, "STANDARD");
                        setDefaultPortForDbType();
                    }
                }
            });

            // 根据是否启用定时查询设置查询间隔字段的可用性
            enableScheduleBox.addActionListener(e ->
                    scheduleIntervalField.setEnabled(enableScheduleBox.isSelected()));

            // 浏览按钮事件
            browseButton.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("选择Access数据库文件");
                fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                        "Access数据库文件 (*.mdb, *.accdb)", "mdb", "accdb"));

                int result = fileChooser.showOpenDialog(this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    databaseField.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }
            });

            // 测试连接按钮事件
            testConnectionButton.addActionListener(e -> {
                String dbType = (String) dbTypeCombo.getSelectedItem();

                // 这里只是模拟一个连接测试，实际应用中应该进行真正的数据库连接测试
                if (validateInput()) {
                    JOptionPane.showMessageDialog(this,
                            "连接测试成功！\n已成功连接到 " + dbType + " 数据库。",
                            "连接测试",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            });
        }

        private void setDefaultPortForDbType() {
            String dbType = (String) dbTypeCombo.getSelectedItem();
            if (dbType == null) return;

            switch (dbType) {
                case "MySQL":
                    portField.setText("3306");
                    break;
                case "Oracle":
                    portField.setText("1521");
                    break;
                case "SQL Server":
                    portField.setText("1433");
                    break;
                case "Access":
                    portField.setText("");
                    break;
            }
        }

        private void setDefaultValues() {
            dbTypeCombo.setSelectedItem("MySQL");
            hostField.setText("localhost");
            portField.setText("3306");
            databaseField.setText("");
            usernameField.setText("");
            passwordField.setText("");
            enableScheduleBox.setSelected(false);
            scheduleIntervalField.setText("60");
            scheduleIntervalField.setEnabled(false);

            // 默认显示标准数据库面板
            dbConfigLayout.show(dbConfigPanel, "STANDARD");
        }

        @Override
        public String getConnectionParams() {
            String dbType = (String) dbTypeCombo.getSelectedItem();

            // Access数据库特殊处理，只需要文件路径
            if ("Access".equals(dbType)) {
                return String.format("%s:%s:%s:%s:%b:%s",
                        dbType,
                        databaseField.getText().trim(),
                        "",
                        "",
                        enableScheduleBox.isSelected(),
                        scheduleIntervalField.getText().trim());
            }

            return String.format("%s:%s:%s:%s:%s:%s:%b:%s",
                    dbType,
                    hostField.getText().trim(),
                    portField.getText().trim(),
                    databaseField.getText().trim(),
                    usernameField.getText().trim(),
                    new String(passwordField.getPassword()),
                    enableScheduleBox.isSelected(),
                    scheduleIntervalField.getText().trim());
        }

        @Override
        public void setConnectionParams(String params) {
            if (params == null || params.isEmpty()) {
                setDefaultValues();
                return;
            }

            try {
                String[] parts = params.split(":");

                if (parts.length >= 1) {
                    dbTypeCombo.setSelectedItem(parts[0]);
                    // Access数据库特殊处理
                    if ("Access".equals(parts[0])) {
                        dbConfigLayout.show(dbConfigPanel, "ACCESS");
                        if (parts.length >= 2) databaseField.setText(parts[1]);
                        if (parts.length >= 5) enableScheduleBox.setSelected(Boolean.parseBoolean(parts[4]));
                        if (parts.length >= 6) scheduleIntervalField.setText(parts[5]);
                        scheduleIntervalField.setEnabled(enableScheduleBox.isSelected());
                        return;
                    } else {
                        dbConfigLayout.show(dbConfigPanel, "STANDARD");
                    }
                }

                if (parts.length >= 2) hostField.setText(parts[1]);
                if (parts.length >= 3) portField.setText(parts[2]);
                if (parts.length >= 4) databaseField.setText(parts[3]);
                if (parts.length >= 5) usernameField.setText(parts[4]);
                if (parts.length >= 6) passwordField.setText(parts[5]);
                if (parts.length >= 7) enableScheduleBox.setSelected(Boolean.parseBoolean(parts[6]));
                if (parts.length >= 8) scheduleIntervalField.setText(parts[7]);

                scheduleIntervalField.setEnabled(enableScheduleBox.isSelected());

            } catch (Exception e) {
                // 解析失败，记录异常并使用默认值
                LOGGER.log(Level.WARNING, "解析数据库连接参数失败: " + params, e);
                setDefaultValues();
            }
        }

        @Override
        public boolean validateInput() {
            String dbType = (String) dbTypeCombo.getSelectedItem();

            // Access数据库特殊验证
            if ("Access".equals(dbType)) {
                if (databaseField.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "数据库文件路径不能为空", "验证失败", JOptionPane.ERROR_MESSAGE);
                    databaseField.requestFocus();
                    return false;
                }

                // 检查文件是否存在
                File dbFile = new File(databaseField.getText().trim());
                if (!dbFile.exists() || !dbFile.isFile()) {
                    JOptionPane.showMessageDialog(this, "数据库文件不存在", "验证失败", JOptionPane.ERROR_MESSAGE);
                    databaseField.requestFocus();
                    return false;
                }

                // 验证查询间隔
                if (enableScheduleBox.isSelected()) {
                    return validateQueryInterval();
                }

                return true;
            }

            // 其他数据库验证
            if (hostField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "主机不能为空", "验证失败", JOptionPane.ERROR_MESSAGE);
                hostField.requestFocus();
                return false;
            }

            if (!portField.getText().trim().isEmpty()) {
                try {
                    int port = Integer.parseInt(portField.getText().trim());
                    if (port <= 0 || port > 65535) {
                        JOptionPane.showMessageDialog(this, "端口必须在1-65535之间", "验证失败", JOptionPane.ERROR_MESSAGE);
                        portField.requestFocus();
                        return false;
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "端口必须是有效的数字", "验证失败", JOptionPane.ERROR_MESSAGE);
                    portField.requestFocus();
                    return false;
                }
            }

            if (databaseField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "数据库名不能为空", "验证失败", JOptionPane.ERROR_MESSAGE);
                databaseField.requestFocus();
                return false;
            }

            // 验证查询间隔
            if (enableScheduleBox.isSelected()) {
                return validateQueryInterval();
            }

            return true;
        }

        private boolean validateQueryInterval() {
            try {
                int interval = Integer.parseInt(scheduleIntervalField.getText().trim());
                if (interval <= 0) {
                    JOptionPane.showMessageDialog(this, "查询间隔必须大于0", "验证失败", JOptionPane.ERROR_MESSAGE);
                    scheduleIntervalField.requestFocus();
                    return false;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "查询间隔必须是有效的数字", "验证失败", JOptionPane.ERROR_MESSAGE);
                scheduleIntervalField.requestFocus();
                return false;
            }
            return true;
        }
    }
}
