package com.pekara.controller.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class Execution {

    private String id;
    private String scriptName;
    private String scriptContent;
    private int requiredCpus;
    private ExecutionStatus currentStatus;
    private Integer exitCode;
    private String output;
    private Instant createdAt;
    private Instant updatedAt;

    public boolean canTransitionTo(ExecutionStatus newStatus) {
        return switch (currentStatus) {
            case QUEUED -> newStatus == ExecutionStatus.IN_PROGRESS;
            case IN_PROGRESS -> newStatus == ExecutionStatus.FINISHED || newStatus == ExecutionStatus.FAILED;
            case FINISHED, FAILED -> false; // Terminal states
        };
    }

    public void updateStatus(ExecutionStatus newStatus, String output, Integer exitCode) {
        if (!canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("Invalid status transition from %s to %s", currentStatus, newStatus)
            );
        }

        this.currentStatus = newStatus;
        this.output = output;
        this.exitCode = exitCode;
        this.updatedAt = Instant.now();
    }
}
