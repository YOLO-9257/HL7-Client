package com.hl7.client.interfaces.window;

import com.hl7.client.domain.model.Device;
import com.hl7.client.domain.model.DeviceStatus;
import com.hl7.client.infrastructure.util.SnowflakeIdGenerator;
import com.hl7.client.infrastructure.util.UIHelper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Logger;

/**
 * 设备编辑对话框
 * 用于添加或编辑设备信息
 */
@Slf4j
public class DeviceDialog extends JDialog {
    private static final Logger LOGGER = Logger.getLogger(DeviceDialog.class.getName());

    // 字体大小设置
    private static final int FONT_SIZE = 16;

    private final DeviceConfigPanel configPanel;
    private final JButton confirmButton;
    private final JButton cancelButton;
    private final JButton helpButton;
    private final JPanel buttonPanel;
    private Device originalDevice;
    private boolean dataChanged = false;

    @Getter
    private boolean confirmed = false;

    /**
     * 构造函数
     *
     * @param owner 所有者窗口
     * @param title 对话框标题
     */
    public DeviceDialog(Frame owner, String title) {
        super(owner, title, true);

        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        // 创建配置面板
        configPanel = new DeviceConfigPanel();
        add(configPanel, BorderLayout.CENTER);

        // 创建状态栏
        JPanel statusPanel = createStatusPanel();
        add(statusPanel, BorderLayout.SOUTH);

        // 创建按钮面板
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 0));

        helpButton = new JButton("帮助");
        confirmButton = new JButton("确定");
        cancelButton = new JButton("取消");

        // 应用字体设置
        UIHelper.applyFontToComponent(helpButton, FONT_SIZE);
        UIHelper.applyFontToComponent(confirmButton, FONT_SIZE);
        UIHelper.applyFontToComponent(cancelButton, FONT_SIZE);

        // 设置按钮助记符
        helpButton.setMnemonic(KeyEvent.VK_H);
        confirmButton.setMnemonic(KeyEvent.VK_O);
        cancelButton.setMnemonic(KeyEvent.VK_C);

        // 设置默认按钮和按钮图标
        confirmButton.setIcon(UIManager.getIcon("OptionPane.okIcon"));
        cancelButton.setIcon(UIManager.getIcon("OptionPane.cancelIcon"));
        helpButton.setIcon(UIManager.getIcon("OptionPane.questionIcon"));

        getRootPane().setDefaultButton(confirmButton);

        buttonPanel.add(helpButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        statusPanel.add(buttonPanel, BorderLayout.EAST);

        // 添加事件监听器
        initListeners();

        // 设置对话框大小和位置
        setSizeAndPosition(owner);

        // 设置对话框图标
        if (owner != null) {
            setIconImage(owner.getIconImage());
        }

        // 添加键盘监听
        setupKeyboardNavigation();
    }

    /**
     * 创建状态面板
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        // 提示标签
        JLabel tipLabel = new JLabel("提示: 填写完所有必填项后点击确定");
        tipLabel.setFont(tipLabel.getFont().deriveFont(Font.ITALIC));
        tipLabel.setForeground(Color.GRAY);

        // 应用字体设置
        UIHelper.applyFontToComponent(tipLabel, FONT_SIZE);

        panel.add(tipLabel, BorderLayout.WEST);
        return panel;
    }

    /**
     * 初始化事件监听器
     */
    private void initListeners() {
        // 确认按钮
        confirmButton.addActionListener(e -> {
            if (configPanel.validateInput()) {
                confirmed = true;
                setVisible(false);
            }
        });

        // 取消按钮
        cancelButton.addActionListener(e -> closeDialog());

        // 帮助按钮
        helpButton.addActionListener(e -> showHelp());

        // 窗口关闭事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeDialog();
            }
        });

        // 监听数据变化
        configPanel.getNameField().getDocument().addDocumentListener(new DataChangeListener());
        configPanel.getModelField().getDocument().addDocumentListener(new DataChangeListener());
        configPanel.getManufacturerField().getDocument().addDocumentListener(new DataChangeListener());
        configPanel.getConnectionTypeCombo().addActionListener(e -> dataChanged = true);
    }

    /**
     * 设置键盘导航快捷键
     */
    private void setupKeyboardNavigation() {
        // ESC键关闭对话框
        KeyStroke escKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().registerKeyboardAction(
                e -> closeDialog(),
                escKeyStroke,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * 设置对话框大小和位置
     */
    private void setSizeAndPosition(Frame owner) {
        // 设置初始大小
        setMinimumSize(new Dimension(550, 650));
        setPreferredSize(new Dimension(600, 700));

        // 调整到适当尺寸
        pack();

        // 居中显示
        setLocationRelativeTo(owner);
    }

    /**
     * 关闭对话框，如有未保存数据则提示
     */
    private void closeDialog() {
        if (dataChanged) {
            int option = JOptionPane.showConfirmDialog(
                    this,
                    "有未保存的更改，确定要关闭吗？",
                    "确认关闭",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (option != JOptionPane.YES_OPTION) {
                return;
            }
        }

        confirmed = false;
        setVisible(false);
    }

    /**
     * 显示帮助信息
     */
    private void showHelp() {
        JOptionPane.showMessageDialog(
                this,
                "设备配置帮助\n\n" +
                        "1. 设备名称: 输入唯一的设备标识名称\n" +
                        "2. 设备型号: 输入设备的型号\n" +
                        "3. 设备厂商: 输入设备的制造商\n" +
                        "4. 连接类型: 选择设备连接方式\n\n" +
                        "根据不同的连接类型，填写相应的连接参数。\n" +
                        "所有带*的字段都是必填项。",
                "帮助",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 从表单创建设备对象
     *
     * @return 设备对象
     */
    public Device createDeviceFromForm() {
        String id = originalDevice != null ? originalDevice.getId() : String.valueOf(SnowflakeIdGenerator.getInstance().nextId());
        DeviceStatus status = originalDevice != null ? originalDevice.getStatus() : DeviceStatus.DISCONNECTED;

        return Device.builder()
                .id(id)
                .name(configPanel.getNameField().getText().trim())
                .model(configPanel.getModelField().getText().trim())
                .manufacturer(configPanel.getManufacturerField().getText().trim())
                .connectionType(configPanel.getSelectedConnectionType())
                .connectionParams(configPanel.getConnectionParams())
                .status(status)
                .build();
    }

    /**
     * 设置设备信息到表单
     *
     * @param device 设备对象
     */
    public void setDeviceInfo(Device device) {
        this.originalDevice = device;
        this.dataChanged = false;
        configPanel.setDeviceInfo(device);

        // 如果是编辑模式，更新窗口标题
        if (device != null) {
            setTitle("编辑设备 - " + device.getName());
        }

        // 重置并给第一个字段焦点
        SwingUtilities.invokeLater(() -> configPanel.getNameField().requestFocus());
    }

    /**
     * 显示对话框并返回是否确认
     *
     * @return 是否确认
     */
    public boolean showDialog() {
        // 重置状态
        confirmed = false;
        dataChanged = false;

        // 显示对话框
        setVisible(true);

        return confirmed;
    }

    /**
     * 监听数据变化的文档监听器
     */
    private class DataChangeListener implements javax.swing.event.DocumentListener {
        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent e) {
            dataChanged = true;
        }

        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent e) {
            dataChanged = true;
        }

        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent e) {
            dataChanged = true;
        }
    }
}
