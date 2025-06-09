package com.hl7.client.domain.service;

import com.hl7.client.domain.constants.NormalSymbol;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * HL7消息解析器
 * 负责验证和解析HL7消息
 */
@Slf4j
@Service
public class HL7MessageParser {

    // HL7消息格式常量
    private static final String SEGMENT_SEPARATOR = NormalSymbol.ENTER;
    private static final String FIELD_SEPARATOR = NormalSymbol.HL7_SPLIT;
    private static final Pattern MSH_PATTERN = Pattern.compile("^MSH\\|.*");
    private static final String HL7_VERSION = "2.3";
    private static final String HL7_PROCESSING_ID = "P";
    private static final String HL7_ACCEPT_ACK_TYPE = "AL";
    private static final String HL7_APP_ACK_TYPE = "NE";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 验证HL7消息格式
     *
     * @param message HL7消息
     * @return 是否为有效的HL7消息
     */
    public boolean validateHL7Message(String message) {
        if (message == null || message.isEmpty()) {
            log.warn("消息为空，验证失败");
            return false;
        }

        // 检查MSH段
        String[] segments = message.split(SEGMENT_SEPARATOR);
        if (segments.length == 0) {
            log.warn("消息没有段，验证失败");
            return false;
        }

        // 验证MSH段格式
        boolean valid = MSH_PATTERN.matcher(segments[0]).matches();
        if (!valid) {
            log.warn("MSH段格式无效: {}", segments[0]);
        }
        return valid;
    }

    /**
     * 异步解析HL7消息
     *
     * @param message HL7消息
     * @return 包含解析结果的CompletableFuture
     */
    public CompletableFuture<List<String>> parseSegmentsAsync(String message) {
        return CompletableFuture.supplyAsync(() -> parseSegments(message));
    }

    /**
     * 解析HL7消息段
     *
     * @param message HL7消息
     * @return 消息段列表
     */
    public List<String> parseSegments(String message) {
        List<String> segments = new ArrayList<>();

        if (message == null || message.isEmpty()) {
            log.warn("消息为空，解析段失败");
            return segments;
        }

        // 尝试使用不同的分隔符分割消息
        String[] segmentArray = message.split(SEGMENT_SEPARATOR);
        if (segmentArray.length <= 1) {
            // 如果没有分割出多个段，尝试使用其他可能的分隔符
            segmentArray = message.split(NormalSymbol.NEW_LINE);
            if (segmentArray.length <= 1) {
                segmentArray = message.split(NormalSymbol.ENTER_NEW_LINE);
            }
        }

        // 添加有效段到结果列表
        for (String segment : segmentArray) {
            String trimmed = segment.trim();
            if (!trimmed.isEmpty()) {
                segments.add(trimmed);
                log.debug("解析到段: {}", trimmed);
            }
        }

        return segments;
    }

    /**
     * 解析HL7消息段的字段
     *
     * @param segment HL7消息段
     * @return 字段列表
     */
    public List<String> parseFields(String segment) {
        List<String> fields = new ArrayList<>();

        if (segment == null || segment.isEmpty()) {
            log.warn("段为空，解析字段失败");
            return fields;
        }

        // 按字段分隔符分割段
        for (String field : segment.split(FIELD_SEPARATOR, -1)) {
            fields.add(field);
            log.debug("解析到字段: {}", field);
        }

        return fields;
    }

    /**
     * 异步解析字段
     *
     * @param segment HL7消息段
     * @return 包含字段列表的CompletableFuture
     */
    public CompletableFuture<List<String>> parseFieldsAsync(String segment) {
        return CompletableFuture.supplyAsync(() -> parseFields(segment));
    }

    /**
     * 获取HL7消息中特定段的特定字段值
     *
     * @param message HL7消息
     * @param segmentType 段类型（如MSH, PID, OBX等）
     * @param fieldIndex 字段索引（从1开始）
     * @return 字段值，如果不存在则返回null
     */
    public String getFieldValue(String message, String segmentType, int fieldIndex) {
        List<String> segments = parseSegments(message);

        for (String segment : segments) {
            if (segment.startsWith(segmentType)) {
                List<String> fields = parseFields(segment);

                // 字段索引检查
                if (fieldIndex > 0 && fieldIndex < fields.size()) {
                    return fields.get(fieldIndex);
                }

                break;
            }
        }

        return null;
    }

    /**
     * 生成一个标准格式的MSH段
     *
     * @param sendingApp 发送应用程序
     * @param sendingFacility 发送机构
     * @param receivingApp 接收应用程序
     * @param receivingFacility 接收机构
     * @param messageType 消息类型
     * @param messageId 消息ID
     * @return MSH段字符串
     */
    public String generateMSHSegment(
            String sendingApp,
            String sendingFacility,
            String receivingApp,
            String receivingFacility,
            String messageType,
            String messageId) {

        String dateTime = LocalDateTime.now().format(DATE_TIME_FORMATTER);

        return String.format("MSH|^~\\&|%s|%s|%s|%s|%s||%s|%s|%s|%s|||%s|%s|",
                sendingApp, sendingFacility, receivingApp, receivingFacility,
                dateTime, messageType, messageId, HL7_PROCESSING_ID, HL7_VERSION,
                HL7_ACCEPT_ACK_TYPE, HL7_APP_ACK_TYPE);
    }
}
