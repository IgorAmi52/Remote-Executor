package com.pekara.controller.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class StatusTransition {

    private final Long id;
    private final String executionId;
    private final ExecutionStatus status;
    private final Instant timestamp;

    public StatusTransition(String executionId, ExecutionStatus status, Instant timestamp) {
        this(null, executionId, status, timestamp);
    }
}
