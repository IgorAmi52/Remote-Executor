package com.pekara.executor.infrastructure.messaging;

import com.pekara.common.json.JsonSerializer;
import com.pekara.common.messaging.MessageQueueClient;
import com.pekara.executor.application.api.out.MessagePublisher;
import com.pekara.executor.domain.model.TaskResult;
import com.pekara.executor.dto.StatusMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqsMessagePublisher implements MessagePublisher {

    private static final Logger logger = LoggerFactory.getLogger(SqsMessagePublisher.class);

    private final MessageQueueClient queueClient;
    private final JsonSerializer jsonSerializer;
    private final String queueUrl;

    public SqsMessagePublisher(MessageQueueClient queueClient, JsonSerializer jsonSerializer, String queueUrl) {
        this.queueClient = queueClient;
        this.jsonSerializer = jsonSerializer;
        this.queueUrl = queueUrl;
    }

    @Override
    public void publishStatus(TaskResult result) {
        StatusMessageDto dto = new StatusMessageDto(
                result.getTaskId(),
                result.getStatus(),
                result.getOutput(),
                result.getExitCode()
        );

        String json = jsonSerializer.toJson(dto);
        queueClient.sendMessage(queueUrl, json);
        logger.info("Published status for task {}: {}", result.getTaskId(), result.getStatus());
    }
}
