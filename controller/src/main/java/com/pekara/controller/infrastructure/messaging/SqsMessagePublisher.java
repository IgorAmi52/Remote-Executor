package com.pekara.controller.infrastructure.messaging;

import com.pekara.common.messaging.MessageQueueClient;
import com.pekara.controller.application.api.out.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SqsMessagePublisher implements MessagePublisher {

    private final MessageQueueClient queueClient;
    private final String queueUrl;

    @Override
    public void publish(String message) {
        queueClient.sendMessage(queueUrl, message);
        log.debug("Published message to queue: {}", queueUrl);
    }
}
