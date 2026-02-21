package com.pekara.executor.infrastructure.messaging;

import com.pekara.common.json.JsonMapper;
import com.pekara.common.messaging.SqsClient;
import com.pekara.executor.application.api.out.MessagePublisher;
import com.pekara.executor.domain.model.TaskResult;
import com.pekara.executor.dto.StatusMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqsMessagePublisher implements MessagePublisher {

    private static final Logger logger = LoggerFactory.getLogger(SqsMessagePublisher.class);

    private final SqsClient sqsClient;
    private final String queueUrl;

    public SqsMessagePublisher(SqsClient sqsClient, String queueUrl) {
        this.sqsClient = sqsClient;
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

        String json = JsonMapper.toJson(dto);
        sqsClient.sendMessage(queueUrl, json);
        logger.info("Published status for task {}: {}", result.getTaskId(), result.getStatus());
    }
}
