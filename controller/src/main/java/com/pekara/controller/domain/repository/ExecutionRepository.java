package com.pekara.controller.domain.repository;

import com.pekara.controller.domain.model.Execution;
import com.pekara.controller.domain.model.ExecutionStatus;
import com.pekara.controller.domain.model.StatusTransition;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ExecutionRepository {

    void save(Execution execution);

    void update(Execution execution);

    Optional<Execution> findById(String id);

    List<Execution> findAll();

    void addStatusTransition(String executionId, ExecutionStatus status, Instant timestamp);

    List<StatusTransition> getStatusHistory(String executionId);
}
