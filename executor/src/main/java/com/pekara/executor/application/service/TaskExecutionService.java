package com.pekara.executor.application.service;

import com.pekara.executor.application.api.out.ContainerRunner;
import com.pekara.executor.application.api.out.MessageConsumer;
import com.pekara.executor.application.api.out.MessagePublisher;
import com.pekara.executor.domain.model.Task;
import com.pekara.executor.domain.model.TaskResult;
import com.pekara.executor.domain.service.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class TaskExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(TaskExecutionService.class);

    private final MessageConsumer messageConsumer;
    private final MessagePublisher messagePublisher;
    private final ContainerRunner containerRunner;
    private final ResourceManager resourceManager;

    public TaskExecutionService(
            MessageConsumer messageConsumer,
            MessagePublisher messagePublisher,
            ContainerRunner containerRunner,
            ResourceManager resourceManager) {
        this.messageConsumer = messageConsumer;
        this.messagePublisher = messagePublisher;
        this.containerRunner = containerRunner;
        this.resourceManager = resourceManager;
    }

    public Optional<Task> pollTask() {
        return messageConsumer.pollTask();
    }

    public boolean tryAllocateResources(Task task) {
        boolean allocated = resourceManager.tryAllocate(task.getRequiredCpus());
        if (!allocated) {
            logger.info("Not enough CPUs for task {}. Required: {}, Available: {}",
                    task.getTaskId(), task.getRequiredCpus(), resourceManager.getAvailableCpus());
        }
        return allocated;
    }

    public void releaseTask(Task task) {
        messageConsumer.releaseTask(task);
        logger.debug("Task {} released back to queue", task.getTaskId());
    }

    public void executeTask(Task task) {
        logger.info("Starting execution of task: {}", task.getTaskId());

        messagePublisher.publishStatus(TaskResult.inProgress(task.getTaskId()));

        TaskResult result = containerRunner.execute(task);
        logger.info("Task {} completed with status: {}", task.getTaskId(), result.getStatus());

        messagePublisher.publishStatus(result);
        messageConsumer.deleteTask(task);
    }

    public void releaseResources(int cpuCount) {
        resourceManager.release(cpuCount);
    }

    public void extendTaskVisibility(Task task, int seconds) {
        messageConsumer.extendVisibility(task, seconds);
    }

    public String getResourceStatus() {
        return String.format("CPUs: %d/%d available",
                resourceManager.getAvailableCpus(), resourceManager.getTotalCpus());
    }

    public int getTotalCpus() {
        return resourceManager.getTotalCpus();
    }

    public void failTaskImmediately(Task task, String reason) {
        logger.warn("Failing task {} immediately: {}", task.getTaskId(), reason);
        TaskResult result = TaskResult.failed(task.getTaskId(), reason, -2);
        messagePublisher.publishStatus(result);
        messageConsumer.deleteTask(task);
    }
}
