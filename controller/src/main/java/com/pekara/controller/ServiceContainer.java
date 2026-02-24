package com.pekara.controller;

import com.pekara.common.json.JacksonJsonMapper;
import com.pekara.common.json.JsonSerializer;
import com.pekara.common.messaging.MessageQueueClient;
import com.pekara.common.messaging.SqsClient;
import com.pekara.common.persistence.DatabaseClient;
import com.pekara.common.persistence.SqliteClient;
import com.pekara.controller.application.api.in.ExecutionQuery;
import com.pekara.controller.application.api.in.ProcessStatusUpdate;
import com.pekara.controller.application.api.in.SubmitCommand;
import com.pekara.controller.application.api.out.MessageConsumer;
import com.pekara.controller.application.api.out.MessagePublisher;
import com.pekara.controller.application.service.CommandSubmissionService;
import com.pekara.controller.application.service.ExecutionQueryService;
import com.pekara.controller.application.service.StatusUpdateService;
import com.pekara.controller.cli.CliApplication;
import com.pekara.controller.config.ControllerConfig;
import com.pekara.controller.domain.repository.ExecutionRepository;
import com.pekara.controller.infrastructure.messaging.SqsMessageConsumer;
import com.pekara.controller.infrastructure.messaging.SqsMessagePublisher;
import com.pekara.controller.infrastructure.persistence.SqliteExecutionRepository;
import com.pekara.controller.worker.StatusPollerWorker;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.aeonbits.owner.ConfigFactory;

@Slf4j
@Getter
public class ServiceContainer {

    private final ControllerConfig config;
    private final DatabaseClient databaseClient;
    private final MessageQueueClient queueClient;
    private final JsonSerializer jsonSerializer;
    private final ExecutionRepository executionRepository;
    private final MessagePublisher messagePublisher;
    private final MessageConsumer messageConsumer;
    private final SubmitCommand submitCommand;
    private final ExecutionQuery executionQuery;
    private final ProcessStatusUpdate processStatusUpdate;
    private final StatusPollerWorker statusPollerWorker;
    private final CliApplication cliApplication;

    public ServiceContainer() {
        log.info("Initializing ServiceContainer...");

        // Config
        this.config = ConfigFactory.create(ControllerConfig.class);

        // Common utilities
        this.databaseClient = new SqliteClient(config.dbPath());
        this.jsonSerializer = new JacksonJsonMapper();

        // SQS Client
        this.queueClient = new SqsClient(
                config.awsRegion(),
                config.awsEndpoint(),
                config.awsAccessKeyId(),
                config.awsSecretAccessKey()
        );

        // Repository
        this.executionRepository = new SqliteExecutionRepository(databaseClient, config.dbPath());

        // Messaging adapters
        this.messagePublisher = new SqsMessagePublisher(queueClient, config.commandQueueUrl());
        this.messageConsumer = new SqsMessageConsumer(queueClient, config.statusQueueUrl());

        // Application services (exposed as interfaces)
        this.submitCommand = new CommandSubmissionService(
                executionRepository,
                messagePublisher,
                jsonSerializer
        );

        this.executionQuery = new ExecutionQueryService(executionRepository);

        this.processStatusUpdate = new StatusUpdateService(
                executionRepository,
                jsonSerializer
        );

        // Worker
        this.statusPollerWorker = new StatusPollerWorker(
                messageConsumer,
                processStatusUpdate
        );

        // CLI
        this.cliApplication = new CliApplication(
                submitCommand,
                executionQuery
        );

        log.info("ServiceContainer initialized successfully");
    }

    public void shutdown() {
        log.info("Shutting down ServiceContainer...");
        databaseClient.close();
        queueClient.close();
        log.info("ServiceContainer shutdown complete");
    }
}
