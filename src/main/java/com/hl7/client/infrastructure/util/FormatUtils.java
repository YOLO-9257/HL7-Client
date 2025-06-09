package com.hl7.client.infrastructure.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 格式化工具类
 * 用于格式化日期、字符串等常用数据类型
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FormatUtils {

    /**
     * 默认日期时间格式
     */
    private static final DateTimeFormatter DEFAULT_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * HL7日期时间格式
     */
    private static final DateTimeFormatter HL7_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 将LocalDateTime格式化为默认格式的字符串
     *
     * @param dateTime 日期时间
     * @return 格式化后的字符串
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DEFAULT_DATE_TIME_FORMATTER);
    }

    /**
     * 将LocalDateTime格式化为HL7格式的字符串
     *
     * @param dateTime 日期时间
     * @return 格式化后的字符串
     */
    public static String formatHL7DateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(HL7_DATE_TIME_FORMATTER);
    }

    /**
     * 解析HL7格式的日期时间字符串
     *
     * @param dateTimeString HL7格式的日期时间字符串
     * @return LocalDateTime对象
     */
    public static LocalDateTime parseHL7DateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeString, HL7_DATE_TIME_FORMATTER);
    }

    /**
     * 将Map格式化为字符串
     *
     * @param map 要格式化的Map
     * @return 格式化后的字符串
     */
    public static String formatMap(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }

        return map.entrySet().stream()
                .map(entry -> String.format("\"%s\": \"%s\"", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    /**
     * 截断字符串
     *
     * @param str 要截断的字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串
     */
    public static String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }

    /**
     * 安全地获取字符串
     *
     * @param obj 对象
     * @return 字符串，如果对象为null则返回空字符串
     */
    public static String safeString(Object obj) {
        return obj == null ? "" : obj.toString();
    }
}
