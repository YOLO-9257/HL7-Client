package com.hl7.client.domain.service;

import com.hl7.client.domain.model.Message;
import com.hl7.client.domain.model.MessageStatus;
import com.hl7.client.infrastructure.exception.MessageProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MessageProcessService单元测试
 */
@ExtendWith(MockitoExtension.class)
class MessageProcessServiceTest {

    @Mock
    private MessageParserFactory messageParserFactory;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private MessageProcessService messageProcessService;

    private Message testMessage;
    private Map<String, Object> parsedData;

    @BeforeEach
    void setUp() {
        // 设置服务器地址
        ReflectionTestUtils.setField(messageProcessService, "serverAddress", "http://test-server/api");

        // 创建测试消息
        testMessage = Message.builder()
                .id(UUID.randomUUID().toString())
                .deviceId("test-device")
                .messageType("HL7")
                .rawContent("MSH|^~\\&|SENDING|FACILITY|RECEIVING|FACILITY|20220418150059||ADT^A01|1234567|P|2.3|||")
                .receivedTime(LocalDateTime.now())
                .status(MessageStatus.NEW.name())
                .build();

        // 创建解析结果
        parsedData = new HashMap<>();
        parsedData.put("messageType", "ADT^A01");
        parsedData.put("patientId", "12345");
        parsedData.put("patientName", "Test Patient");
    }

    @Test
    @DisplayName("成功处理消息")
    void processMessage_Success() {
        // 准备测试数据
        when(messageParserFactory.parseMessage(testMessage)).thenReturn(parsedData);

        // 执行测试
        Message result = messageProcessService.processMessage(testMessage);

        // 验证结果
        assertEquals(MessageStatus.PROCESSED.name(), result.getStatus());
        assertTrue(result.getProcessResult().contains("解析成功"));
        verify(messageParserFactory, times(1)).parseMessage(testMessage);
    }

    @Test
    @DisplayName("处理不完整消息")
    void processMessage_Incomplete() {
        // 准备测试数据
        parsedData.put("INCOMPLETE", false);
        when(messageParserFactory.parseMessage(testMessage)).thenReturn(parsedData);

        // 执行测试
        Message result = messageProcessService.processMessage(testMessage);

        // 验证结果
        assertEquals(MessageStatus.INCOMPLETE.name(), result.getStatus());
        verify(messageParserFactory, times(1)).parseMessage(testMessage);
    }

    @Test
    @DisplayName("处理失败的消息")
    void processMessage_Error() {
        // 准备测试数据
        parsedData.put("error", true);
        parsedData.put("errorMessage", "解析失败");
        when(messageParserFactory.parseMessage(testMessage)).thenReturn(parsedData);

        // 执行测试
        Message result = messageProcessService.processMessage(testMessage);

        // 验证结果
        assertEquals(MessageStatus.ERROR.name(), result.getStatus());
        assertTrue(result.getProcessResult().contains("解析失败"));
        verify(messageParserFactory, times(1)).parseMessage(testMessage);
    }

    @Test
    @DisplayName("异步处理消息")
    void processMessageAsync_Success() throws ExecutionException, InterruptedException, TimeoutException {
        // 准备测试数据
        when(messageParserFactory.parseMessage(testMessage)).thenReturn(parsedData);

        // 执行测试
        CompletableFuture<Message> futureResult = messageProcessService.processMessageAsync(testMessage);
        Message result = futureResult.get(5, TimeUnit.SECONDS);

        // 验证结果
        assertEquals(MessageStatus.PROCESSED.name(), result.getStatus());
        assertTrue(result.getProcessResult().contains("解析成功"));
        verify(messageParserFactory, times(1)).parseMessage(testMessage);
    }

    @Test
    @DisplayName("发送消息到服务端")
    void sendToServer_Success() {
        // 准备测试数据
        testMessage.setStatus(MessageStatus.PROCESSED.name());
        testMessage.setProcessResult("解析成功");

        // 模拟HTTP响应
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", "消息接收成功");

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(responseEntity);

        // 执行测试
        boolean result = messageProcessService.sendToServer(testMessage);

        // 验证结果
        assertTrue(result);
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(Map.class));
    }

    @Test
    @DisplayName("发送消息失败")
    void sendToServer_Failure() {
        // 准备测试数据
        testMessage.setStatus(MessageStatus.PROCESSED.name());
        testMessage.setProcessResult("解析成功");

        // 模拟HTTP响应
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", false);
        responseBody.put("message", "消息接收失败");

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.BAD_REQUEST);
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(responseEntity);

        // 执行测试
        boolean result = messageProcessService.sendToServer(testMessage);

        // 验证结果
        assertFalse(result);
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(Map.class));
    }

    @Test
    @DisplayName("发送消息异常")
    void sendToServer_Exception() {
        // 准备测试数据
        testMessage.setStatus(MessageStatus.PROCESSED.name());
        testMessage.setProcessResult("解析成功");

        // 模拟HTTP请求异常
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("连接失败"));

        // 执行测试并验证异常
        assertThrows(MessageProcessingException.class, () -> messageProcessService.sendToServer(testMessage));
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(Map.class));
    }

    @Test
    @DisplayName("重试失败消息成功")
    void retryFailedMessage_Success() {
        // 准备测试数据
        testMessage.setStatus(MessageStatus.ERROR.name());

        // 模拟HTTP响应
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(responseEntity);

        // 测试前添加到失败消息队列
        Map<String, Message> failedMessages = new HashMap<>();
        failedMessages.put(testMessage.getId(), testMessage);
        ReflectionTestUtils.setField(messageProcessService, "failedMessages", failedMessages);

        // 执行测试
        boolean result = messageProcessService.retryFailedMessage(testMessage);

        // 验证结果
        assertTrue(result);
        assertTrue(failedMessages.isEmpty());
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(Map.class));
    }
}
