# Executor Implementation Plan

## Overview

The Executor is a Java application that runs on EC2 instances, polls an SQS queue for tasks, executes them in Docker containers with CPU limits, and reports status back via another SQS queue.

## Architecture

### Clean Architecture Layers

```
┌─────────────────────────────────────────────────────────────────┐
│                         WORKER LAYER                            │
│  (Threading, background jobs - uses Application layer)          │
├─────────────────────────────────────────────────────────────────┤
│                      APPLICATION LAYER                          │
│  (Use cases, orchestration, defines ports/interfaces)           │
├─────────────────────────────────────────────────────────────────┤
│                        DOMAIN LAYER                             │
│  (Core business logic, models - no external dependencies)       │
├─────────────────────────────────────────────────────────────────┤
│                    INFRASTRUCTURE LAYER                         │
│  (Implements ports - SQS, Docker, etc.)                         │
└─────────────────────────────────────────────────────────────────┘
```

### Package Structure

```
executor/src/main/java/com/remoteexecutor/executor/
├── ExecutorApplication.java           # Entry point

├── domain/                            # Core business logic (no external dependencies)
│   ├── model/
│   │   ├── Task.java                  # taskId, script, requiredCpus
│   │   └── TaskResult.java            # taskId, status, output, exitCode
│   └── service/
│       └── ResourceManager.java       # CPU allocation logic (interface)

├── application/                       # Use cases / orchestration
│   ├── port/
│   │   ├── in/
│   │   │   └── TaskProcessor.java     # Interface for processing tasks
│   │   └── out/
│   │       ├── MessageConsumer.java   # Interface for polling messages
│   │       ├── MessagePublisher.java  # Interface for sending status
│   │       └── ContainerRunner.java   # Interface for running containers
│   └── service/
│       └── TaskExecutionService.java  # Orchestrates: poll → allocate → run → notify

├── infrastructure/                    # External implementations
│   ├── messaging/
│   │   ├── SqsMessageConsumer.java    # Implements MessageConsumer
│   │   └── SqsMessagePublisher.java   # Implements MessagePublisher
│   ├── container/
│   │   └── DockerContainerRunner.java # Implements ContainerRunner
│   └── resource/
│       └── CpuResourceManager.java    # Implements ResourceManager

├── worker/                            # Threading / background jobs
│   ├── TaskPollerWorker.java          # Main polling loop (Runnable)
│   ├── TaskWorker.java                # Per-task execution (Runnable)
│   └── VisibilityExtender.java        # Heartbeat thread to extend visibility timeout

├── dto/                               # Data transfer objects for messaging
│   ├── CommandMessageDto.java         # Incoming from command-queue
│   └── StatusMessageDto.java          # Outgoing to status-queue

└── config/
    └── ExecutorConfig.java            # Configuration (cpu count, queue urls, etc.)
```

### Common Module Structure

```
common/src/main/java/com/remoteexecutor/common/
├── messaging/
│   └── SqsClient.java                 # Generic SQS operations wrapper
├── json/
│   └── JsonMapper.java                # JSON serialization utilities
└── util/
    └── RetryUtils.java                # Retry operations with exponential backoff
```

## Component Details

### Domain Layer

#### Task.java
```java
public class Task {
    private String taskId;
    private String script;
    private int requiredCpus;
    private String receiptHandle;  // For SQS message deletion
}
```

#### TaskResult.java
```java
public class TaskResult {
    private String taskId;
    private TaskStatus status;     // IN_PROGRESS, FINISHED, FAILED
    private String output;
    private int exitCode;
}
```

#### ResourceManager.java (Interface)
```java
public interface ResourceManager {
    boolean tryAllocate(int cpuCount);
    void release(int cpuCount);
    int getAvailableCpus();
    int getTotalCpus();
}
```

### Application Layer (Ports)

#### MessageConsumer.java
```java
public interface MessageConsumer {
    Optional<Task> pollTask();
    void deleteTask(Task task);
    void releaseTask(Task task);  // Make visible again immediately
    void extendVisibility(Task task, int seconds);  // Heartbeat - extend visibility timeout
}
```

#### MessagePublisher.java
```java
public interface MessagePublisher {
    void publishStatus(TaskResult result);
}
```

#### ContainerRunner.java
```java
public interface ContainerRunner {
    TaskResult execute(Task task);
}
```

### Infrastructure Layer

#### CpuResourceManager.java
- Auto-detects CPU count via `Runtime.getRuntime().availableProcessors()`
- Allows override via `EXECUTOR_CPU_COUNT` environment variable
- Thread-safe allocation/release using `synchronized` or `AtomicInteger`

#### SqsMessageConsumer.java
- Uses common module's `SqsClient`
- Maps `CommandMessageDto` to domain `Task`
- Handles visibility timeout

#### DockerContainerRunner.java
- Uses Docker Java client library (`com.github.docker-java`)
- Creates container with `--cpus=N` limit
- Mounts script, captures output
- Handles container lifecycle (create → start → wait → remove)

### Worker Layer

#### TaskPollerWorker.java (Main Thread)
```
while (running) {
    1. Poll for task from MessageConsumer
    2. If no task → sleep briefly, continue
    3. Check ResourceManager.tryAllocate(task.requiredCpus)
       - If NO → releaseTask() (make visible immediately), continue
       - If YES → submit TaskWorker to ExecutorService
}
```

#### TaskWorker.java (Per-Task Thread)
```
try {
    1. Start VisibilityExtender (background heartbeat thread)
    2. Publish IN_PROGRESS status
    3. Execute container via ContainerRunner (may take hours)
    4. Stop VisibilityExtender
    5. Publish FINISHED/FAILED status with output
    6. Delete task from queue
} finally {
    7. Stop VisibilityExtender (if not already stopped)
    8. Release CPUs back to ResourceManager
}
```

#### VisibilityExtender.java (Heartbeat Thread)
```
Runs as daemon thread while task is executing:

while (running && !Thread.interrupted()) {
    1. Sleep for (visibilityTimeout - buffer) e.g., 4 minutes if timeout is 5 min
    2. Call messageConsumer.extendVisibility(task, visibilityTimeout)
    3. Log heartbeat sent
}
```

**Why Heartbeat is Needed:**
- If task execution takes longer than SQS visibility timeout, the message becomes visible again
- Another executor could pick it up → duplicate execution
- Heartbeat periodically extends the timeout while task is running
- If executor crashes, heartbeat stops → message becomes visible → another executor retries

## Threading Model

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          Main Thread                                    │
│                       (TaskPollerWorker)                                │
│                              │                                          │
│              polls SQS ──────┼────── checks CPU availability            │
│                              │                                          │
│                              ▼                                          │
│                   ┌─────────────────────┐                               │
│                   │   ExecutorService   │                               │
│                   │   (Thread Pool)     │                               │
│                   └─────────────────────┘                               │
│                         │    │    │                                     │
│                         ▼    ▼    ▼                                     │
│                   ┌───┐ ┌───┐ ┌───┐                                     │
│                   │TW1│ │TW2│ │TW3│  TaskWorker instances               │
│                   └─┬─┘ └─┬─┘ └─┬─┘                                     │
│                     │     │     │                                       │
│                     ▼     ▼     ▼                                       │
│                   ┌───┐ ┌───┐ ┌───┐                                     │
│                   │VE1│ │VE2│ │VE3│  VisibilityExtender (heartbeat)     │
│                   └───┘ └───┘ └───┘                                     │
└─────────────────────────────────────────────────────────────────────────┘

Each TaskWorker spawns a VisibilityExtender that periodically extends
the SQS message visibility timeout while the container is running.
```

## SQS Message Flow

### Command Message (command-queue → executor)
```json
{
    "taskId": "uuid",
    "script": "#!/bin/bash\necho hello",
    "requiredCpus": 2
}
```

### Status Message (executor → status-queue)
```json
{
    "taskId": "uuid",
    "status": "FINISHED",
    "output": "hello\n",
    "exitCode": 0
}
```

## SQS Visibility Timeout Behavior

1. **Message received** → invisible to ALL consumers for visibility timeout (e.g., 30s)
2. **Task completed + deleted** → message permanently removed
3. **Executor crashes before delete** → after timeout, message reappears for retry
4. **Not enough CPU** → call `releaseTask()` to make visible immediately (or let timeout expire)

This provides fault tolerance - no tasks are lost if an executor crashes.

## Configuration

| Property | Source | Default | Description |
|----------|--------|---------|-------------|
| `EXECUTOR_CPU_COUNT` | Env var | Auto-detect | Total CPUs available for tasks |
| `COMMAND_QUEUE_URL` | Env var | Required | SQS queue to poll for tasks |
| `STATUS_QUEUE_URL` | Env var | Required | SQS queue to publish status |
| `POLL_INTERVAL_MS` | Env var | 1000 | Delay between polls when queue empty |
| `VISIBILITY_TIMEOUT_S` | Env var | 300 | SQS visibility timeout in seconds |
| `HEARTBEAT_INTERVAL_S` | Env var | 240 | How often to extend visibility (should be < VISIBILITY_TIMEOUT_S) |
| `AWS_REGION` | Env var | us-east-1 | AWS region |
| `AWS_ENDPOINT` | Env var | (none) | LocalStack endpoint for local dev |

## Implementation Order

### Phase 1: Common Module
1. `SqsClient` - generic SQS wrapper
2. `JsonMapper` - JSON utilities
3. `RetryUtils` - retry with exponential backoff

### Phase 2: Executor Domain & DTOs
1. `Task`, `TaskResult`, `TaskStatus` models
2. `CommandMessageDto`, `StatusMessageDto`
3. `ResourceManager` interface

### Phase 3: Executor Application Layer
1. Port interfaces (`MessageConsumer`, `MessagePublisher`, `ContainerRunner`)
2. `TaskExecutionService`

### Phase 4: Executor Infrastructure
1. `CpuResourceManager`
2. `SqsMessageConsumer`, `SqsMessagePublisher`
3. `DockerContainerRunner`

### Phase 5: Executor Workers & Main
1. `VisibilityExtender`
2. `TaskWorker`
3. `TaskPollerWorker`
4. `ExecutorApplication` (main entry point)
5. `ExecutorConfig`

### Phase 6: Testing
1. Unit tests for domain and application layers
2. Integration tests with LocalStack and Docker

## Dependencies

### Common Module
- `software.amazon.awssdk:sqs` - AWS SQS SDK v2
- `com.fasterxml.jackson.core:jackson-databind` - JSON processing

### Executor Module
- `common` module
- `com.github.docker-java:docker-java` - Docker client
- `org.slf4j:slf4j-api` + `ch.qos.logback:logback-classic` - Logging

## Open Questions / Decisions

| Question | Recommendation |
|----------|----------------|
| Task timeout? | Add configurable max execution time, kill container if exceeded |
| Graceful shutdown? | On SIGTERM, stop polling, wait for running tasks (with timeout) |
| Script delivery? | Pass script content in message (simple), or S3 URL (for large scripts) |
| Output size limit? | Truncate output in status message if > N bytes |
