package com.hl7.client.domain.service.impl;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.datatype.CE;
import ca.uhn.hl7v2.model.v25.datatype.ST;
import ca.uhn.hl7v2.model.v25.datatype.TS;
import ca.uhn.hl7v2.model.v25.message.ORU_R01;
import ca.uhn.hl7v2.model.v25.segment.MSH;
import ca.uhn.hl7v2.model.v25.segment.OBR;
import ca.uhn.hl7v2.model.v25.segment.OBX;
import ca.uhn.hl7v2.parser.Parser;
import cn.hutool.core.text.CharSequenceUtil;
import com.hl7.client.domain.model.MessageType;
import com.hl7.client.domain.service.MessageParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * HL7消息解析器
 * 解析标准HL7消息
 */
@Slf4j
@Service
public class Hl7MessageParser implements MessageParser {

    private final HapiContext context = new DefaultHapiContext();

    @Override
    public Map<String, Object> parse(com.hl7.client.domain.model.Message domainMessage) {
        try {
            // 创建解析结果Map
            Map<String, Object> result = new HashMap<>();

            // 解析原始HL7消息
            Parser parser = context.getPipeParser();
            Message hapiMessage = parser.parse(domainMessage.getRawContent());

            // 如果是ORU_R01消息（常见的结果消息类型）
            if (hapiMessage instanceof ORU_R01) {
                return parseORU_R01Message((ORU_R01) hapiMessage);
            } else {
                // 通用处理其他类型的HL7消息
                result.put("messageType", hapiMessage.getName());

                // 获取MSH段信息
                MSH msh = (MSH) hapiMessage.get("MSH");
                result.put("sendingApplication", msh.getSendingApplication().getNamespaceID().getValue());
                result.put("sendingFacility", msh.getSendingFacility().getNamespaceID().getValue());
                result.put("receivingApplication", msh.getReceivingApplication().getNamespaceID().getValue());
                result.put("messageControlId", msh.getMessageControlID().getValue());
                result.put("messageDateTime", getTimeValue(msh.getDateTimeOfMessage()));
            }

            return result;
        } catch (HL7Exception e) {
            log.error("解析HL7消息失败: {}", e.getMessage());
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", true);
            errorResult.put("errorMessage", "解析HL7消息失败: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * 从TS类型获取时间值
     *
     * @param ts 时间戳类型
     * @return 时间字符串
     */
    private String getTimeValue(TS ts) {
        if (ts == null) {
            return null;
        }
        try {
            return ts.getTime().getValue();
        } catch (Exception e) {
            log.warn("获取时间值时出错: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析ORU_R01消息
     *
     * @param oruR01 ORU_R01消息
     * @return 解析结果
     * @throws HL7Exception HL7异常
     */
    private Map<String, Object> parseORU_R01Message(ORU_R01 oruR01) throws HL7Exception {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> observations = new ArrayList<>();

        // 获取基本信息
        MSH msh = oruR01.getMSH();
        result.put("messageType", "ORU_R01");
        result.put("sendingApplication", msh.getSendingApplication().getNamespaceID().getValue());
        result.put("sendingFacility", msh.getSendingFacility().getNamespaceID().getValue());
        result.put("messageControlId", msh.getMessageControlID().getValue());
        result.put("messageDateTime", getTimeValue(msh.getDateTimeOfMessage()));

        // 获取患者信息
        result.put("patientId", oruR01.getPATIENT_RESULT().getPATIENT().getPID().getPatientID().getIDNumber().getValue());
        result.put("patientName", oruR01.getPATIENT_RESULT().getPATIENT().getPID().getPatientName(0).getFamilyName().getSurname().getValue());

        // 获取OBR信息（请求信息）
        OBR obr = oruR01.getPATIENT_RESULT().getORDER_OBSERVATION(0).getOBR();
        result.put("observationDateTime", getTimeValue(obr.getObservationDateTime()));
        result.put("orderNumber", obr.getPlacerOrderNumber().getEntityIdentifier().getValue());
        result.put("universalServiceID", obr.getUniversalServiceIdentifier().getIdentifier().getValue());

        // 获取OBX信息（结果信息）
        int obsCount = oruR01.getPATIENT_RESULT().getORDER_OBSERVATION(0).getOBSERVATIONReps();
        for (int i = 0; i < obsCount; i++) {
            OBX obx = oruR01.getPATIENT_RESULT().getORDER_OBSERVATION(0).getOBSERVATION(i).getOBX();

            Map<String, Object> observation = new HashMap<>();
            observation.put("sequence", obx.getSetIDOBX().getValue());

            // 测试项目信息
            CE ce = obx.getObservationIdentifier();
            observation.put("testId", ce.getIdentifier().getValue());
            observation.put("testName", ce.getText().getValue());

            // 结果信息
            if (obx.getValueType().getValue().equals("ST")) {
                ST st = (ST) obx.getObservationValue(0).getData();
                observation.put("value", st.getValue());
            } else {
                observation.put("value", obx.getObservationValue(0).encode());
            }

            // 单位和参考范围
            observation.put("units", obx.getUnits().getIdentifier().getValue());
            observation.put("referenceRange", obx.getReferencesRange().getValue());

            // 结果状态
            observation.put("status", obx.getObservationResultStatus().getValue());

            observations.add(observation);
        }

        result.put("observations", observations);
        return result;
    }

    @Override
    public String getType() {
        return MessageType.HL7.name();
    }

    @Override
    public boolean supports(com.hl7.client.domain.model.Message message) {
        // 检查消息类型是否为HL7
        boolean typeMatches = CharSequenceUtil.isBlank(message.getMessageProcessor()) &&
                Objects.equals(message.getMessageType(), getType());

        // 如果消息类型匹配并且不是指定型号的设备，则此解析器支持处理
        // 这是为了避免与设备特定的解析器冲突
        if (typeMatches && !"Test01".equals(message.getDeviceModel())) {
            return true;
        }

        return false;
    }

    @Override
    public String checkMessageCompleteness(com.hl7.client.domain.model.Message message) {
        return "";
    }
}
