package com.pekara.executor.config;

import org.aeonbits.owner.Config;

@Config.Sources("system:env")
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

    @Key("DOCKER_IMAGE")
    @DefaultValue("alpine:latest")
    String dockerImage();

    @Key("SQS_WAIT_TIME_S")
    @DefaultValue("20")
    int sqsWaitTimeSeconds();

    default int getCpuCount() {
        // Auto-detect and reserve 2 CPUs for OS/JVM overhead
        return Runtime.getRuntime().availableProcessors() - 2;
    }

    default String getCommandQueueUrl() {
        return commandQueueUrl();
    }

    default String getStatusQueueUrl() {
        return statusQueueUrl();
    }

    default int getPollIntervalMs() {
        return pollIntervalMs();
    }

    default int getVisibilityTimeoutSeconds() {
        return visibilityTimeoutSeconds();
    }

    default int getHeartbeatIntervalSeconds() {
        return heartbeatIntervalSeconds();
    }

    default String getAwsRegion() {
        return awsRegion();
    }

    default String getAwsEndpoint() {
        return awsEndpoint();
    }

    default String getDockerImage() {
        return dockerImage();
    }

    default int getSqsWaitTimeSeconds() {
        return sqsWaitTimeSeconds();
    }
}
