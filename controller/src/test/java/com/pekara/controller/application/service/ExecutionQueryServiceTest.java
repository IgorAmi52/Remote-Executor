package com.pekara.controller.application.service;

import com.pekara.controller.domain.model.Execution;
import com.pekara.controller.domain.model.ExecutionStatus;
import com.pekara.controller.domain.model.StatusTransition;
import com.pekara.controller.domain.repository.ExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutionQueryServiceTest {

    @Mock
    private ExecutionRepository executionRepository;

    private ExecutionQueryService service;

    @BeforeEach
    void setUp() {
        service = new ExecutionQueryService(executionRepository);
    }

    @Test
    void findAll_delegatesToRepository() {
        // Given
        List<Execution> executions = List.of(
                createExecution("id-1", "script1.sh"),
                createExecution("id-2", "script2.sh")
        );
        when(executionRepository.findAll()).thenReturn(executions);

        // When
        List<Execution> result = service.findAll();

        // Then
        assertThat(result).isEqualTo(executions);
        verify(executionRepository).findAll();
    }

    @Test
    void findById_delegatesToRepository() {
        // Given
        String id = "execution-123";
        Execution execution = createExecution(id, "test.sh");
        when(executionRepository.findById(id)).thenReturn(Optional.of(execution));

        // When
        Optional<Execution> result = service.findById(id);

        // Then
        assertThat(result).contains(execution);
        verify(executionRepository).findById(id);
    }

    @Test
    void findByPartialId_returnsExactMatchFirst() {
        // Given
        String exactId = "abc123";
        Execution execution = createExecution(exactId, "test.sh");
        when(executionRepository.findById(exactId)).thenReturn(Optional.of(execution));

        // When
        Optional<Execution> result = service.findByPartialId(exactId);

        // Then
        assertThat(result).contains(execution);
        verify(executionRepository).findById(exactId);
    }

    @Test
    void findByPartialId_returnsPartialMatch() {
        // Given
        String partialId = "abc";
        String fullId = "abc123-full-id";
        Execution execution = createExecution(fullId, "test.sh");
        when(executionRepository.findById(partialId)).thenReturn(Optional.empty());
        when(executionRepository.findAll()).thenReturn(List.of(execution));

        // When
        Optional<Execution> result = service.findByPartialId(partialId);

        // Then
        assertThat(result).contains(execution);
    }

    @Test
    void findByPartialId_returnsEmptyWhenNoMatch() {
        // Given
        String partialId = "xyz";
        Execution execution = createExecution("abc123", "test.sh");
        when(executionRepository.findById(partialId)).thenReturn(Optional.empty());
        when(executionRepository.findAll()).thenReturn(List.of(execution));

        // When
        Optional<Execution> result = service.findByPartialId(partialId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getStatusHistory_delegatesToRepository() {
        // Given
        String executionId = "exec-123";
        List<StatusTransition> transitions = List.of(
                new StatusTransition(executionId, ExecutionStatus.QUEUED, Instant.now()),
                new StatusTransition(executionId, ExecutionStatus.IN_PROGRESS, Instant.now())
        );
        when(executionRepository.getStatusHistory(executionId)).thenReturn(transitions);

        // When
        List<StatusTransition> result = service.getStatusHistory(executionId);

        // Then
        assertThat(result).isEqualTo(transitions);
        verify(executionRepository).getStatusHistory(executionId);
    }

    private Execution createExecution(String id, String scriptName) {
        return Execution.builder()
                .id(id)
                .scriptName(scriptName)
                .scriptContent("echo test")
                .requiredCpus(1)
                .currentStatus(ExecutionStatus.QUEUED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
