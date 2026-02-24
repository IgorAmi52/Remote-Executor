package com.pekara.controller.application.service;

import com.pekara.common.json.JsonSerializer;
import com.pekara.controller.application.api.in.ProcessStatusUpdate;
import com.pekara.controller.domain.model.Execution;
import com.pekara.controller.domain.repository.ExecutionRepository;
import com.pekara.controller.dto.StatusMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class StatusUpdateService implements ProcessStatusUpdate {

    private final ExecutionRepository executionRepository;
    private final JsonSerializer jsonSerializer;

    @Override
    public void process(String messageJson) {
        try {
            StatusMessageDto statusMessage = jsonSerializer.fromJson(messageJson, StatusMessageDto.class);

            String taskId = statusMessage.getTaskId();
            Optional<Execution> executionOpt = executionRepository.findById(taskId);

            if (executionOpt.isEmpty()) {
                log.warn("Received status update for unknown execution: {}", taskId);
                return;
            }

            Execution execution = executionOpt.get();

            // Validate and update status
            try {
                execution.updateStatus(
                        statusMessage.getStatus(),
                        statusMessage.getOutput(),
                        statusMessage.getExitCode()
                );

                // Save updated execution
                executionRepository.update(execution);

                // Record status transition
                executionRepository.addStatusTransition(
                        taskId,
                        statusMessage.getStatus(),
                        Instant.now()
                );

                log.debug("Updated execution {} to status: {}", taskId, statusMessage.getStatus());

            } catch (IllegalStateException e) {
                // Invalid status transition - log warning but don't fail
                log.warn("Invalid status transition for execution {}: {} -> {}",
                        taskId, execution.getCurrentStatus(), statusMessage.getStatus());

                // Update anyway (trust the executor)
                execution.setCurrentStatus(statusMessage.getStatus());
                execution.setOutput(statusMessage.getOutput());
                execution.setExitCode(statusMessage.getExitCode());
                execution.setUpdatedAt(Instant.now());

                executionRepository.update(execution);
                executionRepository.addStatusTransition(
                        taskId,
                        statusMessage.getStatus(),
                        Instant.now()
                );
            }

        } catch (Exception e) {
            log.error("Error processing status update: {}", messageJson, e);
        }
    }
}
