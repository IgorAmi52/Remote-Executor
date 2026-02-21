package com.pekara.executor;

import com.pekara.common.messaging.SqsClient;
import com.pekara.executor.application.api.out.ContainerRunner;
import com.pekara.executor.application.api.out.MessageConsumer;
import com.pekara.executor.application.api.out.MessagePublisher;
import com.pekara.executor.application.service.TaskExecutionService;
import com.pekara.executor.config.ExecutorConfig;
import com.pekara.executor.domain.service.ResourceManager;
import com.pekara.executor.infrastructure.container.DockerContainerRunner;
import com.pekara.executor.infrastructure.messaging.SqsMessageConsumer;
import com.pekara.executor.infrastructure.messaging.SqsMessagePublisher;
import com.pekara.executor.infrastructure.resource.CpuResourceManager;
import com.pekara.executor.worker.TaskPollerWorker;
import lombok.Getter;

import java.io.Closeable;

public class ServiceContainer implements Closeable {

    private final ExecutorConfig config;
    private final SqsClient sqsClient;
    private final MessageConsumer messageConsumer;
    private final MessagePublisher messagePublisher;
    private final ContainerRunner containerRunner;
    private final ResourceManager resourceManager;
    private final TaskExecutionService executionService;
    @Getter
    private final TaskPollerWorker pollerWorker;

    public ServiceContainer(ExecutorConfig config) {
        this.config = config;

        this.sqsClient = new SqsClient(config.getAwsRegion(), config.getAwsEndpoint());

        this.messageConsumer = new SqsMessageConsumer(
                sqsClient,
                config.getCommandQueueUrl(),
                config.getSqsWaitTimeSeconds(),
                config.getVisibilityTimeoutSeconds()
        );

        this.messagePublisher = new SqsMessagePublisher(
                sqsClient,
                config.getStatusQueueUrl()
        );

        this.containerRunner = new DockerContainerRunner(config.getDockerImage());
        this.resourceManager = new CpuResourceManager(config.getCpuCount());

        this.executionService = new TaskExecutionService(
                messageConsumer,
                messagePublisher,
                containerRunner,
                resourceManager
        );

        this.pollerWorker = new TaskPollerWorker(executionService, config);
    }

    @Override
    public void close() {
        pollerWorker.stop();
        if (containerRunner instanceof AutoCloseable) {
            try {
                ((AutoCloseable) containerRunner).close();
            } catch (Exception e) {
                // Log or handle exception
            }
        }
        sqsClient.close();
    }
}
