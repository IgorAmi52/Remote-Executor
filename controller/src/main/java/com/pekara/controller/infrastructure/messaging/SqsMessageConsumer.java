package com.pekara.controller.infrastructure.messaging;

import com.pekara.common.messaging.MessageQueueClient;
import com.pekara.common.messaging.QueueMessage;
import com.pekara.controller.application.api.out.MessageConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class SqsMessageConsumer implements MessageConsumer {

    private final MessageQueueClient queueClient;
    private final String queueUrl;

    @Override
    public List<ReceivedMessage> poll(int maxMessages, int waitTimeSeconds) {
        List<QueueMessage> messages = queueClient.receiveMessages(queueUrl, maxMessages, waitTimeSeconds, 30);
        return messages.stream()
                .map(m -> new ReceivedMessage(m.body(), m.receiptHandle()))
                .toList();
    }

    @Override
    public void deleteMessage(String receiptHandle) {
        queueClient.deleteMessage(queueUrl, receiptHandle);
        log.debug("Deleted message from queue: {}", queueUrl);
    }
}
