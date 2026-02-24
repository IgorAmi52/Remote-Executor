package com.pekara.controller.application.service;

import com.pekara.common.json.JsonSerializer;
import com.pekara.controller.domain.model.Execution;
import com.pekara.controller.domain.model.ExecutionStatus;
import com.pekara.controller.domain.repository.ExecutionRepository;
import com.pekara.controller.dto.StatusMessageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatusUpdateServiceTest {

    @Mock
    private ExecutionRepository executionRepository;

    @Mock
    private JsonSerializer jsonSerializer;

    private StatusUpdateService service;

    @BeforeEach
    void setUp() {
        service = new StatusUpdateService(executionRepository, jsonSerializer);
    }

    @Test
    void process_updatesExecutionStatus() {
        // Given
        String taskId = "task-123";
        String messageJson = "{\"taskId\":\"task-123\",\"status\":\"IN_PROGRESS\"}";
        StatusMessageDto statusMessage = new StatusMessageDto(taskId, ExecutionStatus.IN_PROGRESS, null, 0);
        Execution execution = createExecution(taskId, ExecutionStatus.QUEUED);

        when(jsonSerializer.fromJson(messageJson, StatusMessageDto.class)).thenReturn(statusMessage);
        when(executionRepository.findById(taskId)).thenReturn(Optional.of(execution));

        // When
        service.process(messageJson);

        // Then
        verify(executionRepository).update(execution);
        verify(executionRepository).addStatusTransition(eq(taskId), eq(ExecutionStatus.IN_PROGRESS), any(Instant.class));
    }

    @Test
    void process_unknownTaskId_doesNotUpdate() {
        // Given
        String taskId = "unknown-task";
        String messageJson = "{\"taskId\":\"unknown-task\",\"status\":\"IN_PROGRESS\"}";
        StatusMessageDto statusMessage = new StatusMessageDto(taskId, ExecutionStatus.IN_PROGRESS, null, 0);

        when(jsonSerializer.fromJson(messageJson, StatusMessageDto.class)).thenReturn(statusMessage);
        when(executionRepository.findById(taskId)).thenReturn(Optional.empty());

        // When
        service.process(messageJson);

        // Then
        verify(executionRepository, never()).update(any());
        verify(executionRepository, never()).addStatusTransition(any(), any(), any());
    }

    @Test
    void process_invalidJson_doesNotThrow() {
        // Given
        String invalidJson = "not valid json";
        when(jsonSerializer.fromJson(invalidJson, StatusMessageDto.class))
                .thenThrow(new RuntimeException("Parse error"));

        // When - should not throw
        service.process(invalidJson);

        // Then
        verify(executionRepository, never()).findById(any());
        verify(executionRepository, never()).update(any());
    }

    @Test
    void process_invalidTransition_stillUpdates() {
        // Given
        String taskId = "task-123";
        String messageJson = "{\"taskId\":\"task-123\",\"status\":\"QUEUED\"}";
        // Trying to transition from IN_PROGRESS back to QUEUED (invalid)
        StatusMessageDto statusMessage = new StatusMessageDto(taskId, ExecutionStatus.QUEUED, null, 0);
        Execution execution = createExecution(taskId, ExecutionStatus.IN_PROGRESS);

        when(jsonSerializer.fromJson(messageJson, StatusMessageDto.class)).thenReturn(statusMessage);
        when(executionRepository.findById(taskId)).thenReturn(Optional.of(execution));

        // When
        service.process(messageJson);

        // Then - should still update (trusts executor)
        verify(executionRepository).update(execution);
        verify(executionRepository).addStatusTransition(eq(taskId), eq(ExecutionStatus.QUEUED), any(Instant.class));
    }

    @Test
    void process_finishedStatus_updatesWithOutputAndExitCode() {
        // Given
        String taskId = "task-123";
        String output = "Script completed successfully";
        int exitCode = 0;
        String messageJson = "{\"taskId\":\"task-123\",\"status\":\"FINISHED\"}";
        StatusMessageDto statusMessage = new StatusMessageDto(taskId, ExecutionStatus.FINISHED, output, exitCode);
        Execution execution = createExecution(taskId, ExecutionStatus.IN_PROGRESS);

        when(jsonSerializer.fromJson(messageJson, StatusMessageDto.class)).thenReturn(statusMessage);
        when(executionRepository.findById(taskId)).thenReturn(Optional.of(execution));

        // When
        service.process(messageJson);

        // Then
        verify(executionRepository).update(execution);
        verify(executionRepository).addStatusTransition(eq(taskId), eq(ExecutionStatus.FINISHED), any(Instant.class));
    }

    private Execution createExecution(String id, ExecutionStatus status) {
        return Execution.builder()
                .id(id)
                .scriptName("test.sh")
                .scriptContent("echo test")
                .requiredCpus(1)
                .currentStatus(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
