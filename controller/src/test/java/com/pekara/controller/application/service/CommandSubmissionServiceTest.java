package com.pekara.controller.application.service;

import com.pekara.common.json.JsonSerializer;
import com.pekara.controller.application.api.out.MessagePublisher;
import com.pekara.controller.domain.model.Execution;
import com.pekara.controller.domain.model.ExecutionStatus;
import com.pekara.controller.domain.repository.ExecutionRepository;
import com.pekara.controller.dto.CommandMessageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandSubmissionServiceTest {

    @Mock
    private ExecutionRepository executionRepository;

    @Mock
    private MessagePublisher messagePublisher;

    @Mock
    private JsonSerializer jsonSerializer;

    private CommandSubmissionService service;

    @BeforeEach
    void setUp() {
        service = new CommandSubmissionService(executionRepository, messagePublisher, jsonSerializer);
    }

    @Test
    void submit_savesExecutionWithQueuedStatus() {
        // Given
        String scriptName = "test-script.sh";
        String scriptContent = "echo hello";
        int requiredCpus = 2;
        when(jsonSerializer.toJson(any(CommandMessageDto.class))).thenReturn("{}");

        // When
        service.submit(scriptName, scriptContent, requiredCpus);

        // Then
        ArgumentCaptor<Execution> captor = ArgumentCaptor.forClass(Execution.class);
        verify(executionRepository).save(captor.capture());

        Execution saved = captor.getValue();
        assertThat(saved.getScriptName()).isEqualTo(scriptName);
        assertThat(saved.getScriptContent()).isEqualTo(scriptContent);
        assertThat(saved.getRequiredCpus()).isEqualTo(requiredCpus);
        assertThat(saved.getCurrentStatus()).isEqualTo(ExecutionStatus.QUEUED);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void submit_addsStatusTransition() {
        // Given
        when(jsonSerializer.toJson(any(CommandMessageDto.class))).thenReturn("{}");

        // When
        String executionId = service.submit("script.sh", "echo test", 1);

        // Then
        verify(executionRepository).addStatusTransition(eq(executionId), eq(ExecutionStatus.QUEUED), any());
    }

    @Test
    void submit_publishesCorrectMessage() {
        // Given
        String scriptContent = "echo hello";
        int requiredCpus = 4;
        String expectedJson = "{\"taskId\":\"123\",\"script\":\"echo hello\",\"requiredCpus\":4}";
        when(jsonSerializer.toJson(any(CommandMessageDto.class))).thenReturn(expectedJson);

        // When
        service.submit("script.sh", scriptContent, requiredCpus);

        // Then
        ArgumentCaptor<CommandMessageDto> dtoCaptor = ArgumentCaptor.forClass(CommandMessageDto.class);
        verify(jsonSerializer).toJson(dtoCaptor.capture());

        CommandMessageDto dto = dtoCaptor.getValue();
        assertThat(dto.getScript()).isEqualTo(scriptContent);
        assertThat(dto.getRequiredCpus()).isEqualTo(requiredCpus);
        assertThat(dto.getTaskId()).isNotNull();

        verify(messagePublisher).publish(expectedJson);
    }

    @Test
    void submit_returnsGeneratedExecutionId() {
        // Given
        when(jsonSerializer.toJson(any(CommandMessageDto.class))).thenReturn("{}");

        // When
        String executionId = service.submit("script.sh", "echo test", 1);

        // Then
        assertThat(executionId).isNotNull().isNotEmpty();
        // Verify it's a valid UUID format
        assertThat(executionId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }
}
