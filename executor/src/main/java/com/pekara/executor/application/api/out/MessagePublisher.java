package com.pekara.executor.application.api.out;

import com.pekara.executor.domain.model.TaskResult;

public interface MessagePublisher {

    void publishStatus(TaskResult result);
}
