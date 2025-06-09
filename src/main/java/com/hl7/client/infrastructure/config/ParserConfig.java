package com.hl7.client.infrastructure.config;

import com.hl7.client.domain.service.MessageParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息解析器配置
 * 根据设备型号配置不同的解析器
 */
@Slf4j
@Configuration
public class ParserConfig {

    /**
     * 创建设备型号与解析器的映射关系
     *
     * @param parsers 系统中所有的解析器
     * @return 设备型号对应的解析器映射
     */
    @Bean("deviceModelParserMap")
    public Map<String, MessageParser> deviceModelParserMap(List<MessageParser> parsers) {
        Map<String, MessageParser> modelParserMap = new HashMap<>();

        // 遍历所有解析器，查找其支持的设备型号
        for (MessageParser parser : parsers) {
            try {
                String parserType = parser.getType();
                log.info("注册解析器: {}", parserType);

                // 为特定设备型号注册解析器
                // 例如: Test01型号设备使用Test01Parser
                if ("Test01".equals(parserType)) {
                    modelParserMap.put("Test01", parser);
                    log.info("为设备型号 {} 注册了解析器 {}", "Test01", parserType);
                }
                // 可以根据需要继续添加更多的设备型号与解析器映射

            } catch (Exception e) {
                log.error("注册解析器时出错: {}", e.getMessage(), e);
            }
        }

        log.info("设备型号解析器映射配置完成，共配置 {} 个映射", modelParserMap.size());
        return modelParserMap;
    }
}
