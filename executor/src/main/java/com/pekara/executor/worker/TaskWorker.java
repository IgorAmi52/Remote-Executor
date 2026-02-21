package com.pekara.executor.worker;

import com.pekara.executor.application.service.TaskExecutionService;
import com.pekara.executor.domain.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(TaskWorker.class);

    private final Task task;
    private final TaskExecutionService executionService;
    private final int visibilityTimeout;
    private final int heartbeatInterval;

    public TaskWorker(Task task, TaskExecutionService executionService, int visibilityTimeout, int heartbeatInterval) {
        this.task = task;
        this.executionService = executionService;
        this.visibilityTimeout = visibilityTimeout;
        this.heartbeatInterval = heartbeatInterval;
    }

    @Override
    public void run() {
        VisibilityExtender visibilityExtender = new VisibilityExtender(
                task, executionService, visibilityTimeout, heartbeatInterval);

        try {
            visibilityExtender.start();
            executionService.executeTask(task);
        } catch (Exception e) {
            logger.error("Unexpected error executing task {}: {}", task.getTaskId(), e.getMessage(), e);
        } finally {
            visibilityExtender.stop();
            executionService.releaseResources(task.getRequiredCpus());
            logger.debug("Released {} CPUs for task: {}", task.getRequiredCpus(), task.getTaskId());
        }
    }
}
