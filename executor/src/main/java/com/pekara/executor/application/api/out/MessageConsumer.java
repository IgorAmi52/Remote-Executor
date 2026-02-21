package com.pekara.executor.application.api.out;

import com.pekara.executor.domain.model.Task;

import java.util.Optional;

public interface MessageConsumer {

    Optional<Task> pollTask();

    void deleteTask(Task task);

    void releaseTask(Task task);

    void extendVisibility(Task task, int seconds);
}
