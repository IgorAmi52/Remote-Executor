package com.pekara.common.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.model.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public class SqsClient implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(SqsClient.class);

    private final software.amazon.awssdk.services.sqs.SqsClient client;

    public SqsClient(String region, String endpoint) {
        var builder = software.amazon.awssdk.services.sqs.SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        this.client = builder.build();
        logger.info("SQS client initialized for region: {}, endpoint: {}", region, endpoint);
    }

    public SqsClient(String region) {
        this(region, null);
    }

    public void sendMessage(String queueUrl, String messageBody) {
        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody)
                .build();

        client.sendMessage(request);
        logger.debug("Message sent to queue: {}", queueUrl);
    }

    public List<Message> receiveMessages(String queueUrl, int maxMessages, int waitTimeSeconds, int visibilityTimeout) {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(maxMessages)
                .waitTimeSeconds(waitTimeSeconds)
                .visibilityTimeout(visibilityTimeout)
                .build();

        ReceiveMessageResponse response = client.receiveMessage(request);
        List<Message> messages = response.messages();

        logger.debug("Received {} messages from queue: {}", messages.size(), queueUrl);
        return messages;
    }

    public Optional<Message> receiveMessage(String queueUrl, int waitTimeSeconds, int visibilityTimeout) {
        List<Message> messages = receiveMessages(queueUrl, 1, waitTimeSeconds, visibilityTimeout);
        return messages.isEmpty() ? Optional.empty() : Optional.of(messages.get(0));
    }

    public void deleteMessage(String queueUrl, String receiptHandle) {
        DeleteMessageRequest request = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .build();

        client.deleteMessage(request);
        logger.debug("Message deleted from queue: {}", queueUrl);
    }

    public void changeMessageVisibility(String queueUrl, String receiptHandle, int visibilityTimeout) {
        ChangeMessageVisibilityRequest request = ChangeMessageVisibilityRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .visibilityTimeout(visibilityTimeout)
                .build();

        client.changeMessageVisibility(request);
        logger.debug("Message visibility changed to {} seconds", visibilityTimeout);
    }

    @Override
    public void close() {
        client.close();
        logger.info("SQS client closed");
    }
}
