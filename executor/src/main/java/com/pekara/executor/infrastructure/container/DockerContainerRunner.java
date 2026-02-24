package com.pekara.executor.infrastructure.container;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.pekara.executor.application.api.out.ContainerRunner;
import com.pekara.executor.domain.model.Task;
import com.pekara.executor.domain.model.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class DockerContainerRunner implements ContainerRunner, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(DockerContainerRunner.class);
    private static final String DEFAULT_IMAGE = "alpine:latest";
    private static final long CONTAINER_TIMEOUT_MINUTES = 60;

    private final DockerClient dockerClient;
    private final String image;

    public DockerContainerRunner() {
        this(DEFAULT_IMAGE);
    }

    public DockerContainerRunner(String image) {
        this.image = image;

        var config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

        var httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
        logger.info("DockerContainerRunner initialized with image: {}", image);
    }

    @Override
    public TaskResult execute(Task task) {
        String containerId = null;

        try {
            logger.info("Creating container for task: {}", task.getTaskId());

            long cpuPeriod = 100000L;
            long cpuQuota = task.getRequiredCpus() * cpuPeriod;

            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withCpuPeriod(cpuPeriod)
                    .withCpuQuota(cpuQuota)
                    .withAutoRemove(false);

            CreateContainerResponse container = dockerClient.createContainerCmd(image)
                    .withHostConfig(hostConfig)
                    .withCmd("bash", "-c", task.getScript())
                    .withTty(false)
                    .exec();

            containerId = container.getId();
            logger.info("Container created: {} for task: {}", containerId, task.getTaskId());

            dockerClient.startContainerCmd(containerId).exec();
            logger.info("Container started: {} for task: {}", containerId, task.getTaskId());

            int exitCode = dockerClient.waitContainerCmd(containerId)
                    .exec(new WaitContainerResultCallback())
                    .awaitStatusCode(CONTAINER_TIMEOUT_MINUTES, TimeUnit.MINUTES);

            String output = collectLogs(containerId);
            logger.info("Container {} finished with exit code: {}", containerId, exitCode);

            if (exitCode == 0) {
                return TaskResult.finished(task.getTaskId(), output, exitCode);
            } else {
                return TaskResult.failed(task.getTaskId(), output, exitCode);
            }

        } catch (Exception e) {
            logger.error("Error executing task {}: {}", task.getTaskId(), e.getMessage(), e);
            return TaskResult.failed(task.getTaskId(), "Execution error: " + e.getMessage(), -1);
        } finally {
            if (containerId != null) {
                removeContainer(containerId);
            }
        }
    }

    private String collectLogs(String containerId) throws InterruptedException {
        StringBuilder outputBuilder = new StringBuilder();
        dockerClient.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .exec(new ResultCallback.Adapter<Frame>() {
                    @Override
                    public void onNext(Frame frame) {
                        outputBuilder.append(new String(frame.getPayload()));
                    }
                }).awaitCompletion(30, TimeUnit.SECONDS);
        return outputBuilder.toString();
    }

    private void removeContainer(String containerId) {
        try {
            dockerClient.removeContainerCmd(containerId)
                    .withForce(true)
                    .exec();
            logger.debug("Container removed: {}", containerId);
        } catch (Exception e) {
            logger.warn("Failed to remove container {}: {}", containerId, e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            dockerClient.close();
            logger.info("Docker client closed");
        } catch (Exception e) {
            logger.warn("Error closing Docker client: {}", e.getMessage());
        }
    }
}
