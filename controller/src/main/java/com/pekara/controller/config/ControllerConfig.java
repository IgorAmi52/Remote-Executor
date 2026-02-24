package com.pekara.controller.config;

import org.aeonbits.owner.Config;

@Config.Sources({"system:properties", "system:env"})
public interface ControllerConfig extends Config {

    @Key("AWS_REGION")
    @DefaultValue("us-east-1")
    String awsRegion();

    @Key("AWS_ACCESS_KEY_ID")
    String awsAccessKeyId();

    @Key("AWS_SECRET_ACCESS_KEY")
    String awsSecretAccessKey();

    @Key("AWS_ENDPOINT")
    String awsEndpoint();

    @Key("COMMAND_QUEUE_URL")
    String commandQueueUrl();

    @Key("STATUS_QUEUE_URL")
    String statusQueueUrl();

    @Key("DB_PATH")
    @DefaultValue("./controller.db")
    String dbPath();
}
