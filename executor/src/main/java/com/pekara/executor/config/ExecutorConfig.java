package com.pekara.executor.config;

import org.aeonbits.owner.Config;

@Config.Sources({"system:properties", "system:env"})
public interface ExecutorConfig extends Config {

    @Key("COMMAND_QUEUE_URL")
    String commandQueueUrl();

    @Key("STATUS_QUEUE_URL")
    String statusQueueUrl();

    @Key("POLL_INTERVAL_MS")
    @DefaultValue("1000")
    int pollIntervalMs();

    @Key("VISIBILITY_TIMEOUT_S")
    @DefaultValue("300")
    int visibilityTimeoutSeconds();

    @Key("HEARTBEAT_INTERVAL_S")
    @DefaultValue("240")
    int heartbeatIntervalSeconds();

    @Key("AWS_REGION")
    @DefaultValue("us-east-1")
    String awsRegion();

    @Key("AWS_ENDPOINT")
    String awsEndpoint();

    @Key("AWS_ACCESS_KEY_ID")
    String awsAccessKeyId();

    @Key("AWS_SECRET_ACCESS_KEY")
    String awsSecretAccessKey();

    @Key("DOCKER_IMAGE")
    @DefaultValue("alpine:latest")
    String dockerImage();

    @Key("SQS_WAIT_TIME_S")
    @DefaultValue("20")
    int sqsWaitTimeSeconds();
}
