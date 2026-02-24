package com.pekara.controller.application.api.out;

import java.util.List;

public interface MessageConsumer {

    record ReceivedMessage(String body, String receiptHandle) {}

    List<ReceivedMessage> poll(int maxMessages, int waitTimeSeconds);

    void deleteMessage(String receiptHandle);
}
