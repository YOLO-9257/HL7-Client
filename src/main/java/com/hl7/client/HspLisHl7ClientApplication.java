/*
    HL7 解析服务对应数据分发客户端
    支持配置持久化存储，在系统重启后自动加载
 */
package com.hl7.client;

import com.hl7.client.application.DeviceManager;
import com.hl7.client.application.MessageProcessor;
import com.hl7.client.domain.constants.ApplicationConstants;
import com.hl7.client.infrastructure.service.CommunicationMonitorService;
import com.hl7.client.infrastructure.util.UIHelper;
import com.hl7.client.interfaces.window.MainFrame;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.swing.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 启动类
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
@EnableAsync(proxyTargetClass = true)
@ConfigurationPropertiesScan
@EnableConfigurationProperties
public class HspLisHl7ClientApplication {

    @SneakyThrows
    public static void main(String[] args) {
        // 设置全局字体大小
        UIHelper.setGlobalFontSize(ApplicationConstants.UI.GLOBAL_FONT_SIZE);

        // 启动Spring应用
        SpringApplicationBuilder builder = new SpringApplicationBuilder(HspLisHl7ClientApplication.class);
        ConfigurableApplicationContext context = builder.headless(false).web(WebApplicationType.NONE).run(args);

        // 判断是否重复启动
        if (isAlreadyRunning()) {
            JOptionPane.showConfirmDialog(null, "请勿重复打开");
            System.exit(0);
        }

        // 启动单实例检测服务
        startSingleInstanceService();

        // 初始化应用程序
        initializeApplication(context);

        log.info("纯牛马启动器");
        log.info("=============================================================================================================================================");
        log.info("\r"+"                                         __,----.___                \n" +
            "                               ______,-'           `--.__           \n" +
            "      ,-'~~~~~~`--------'~~~~~~         //         ______`.      /\\ \n" +
            "     / ,`   ,   ,~~                   .'(________;'|:|::'`.______) ;\n" +
            "    :.' '  /     '                    `(________;:::;::::;:________)\n" +
            "    `: , / '                   ,          ___,-'|\"`'\"`;;\" `|`-.__   \n" +
            "    ,' `; ;                   /   ;      `.   __|{0     0} ;  ___/  \n" +
            "    : ; ;            ;\",     ,  ;  ,       `-'  :  ,' `.  /--'      \n" +
            "   :   ;   \"          ;`         ; `    /        ):c~-~c:(          \n" +
            "   `; /   \":        ;  ,    \"  \"       ;         \\ `---' /          \n" +
            "   : ; ; \";`  ,    ;  ;`,       \"      ` ;;      /`~~~~~'           \n" +
            "   `;  ; / :  ` \"     , ` ,   \"   :   ;    ;;    |                  \n" +
            "    ;/\\.`, ;  ;  /: : `   `      ; : ,'         (;                  \n" +
            "        \"-.     ( `:`.; ;;;;;\" ;; ;` `.| ;    :;;;                  \n" +
            "          (\"~\"~\";`~\"~`;:;; :\":\": :::   ;    :;`` :                  \n" +
            "           :   /`.   : `\"\"`:;;;;~~~~`'\"|:;\"|`;   ;                  \n" +
            "           |   : ;___(_    ````       .;   : :   `.                 \n" +
            "           )___(_`-.###\\              ; ___`_ `.__`_                \n" +
            "           `-.###\\ ~~~~~               `-.###\\ ;####\\               \n" +
            "             ~~~~~                        ~~~~'~~~~~~               ");
        log.info("=============================================================================================================================================");
    }


    /**
     * 初始化应用程序
     *
     * @param context Spring上下文
     */
    private static void initializeApplication(ConfigurableApplicationContext context) {
        log.info("正在初始化应用程序...");

        // 获取所需的Bean
        DeviceManager deviceManager = context.getBean(DeviceManager.class);
        MessageProcessor messageProcessor = context.getBean(MessageProcessor.class);

        // 确保通信监控服务已启动
        try {
            CommunicationMonitorService monitorService = context.getBean(CommunicationMonitorService.class);
            log.info("通信监控服务已注册");
        } catch (Exception e) {
            log.error("获取通信监控服务失败: {}", e.getMessage());
        }

        // 渲染主界面
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

                // 从Spring容器中获取MainFrame
                MainFrame mainFrame = context.getBean(MainFrame.class);
                mainFrame.setVisible(true);

                log.info("应用程序界面已启动");
            } catch (Exception e) {
                log.error("初始化UI失败: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * 检查程序是否已经在运行
     *
     * @return 是否已经运行
     */
    private static boolean isAlreadyRunning() {
        try {
            new Socket(ApplicationConstants.Network.LOCALHOST, ApplicationConstants.Network.SINGLE_INSTANCE_PORT);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 启动单实例检测服务
     */
    private static void startSingleInstanceService() {
        Thread serverThread = new Thread(new SocketServerHelper(
                String.valueOf(ApplicationConstants.Network.SINGLE_INSTANCE_PORT),
                ApplicationConstants.Network.LOCALHOST));
        serverThread.setDaemon(true);
        serverThread.start();
        log.info("单实例检测服务已启动");
    }

    /**
     * Socket服务辅助类
     * 用于检测程序是否已经在运行
     */
    static class SocketServerHelper implements Runnable {
        private final String port;
        private final String ip;

        SocketServerHelper(String port, String ip) {
            this.port = port;
            this.ip = ip;
        }

        @Override
        public void run() {
            ServerSocket serverSocket;
            try {
                if (Strings.isNotBlank(ip)) {
                    InetAddress addr = InetAddress.getByName(ip);
                    serverSocket = new ServerSocket(Integer.parseInt(port), 50, addr);
                } else {
                    serverSocket = new ServerSocket(Integer.parseInt(port));
                }
            } catch (IOException e) {
                log.error("创建单实例检测服务失败: {}", e.getMessage());
                return;
            }

            try {
                while (true) {
                    Socket socket = serverSocket.accept();
                    socket.close();
                }
            } catch (IOException e) {
                log.error("单实例检测服务运行异常: {}", e.getMessage());
            }
        }
    }
}

