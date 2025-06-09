package com.hl7.client.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import com.hl7.client.application.DeviceManager;
import com.hl7.client.application.MessageProcessor;
import com.hl7.client.infrastructure.factory.DeviceAdapterCache;
import com.hl7.client.interfaces.window.MainFrame;

/**
 * 应用程序Bean配置类
 * 解决循环依赖问题
 *
 * 循环依赖问题说明：
 * - DeviceManager 依赖 MessageProcessor
 * - MessageProcessor 依赖 DeviceManager
 * - MainFrame 依赖 DeviceManager 和 MessageProcessor
 *
 * 解决方案：
 * 1. 在MessageProcessor中已经使用@Lazy注解引用DeviceManager
 * 2. 这里我们从MainFrame中移除@Component注解
 * 3. 使用@Bean、@Lazy和@Primary手动创建MainFrame实例
 * 4. @Lazy使Spring容器在需要时才初始化MainFrame
 * 5. @Primary确保这个Bean作为MainFrame的主要实现被注入
 */
@Configuration
public class BeanConfig {

    /**
     * 创建MainFrame Bean
     * 使用@Lazy注解解决循环依赖问题
     *
     * @param deviceManager 设备管理器
     * @param messageProcessor 消息处理器
     * @param adapterCache 设备适配器缓存
     * @return MainFrame实例
     */
    @Bean
    @Lazy
    @Primary
    public MainFrame mainFrame(DeviceManager deviceManager,
                             MessageProcessor messageProcessor,
                             DeviceAdapterCache adapterCache) {
        return new MainFrame(deviceManager, messageProcessor, adapterCache);
    }

    /**
     * 配置RestTemplate Bean
     * 用于HTTP请求
     *
     * @param builder RestTemplateBuilder
     * @return RestTemplate实例
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 连接超时5秒
        factory.setReadTimeout(10000);   // 读取超时10秒

        RestTemplate restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();

        restTemplate.setRequestFactory(factory);

        return restTemplate;
    }
}
