package com.pekara.executor.infrastructure.messaging;

import com.pekara.common.json.JsonMapper;
import com.pekara.common.messaging.SqsClient;
import com.pekara.executor.application.api.out.MessageConsumer;
import com.pekara.executor.domain.model.Task;
import com.pekara.executor.dto.CommandMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.Optional;

public class SqsMessageConsumer implements MessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(SqsMessageConsumer.class);

    private final SqsClient sqsClient;
    private final String queueUrl;
    private final int waitTimeSeconds;
    private final int visibilityTimeout;

    public SqsMessageConsumer(SqsClient sqsClient, String queueUrl, int waitTimeSeconds, int visibilityTimeout) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
        this.waitTimeSeconds = waitTimeSeconds;
        this.visibilityTimeout = visibilityTimeout;
    }

    @Override
    public Optional<Task> pollTask() {
        Optional<Message> messageOpt = sqsClient.receiveMessage(queueUrl, waitTimeSeconds, visibilityTimeout);

        if (messageOpt.isEmpty()) {
            return Optional.empty();
        }

        Message message = messageOpt.get();
        try {
            CommandMessageDto dto = JsonMapper.fromJson(message.body(), CommandMessageDto.class);
            Task task = new Task(
                    dto.getTaskId(),
                    dto.getScript(),
                    dto.getRequiredCpus(),
                    message.receiptHandle()
            );
            logger.debug("Polled task: {}", task.getTaskId());
            return Optional.of(task);
        } catch (Exception e) {
            logger.error("Failed to parse message: {}", message.body(), e);
            sqsClient.deleteMessage(queueUrl, message.receiptHandle());
            return Optional.empty();
        }
    }

    @Override
    public void deleteTask(Task task) {
        sqsClient.deleteMessage(queueUrl, task.getReceiptHandle());
        logger.debug("Deleted task from queue: {}", task.getTaskId());
    }

    @Override
    public void releaseTask(Task task) {
        sqsClient.changeMessageVisibility(queueUrl, task.getReceiptHandle(), 0);
        logger.debug("Released task back to queue: {}", task.getTaskId());
    }

    @Override
    public void extendVisibility(Task task, int seconds) {
        sqsClient.changeMessageVisibility(queueUrl, task.getReceiptHandle(), seconds);
        logger.debug("Extended visibility for task {} by {} seconds", task.getTaskId(), seconds);
    }
}
