package com.pekara.common.messaging;

public record QueueMessage(
        String messageId,
        String body,
        String receiptHandle
) {
}
