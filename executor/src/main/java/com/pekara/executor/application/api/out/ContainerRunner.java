package com.pekara.executor.application.api.out;

import com.pekara.executor.domain.model.Task;
import com.pekara.executor.domain.model.TaskResult;

public interface ContainerRunner {

    TaskResult execute(Task task);
}
