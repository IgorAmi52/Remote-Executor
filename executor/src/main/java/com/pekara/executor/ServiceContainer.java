package com.pekara.executor;

import com.pekara.common.json.JacksonJsonMapper;
import com.pekara.common.json.JsonSerializer;
import com.pekara.common.messaging.MessageQueueClient;
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
    private final MessageQueueClient queueClient;
    private final JsonSerializer jsonSerializer;
    private final MessageConsumer messageConsumer;
    private final MessagePublisher messagePublisher;
    private final ContainerRunner containerRunner;
    private final ResourceManager resourceManager;
    private final TaskExecutionService executionService;
    @Getter
    private final TaskPollerWorker pollerWorker;

    public ServiceContainer(ExecutorConfig config) {
        this.config = config;

        this.queueClient = new SqsClient(
                config.awsRegion(),
                config.awsEndpoint(),
                config.awsAccessKeyId(),
                config.awsSecretAccessKey()
        );

        this.jsonSerializer = new JacksonJsonMapper();

        this.messageConsumer = new SqsMessageConsumer(
                queueClient,
                jsonSerializer,
                config.commandQueueUrl(),
                config.sqsWaitTimeSeconds(),
                config.visibilityTimeoutSeconds()
        );

        this.messagePublisher = new SqsMessagePublisher(
                queueClient,
                jsonSerializer,
                config.statusQueueUrl()
        );

        this.containerRunner = new DockerContainerRunner(config.dockerImage());
        this.resourceManager = new CpuResourceManager(Main.getCpuCount());

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
        queueClient.close();
    }

}
