package com.pekara.controller.application.service;

import com.pekara.controller.application.api.in.ExecutionQuery;
import com.pekara.controller.domain.model.Execution;
import com.pekara.controller.domain.model.StatusTransition;
import com.pekara.controller.domain.repository.ExecutionRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ExecutionQueryService implements ExecutionQuery {

    private final ExecutionRepository executionRepository;

    @Override
    public List<Execution> findAll() {
        return executionRepository.findAll();
    }

    @Override
    public Optional<Execution> findById(String id) {
        return executionRepository.findById(id);
    }

    @Override
    public Optional<Execution> findByPartialId(String partialId) {
        Optional<Execution> exact = executionRepository.findById(partialId);
        if (exact.isPresent()) {
            return exact;
        }

        return executionRepository.findAll().stream()
                .filter(e -> e.getId().startsWith(partialId))
                .findFirst();
    }

    @Override
    public List<StatusTransition> getStatusHistory(String executionId) {
        return executionRepository.getStatusHistory(executionId);
    }
}
