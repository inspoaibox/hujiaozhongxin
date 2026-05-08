package com.qianniuyun.call.service;

import com.qianniuyun.call.entity.Call;
import com.qianniuyun.call.freeswitch.FreeSwitchClient;
import com.qianniuyun.call.repository.CallRepository;
import com.qianniuyun.common.enums.CallStatus;
import com.qianniuyun.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 呼叫服务单元测试
 * 作者：深圳市千牛云科技有限公司
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("呼叫管理服务测试")
class CallServiceTest {

    @Mock
    private CallRepository callRepository;

    @Mock
    private FreeSwitchClient freeSwitchClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private CallService callService;

    private Call testCall;

    @BeforeEach
    void setUp() {
        testCall = new Call();
        testCall.setId("test-call-001");
        testCall.setCallType("INBOUND");
        testCall.setStatus(CallStatus.ANSWERED);
        testCall.setCallerNumber("13800138000");
        testCall.setAgentId(1L);
        testCall.setAnswerAt(LocalDateTime.now().minusMinutes(5));
        testCall.setCreatedAt(LocalDateTime.now().minusMinutes(5));
    }

    @Test
    @DisplayName("呼入呼叫创建 - 应正确保存通话记录")
    void handleInboundCallCreated_ShouldSaveCallRecord() {
        when(callRepository.save(any(Call.class))).thenAnswer(i -> i.getArgument(0));

        Call result = callService.handleInboundCallCreated(
                "call-001", "13800138000", "4008001234");

        assertThat(result.getCallType()).isEqualTo("INBOUND");
        assertThat(result.getStatus()).isEqualTo(CallStatus.RINGING);
        assertThat(result.getCallerNumber()).isEqualTo("13800138000");
        verify(callRepository).save(any(Call.class));
        verify(kafkaTemplate).send(eq("qianniu.call.events"), anyString(), any());
    }

    @Test
    @DisplayName("呼叫挂断 - 应正确计算通话时长")
    void handleCallHangup_ShouldCalculateDuration() {
        when(callRepository.findById("test-call-001")).thenReturn(Optional.of(testCall));
        when(callRepository.save(any(Call.class))).thenAnswer(i -> i.getArgument(0));

        callService.handleCallHangup("test-call-001", "NORMAL_CLEARING");

        assertThat(testCall.getStatus()).isEqualTo(CallStatus.COMPLETED);
        assertThat(testCall.getDuration()).isGreaterThan(0);
        assertThat(testCall.getHangupAt()).isNotNull();
    }

    @Test
    @DisplayName("转接呼叫 - 非通话中状态应抛出异常")
    void transferCall_WhenNotAnswered_ShouldThrowException() {
        testCall.setStatus(CallStatus.QUEUED);
        when(callRepository.findById("test-call-001")).thenReturn(Optional.of(testCall));

        assertThatThrownBy(() -> callService.transferCall("test-call-001", 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有通话中的呼叫才能转接");
    }

    @Test
    @DisplayName("满意度评分 - 超出范围应抛出异常")
    void recordSatisfaction_WithInvalidScore_ShouldThrowException() {
        when(callRepository.findById("test-call-001")).thenReturn(Optional.of(testCall));

        assertThatThrownBy(() -> callService.recordSatisfaction("test-call-001", 6))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("满意度评分必须在1-5之间");

        assertThatThrownBy(() -> callService.recordSatisfaction("test-call-001", 0))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("满意度评分 - 有效评分应正确保存")
    void recordSatisfaction_WithValidScore_ShouldSave() {
        when(callRepository.findById("test-call-001")).thenReturn(Optional.of(testCall));
        when(callRepository.save(any(Call.class))).thenAnswer(i -> i.getArgument(0));

        callService.recordSatisfaction("test-call-001", 5);

        assertThat(testCall.getSatisfaction()).isEqualTo(5);
        verify(callRepository).save(testCall);
    }
}
