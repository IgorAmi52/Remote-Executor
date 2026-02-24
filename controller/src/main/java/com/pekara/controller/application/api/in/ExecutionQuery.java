package com.pekara.controller.application.api.in;

import com.pekara.controller.domain.model.Execution;
import com.pekara.controller.domain.model.StatusTransition;

import java.util.List;
import java.util.Optional;

public interface ExecutionQuery {

    List<Execution> findAll();

    Optional<Execution> findById(String id);

    Optional<Execution> findByPartialId(String partialId);

    List<StatusTransition> getStatusHistory(String executionId);
}
