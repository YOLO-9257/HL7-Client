package com.hl7.client.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson配置类
 * 提供ObjectMapper等Jackson相关的Bean
 */
@Configuration
public class JacksonConfig {

    /**
     * 创建ObjectMapper bean
     *
     * @return 配置好的ObjectMapper实例
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 启用格式化输出
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        // 忽略未知属性
        //objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }
}
