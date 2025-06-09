package com.hl7.client.domain.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONUtil;
import com.hl7.client.domain.model.Hl7StorageModel;
import com.hl7.client.domain.model.Message;
import com.hl7.client.domain.model.MessageType;
import com.hl7.client.domain.constants.NormalSymbol;
import com.hl7.client.domain.model.TargetDatabaseCode;
import com.hl7.client.domain.service.MessageParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * HL7消息解析器
 * 解析特定仪器格式的HL7消息，专用于凝血仪数据解析
 */
@Slf4j
@Service
public class Test01Parser implements MessageParser {

    // 字段定义常量，增强代码可读性
    private static final String RECORD_TYPE_R = "R";
    private static final String RECORD_TYPE_H = "H";
    private static final String RECORD_TYPE_O = "O";

    // 特定通道标识符常量
    private static final String CHANNEL_PT = "PT";
    private static final String CHANNEL_FIB = "FIB";
    private static final String CHANNEL_D_DIMER = "D-Dimer";

    // 结果单位标识
    private static final String UNIT_S_OR_INR = "s";
    private static final String UNIT_INR = "INR";
    private static final String UNIT_G_L = "g/L";
    private static final String UNIT_UG_ML = "ug/mL";

    // 消息状态常量
    private static final String STATUS_INCOMPLETE = "INCOMPLETE";
    private static final String STATUS_COMPLETE = "COMPLETE";
    private static final String PROCESS_RESULT_COMPLETE = "消息完整";
    private static final String PROCESS_RESULT_ACK = "ack";

    // 索引常量，避免魔术数字
    private static final int INDEX_RECORD_TYPE = 0;
    private static final int INDEX_CHANNEL_INFO = 2;
    private static final int INDEX_RESULT_VALUE = 3;
    private static final int INDEX_RESULT_UNIT = 4;
    private static final int INDEX_TEST_DATE = 13;
    private static final int INDEX_SAMPLE_ID = 1;

    // 日期格式常量
    private static final int DATE_LENGTH = 8;

    @Override
    public Map<String, Object> parse(Message domainMessage) {
        Map<String, Object> result = new HashMap<>();

        try {

            String messageContent = domainMessage.getRawContent();
            if (CharSequenceUtil.isBlank(messageContent)) {
                log.warn("消息内容为空，无法解析");
                result.put(STATUS_INCOMPLETE, false);
                domainMessage.setProcessResult("消息内容为空");
                return result;
            }

            // 解析消息内容
            List<Hl7StorageModel> parsedModels = parseMessageContent(messageContent);

            // 记录解析结果
            if (!parsedModels.isEmpty()) {
                result.put("models", JSONUtil.toJsonStr(parsedModels));
                //目标数据库（aid,emis等）
                result.put("targetDatabase", TargetDatabaseCode.EMIS.name());
                result.put(STATUS_COMPLETE, true);

                // 将解析结果输出到日志
                log.info("解析结果: {}", JSONUtil.toJsonStr(parsedModels));
            } else {
                log.warn("未能从消息中提取有效数据");
                result.put(STATUS_COMPLETE, false);
            }

            domainMessage.setProcessResult(PROCESS_RESULT_COMPLETE);
        } catch (Exception e) {
            log.error("解析消息时发生异常", e);
            result.put("error", e.getMessage());
            result.put(STATUS_COMPLETE, false);
            domainMessage.setProcessResult("解析异常: " + e.getMessage());
        }

        return result;
    }

    /**
     * 解析HL7消息内容，提取关键数据
     *
     * @param messageContent HL7消息内容
     * @return 解析后的存储模型列表
     */
    private List<Hl7StorageModel> parseMessageContent(String messageContent) {
        List<Hl7StorageModel> models = new ArrayList<>();
        Hl7StorageModel model = new Hl7StorageModel();
        List<Hl7StorageModel.Item> resultItems = new ArrayList<>();

        // 分行处理消息
        String[] lines = messageContent.split(NormalSymbol.NEW_LINE);

        for (String line : lines) {
            if (CharSequenceUtil.isBlank(line)) {
                continue;
            }

            String[] segments = line.split(NormalSymbol.HL7_SPLIT);
            if (segments.length == 0) {
                continue;
            }

            String recordType = segments[INDEX_RECORD_TYPE];

            // 处理结果记录行
            if (recordType.contains(RECORD_TYPE_R)) {
                processResultLine(segments, resultItems);
            }
            // 处理头部信息行
            else if (recordType.contains(RECORD_TYPE_H)) {
                processHeaderLine(segments, model);
            }
            // 处理订单信息行
            else if (recordType.contains(RECORD_TYPE_O)) {
                processOrderLine(segments, model);
            }
        }

        // 填充解析结果
        if (!resultItems.isEmpty()) {
            model.setItemList(resultItems);
            models.add(model);
        }

        return models;
    }

    /**
     * 处理结果行记录
     *
     * @param segments 分割后的行片段
     * @param resultItems 结果项集合
     */
    private void processResultLine(String[] segments, List<Hl7StorageModel.Item> resultItems) {
        if (segments.length <= Math.max(INDEX_CHANNEL_INFO, INDEX_RESULT_UNIT)) {
            log.warn("结果行段数不足，跳过: {}", String.join(", ", segments));
            return;
        }

        try {
            // 获取通道信息
            String channelInfo = segments[INDEX_CHANNEL_INFO];
            String[] channelParts = channelInfo.split(NormalSymbol.DIVISION);

            if (channelParts.length <= 3) {
                log.warn("通道信息格式不正确: {}", channelInfo);
                return;
            }

            String channelType = channelParts[3].trim();
            String resultValue = segments[INDEX_RESULT_VALUE];
            String resultUnit = segments[INDEX_RESULT_UNIT];

            // 根据不同通道和单位决定是否添加结果项
            Hl7StorageModel.Item item = new Hl7StorageModel.Item();

            if (CHANNEL_PT.equals(channelType)) {
                if (resultUnit.contains(UNIT_S_OR_INR) || resultUnit.contains(UNIT_INR)) {
                    item.setChannelCode(resultUnit.trim());
                    item.setResult(resultValue);
                    resultItems.add(item);
                }
            } else if (channelType.contains(CHANNEL_FIB)) {
                if (resultUnit.contains(UNIT_G_L)) {
                    item.setChannelCode(channelType);
                    item.setResult(resultValue);
                    resultItems.add(item);
                }
            } else if (channelType.contains(CHANNEL_D_DIMER)) {
                if (resultUnit.contains(UNIT_UG_ML)) {
                    item.setChannelCode(channelType);
                    item.setResult(resultValue);
                    resultItems.add(item);
                }
            } else {
                // 其他通道直接添加
                item.setChannelCode(channelType);
                item.setResult(resultValue);
                resultItems.add(item);
            }
        } catch (Exception e) {
            log.warn("处理结果行时发生异常: {}", e.getMessage());
        }
    }

    /**
     * 处理头部信息行
     *
     * @param segments 分割后的行片段
     * @param model 存储模型
     */
    private void processHeaderLine(String[] segments, Hl7StorageModel model) {
        if (segments.length <= INDEX_TEST_DATE) {
            log.warn("头部信息行段数不足，跳过");
            return;
        }

        try {
            String dateStr = segments[INDEX_TEST_DATE];
            if (dateStr.length() >= DATE_LENGTH) {
                model.setTestDate(dateStr.substring(0, DATE_LENGTH));
            } else {
                log.warn("测试日期格式不正确: {}", dateStr);
            }
        } catch (Exception e) {
            log.warn("处理头部行时发生异常: {}", e.getMessage());
        }
    }

    /**
     * 处理订单信息行
     *
     * @param segments 分割后的行片段
     * @param model 存储模型
     */
    private void processOrderLine(String[] segments, Hl7StorageModel model) {
        if (segments.length <= INDEX_CHANNEL_INFO) {
            log.warn("订单信息行段数不足，跳过");
            return;
        }

        try {
            String barcode = segments[INDEX_CHANNEL_INFO];
            String[] barcodeParts = barcode.split(NormalSymbol.DIVISION);

            if (barcodeParts.length <= INDEX_SAMPLE_ID) {
                log.warn("条码信息格式不正确: {}", barcode);
                return;
            }

            String sampleId = barcodeParts[INDEX_SAMPLE_ID];

            // 根据条码长度决定是样本编号还是条形码
            if (sampleId.length() < 5) {
                model.setSampleCode(sampleId);
            } else {
                model.setBarcode(sampleId);
            }
        } catch (Exception e) {
            log.warn("处理订单行时发生异常: {}", e.getMessage());
        }
    }

    @Override
    public String getType() {
        return MessageType.CUSTOM.name();
    }

    @Override
    public boolean supports(Message message) {
        // 否则使用原有逻辑
        return Objects.equals(message.getDeviceModel(), getClassName());
    }

    /**
     * 获取当前解析器的类名
     * @return 类名（简单名称，不含包名）
     */
    public String getClassName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 检查消息是否完整
     * HL7消息应以回车符(ASCII 13)结尾
     *
     * @param message 消息对象
     * @return 消息是否完整
     */
    @Override
    public String checkMessageCompleteness(Message message) {
        String content = message.getRawContent();
        // 检查消息是否以回车符结尾
        if (content.charAt(content.length() - 1) == 13) {
            return null;
        }
        return "ack";
    }
}
