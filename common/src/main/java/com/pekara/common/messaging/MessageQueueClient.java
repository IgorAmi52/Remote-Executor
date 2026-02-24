package com.pekara.common.messaging;

import java.util.List;
import java.util.Optional;

public interface MessageQueueClient extends AutoCloseable {

    void sendMessage(String queueUrl, String messageBody);

    List<QueueMessage> receiveMessages(String queueUrl, int maxMessages, int waitTimeSeconds, int visibilityTimeout);

    Optional<QueueMessage> receiveMessage(String queueUrl, int waitTimeSeconds, int visibilityTimeout);

    void deleteMessage(String queueUrl, String receiptHandle);

    void changeMessageVisibility(String queueUrl, String receiptHandle, int visibilityTimeout);

    @Override
    void close();
}
