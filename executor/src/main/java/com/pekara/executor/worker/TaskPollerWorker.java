package com.pekara.executor.worker;

import com.pekara.executor.application.service.TaskExecutionService;
import com.pekara.executor.config.ExecutorConfig;
import com.pekara.executor.domain.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TaskPollerWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(TaskPollerWorker.class);

    private final TaskExecutionService executionService;
    private final ExecutorConfig config;
    private final ExecutorService threadPool;
    private final AtomicBoolean running;

    public TaskPollerWorker(TaskExecutionService executionService, ExecutorConfig config) {
        this.executionService = executionService;
        this.config = config;
        this.threadPool = Executors.newCachedThreadPool();
        this.running = new AtomicBoolean(false);
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            Thread pollerThread = new Thread(this, "task-poller");
            pollerThread.start();
            logger.info("Task poller started. {}", executionService.getResourceStatus());
        }
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            logger.info("Stopping task poller...");

            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                    if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                        logger.error("Thread pool did not terminate");
                    }
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }

            logger.info("Task poller stopped");
        }
    }

    @Override
    public void run() {
        logger.info("Polling loop started");

        while (running.get()) {
            try {
                Optional<Task> taskOpt = executionService.pollTask();

                if (taskOpt.isEmpty()) {
                    Thread.sleep(config.pollIntervalMs());
                    continue;
                }

                Task task = taskOpt.get();
                logger.info("Received task: {} (requires {} CPUs)", task.getTaskId(), task.getRequiredCpus());

                if (!executionService.tryAllocateResources(task)) {
                    executionService.releaseTask(task);
                    logger.info("Task {} released - not enough CPU. {}", task.getTaskId(), executionService.getResourceStatus());
                    Thread.sleep(config.pollIntervalMs());
                    continue;
                }

                TaskWorker taskWorker = new TaskWorker(
                        task,
                        executionService,
                        config.visibilityTimeoutSeconds(),
                        config.heartbeatIntervalSeconds()
                );
                threadPool.submit(taskWorker);
                logger.info("Task {} submitted for execution. {}", task.getTaskId(), executionService.getResourceStatus());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("Polling loop interrupted");
                break;
            } catch (Exception e) {
                logger.error("Error in polling loop: {}", e.getMessage(), e);
                try {
                    Thread.sleep(config.pollIntervalMs());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        logger.info("Polling loop ended");
    }
}
