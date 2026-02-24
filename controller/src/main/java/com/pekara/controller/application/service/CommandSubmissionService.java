package com.pekara.controller.application.service;

import com.pekara.common.json.JsonSerializer;
import com.pekara.controller.application.api.in.SubmitCommand;
import com.pekara.controller.application.api.out.MessagePublisher;
import com.pekara.controller.domain.model.Execution;
import com.pekara.controller.domain.model.ExecutionStatus;
import com.pekara.controller.domain.repository.ExecutionRepository;
import com.pekara.controller.dto.CommandMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class CommandSubmissionService implements SubmitCommand {

    private final ExecutionRepository executionRepository;
    private final MessagePublisher messagePublisher;
    private final JsonSerializer jsonSerializer;

    @Override
    public String submit(String scriptName, String scriptContent, int requiredCpus) {
        // Generate unique ID
        String executionId = UUID.randomUUID().toString();

        // Create execution entity
        Instant now = Instant.now();
        Execution execution = Execution.builder()
                .id(executionId)
                .scriptName(scriptName)
                .scriptContent(scriptContent)
                .requiredCpus(requiredCpus)
                .currentStatus(ExecutionStatus.QUEUED)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Save to repository
        executionRepository.save(execution);

        // Record initial status transition
        executionRepository.addStatusTransition(executionId, ExecutionStatus.QUEUED, now);

        // Create and publish command message
        CommandMessageDto commandMessage = new CommandMessageDto(
                executionId,
                scriptContent,
                requiredCpus
        );

        String messageJson = jsonSerializer.toJson(commandMessage);
        messagePublisher.publish(messageJson);

        log.debug("Submitted command: {} (ID: {})", scriptName, executionId);

        return executionId;
    }
}
