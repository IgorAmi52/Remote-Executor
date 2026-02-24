package com.pekara.executor.infrastructure.messaging;

import com.pekara.common.json.JsonSerializer;
import com.pekara.common.messaging.MessageQueueClient;
import com.pekara.common.messaging.QueueMessage;
import com.pekara.executor.application.api.out.MessageConsumer;
import com.pekara.executor.domain.model.Task;
import com.pekara.executor.dto.CommandMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class SqsMessageConsumer implements MessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(SqsMessageConsumer.class);

    private final MessageQueueClient queueClient;
    private final JsonSerializer jsonSerializer;
    private final String queueUrl;
    private final int waitTimeSeconds;
    private final int visibilityTimeout;

    public SqsMessageConsumer(MessageQueueClient queueClient, JsonSerializer jsonSerializer, String queueUrl, int waitTimeSeconds, int visibilityTimeout) {
        this.queueClient = queueClient;
        this.jsonSerializer = jsonSerializer;
        this.queueUrl = queueUrl;
        this.waitTimeSeconds = waitTimeSeconds;
        this.visibilityTimeout = visibilityTimeout;
    }

    @Override
    public Optional<Task> pollTask() {
        Optional<QueueMessage> messageOpt = queueClient.receiveMessage(queueUrl, waitTimeSeconds, visibilityTimeout);

        if (messageOpt.isEmpty()) {
            return Optional.empty();
        }

        QueueMessage message = messageOpt.get();
        try {
            CommandMessageDto dto = jsonSerializer.fromJson(message.body(), CommandMessageDto.class);
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
            queueClient.deleteMessage(queueUrl, message.receiptHandle());
            return Optional.empty();
        }
    }

    @Override
    public void deleteTask(Task task) {
        queueClient.deleteMessage(queueUrl, task.getReceiptHandle());
        logger.debug("Deleted task from queue: {}", task.getTaskId());
    }

    @Override
    public void releaseTask(Task task) {
        queueClient.changeMessageVisibility(queueUrl, task.getReceiptHandle(), 0);
        logger.debug("Released task back to queue: {}", task.getTaskId());
    }

    @Override
    public void extendVisibility(Task task, int seconds) {
        queueClient.changeMessageVisibility(queueUrl, task.getReceiptHandle(), seconds);
        logger.debug("Extended visibility for task {} by {} seconds", task.getTaskId(), seconds);
    }
}
