package com.pekara.executor.worker;

import com.pekara.executor.application.service.TaskExecutionService;
import com.pekara.executor.domain.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class VisibilityExtender implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(VisibilityExtender.class);

    private final Task task;
    private final TaskExecutionService executionService;
    private final int visibilityTimeout;
    private final int heartbeatInterval;
    private final AtomicBoolean running;

    private Thread thread;

    public VisibilityExtender(Task task, TaskExecutionService executionService, int visibilityTimeout, int heartbeatInterval) {
        this.task = task;
        this.executionService = executionService;
        this.visibilityTimeout = visibilityTimeout;
        this.heartbeatInterval = heartbeatInterval;
        this.running = new AtomicBoolean(false);
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            thread = new Thread(this, "visibility-extender-" + task.getTaskId());
            thread.setDaemon(true);
            thread.start();
            logger.debug("Visibility extender started for task: {}", task.getTaskId());
        }
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (thread != null) {
                thread.interrupt();
            }
            logger.debug("Visibility extender stopped for task: {}", task.getTaskId());
        }
    }

    @Override
    public void run() {
        // Extend visibility immediately when starting
        try {
            executionService.extendTaskVisibility(task, visibilityTimeout);
            logger.debug("Initial visibility extension for task {} by {} seconds",
                    task.getTaskId(), visibilityTimeout);
        } catch (Exception e) {
            logger.warn("Failed initial visibility extension for task {}: {}",
                    task.getTaskId(), e.getMessage());
        }

        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(heartbeatInterval * 1000L);

                if (running.get()) {
                    executionService.extendTaskVisibility(task, visibilityTimeout);
                    logger.debug("Heartbeat: extended visibility for task {} by {} seconds",
                            task.getTaskId(), visibilityTimeout);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.debug("Visibility extender interrupted for task: {}", task.getTaskId());
                break;
            } catch (Exception e) {
                logger.warn("Failed to extend visibility for task {}: {}", task.getTaskId(), e.getMessage());
            }
        }
    }
}
