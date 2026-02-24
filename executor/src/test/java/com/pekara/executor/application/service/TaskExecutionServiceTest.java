package com.pekara.executor.application.service;

import com.pekara.executor.application.api.out.ContainerRunner;
import com.pekara.executor.application.api.out.MessageConsumer;
import com.pekara.executor.application.api.out.MessagePublisher;
import com.pekara.executor.domain.model.Task;
import com.pekara.executor.domain.model.TaskResult;
import com.pekara.executor.domain.model.TaskStatus;
import com.pekara.executor.domain.service.ResourceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskExecutionServiceTest {

    @Mock
    private MessageConsumer messageConsumer;

    @Mock
    private MessagePublisher messagePublisher;

    @Mock
    private ContainerRunner containerRunner;

    @Mock
    private ResourceManager resourceManager;

    private TaskExecutionService service;

    @BeforeEach
    void setUp() {
        service = new TaskExecutionService(messageConsumer, messagePublisher, containerRunner, resourceManager);
    }

    @Test
    void pollTask_delegatesToMessageConsumer() {
        Task task = new Task("task-1", "echo hello", 2, "receipt-1");
        when(messageConsumer.pollTask()).thenReturn(Optional.of(task));

        Optional<Task> result = service.pollTask();

        assertTrue(result.isPresent());
        assertEquals("task-1", result.get().getTaskId());
        verify(messageConsumer).pollTask();
    }

    @Test
    void pollTask_returnsEmpty_whenNoTaskAvailable() {
        when(messageConsumer.pollTask()).thenReturn(Optional.empty());

        Optional<Task> result = service.pollTask();

        assertTrue(result.isEmpty());
        verify(messageConsumer).pollTask();
    }

    @Test
    void tryAllocateResources_returnsTrue_whenResourcesAvailable() {
        Task task = new Task("task-1", "echo hello", 2, "receipt-1");
        when(resourceManager.tryAllocate(2)).thenReturn(true);

        boolean result = service.tryAllocateResources(task);

        assertTrue(result);
        verify(resourceManager).tryAllocate(2);
    }

    @Test
    void tryAllocateResources_returnsFalse_whenResourcesUnavailable() {
        Task task = new Task("task-1", "echo hello", 4, "receipt-1");
        when(resourceManager.tryAllocate(4)).thenReturn(false);
        when(resourceManager.getAvailableCpus()).thenReturn(2);

        boolean result = service.tryAllocateResources(task);

        assertFalse(result);
        verify(resourceManager).tryAllocate(4);
    }

    @Test
    void releaseTask_delegatesToMessageConsumer() {
        Task task = new Task("task-1", "echo hello", 2, "receipt-1");

        service.releaseTask(task);

        verify(messageConsumer).releaseTask(task);
    }

    @Test
    void executeTask_publishesInProgressStatus_thenExecutes_thenPublishesFinalStatus() {
        Task task = new Task("task-1", "echo hello", 2, "receipt-1");
        TaskResult executionResult = TaskResult.finished("task-1", "hello", 0);
        when(containerRunner.execute(task)).thenReturn(executionResult);

        service.executeTask(task);

        InOrder inOrder = inOrder(messagePublisher, containerRunner, messageConsumer);
        // 1. First publish IN_PROGRESS status
        inOrder.verify(messagePublisher).publishStatus(argThat(result ->
                result.getTaskId().equals("task-1") && result.getStatus() == TaskStatus.IN_PROGRESS));
        // 2. Execute the task
        inOrder.verify(containerRunner).execute(task);
        // 3. Publish final status
        inOrder.verify(messagePublisher).publishStatus(executionResult);
        // 4. Delete the task from queue
        inOrder.verify(messageConsumer).deleteTask(task);
    }

    @Test
    void executeTask_publishesFailedStatus_whenExecutionFails() {
        Task task = new Task("task-1", "exit 1", 2, "receipt-1");
        TaskResult failedResult = TaskResult.failed("task-1", "error", 1);
        when(containerRunner.execute(task)).thenReturn(failedResult);

        service.executeTask(task);

        verify(messagePublisher).publishStatus(argThat(result ->
                result.getStatus() == TaskStatus.IN_PROGRESS));
        verify(messagePublisher).publishStatus(failedResult);
        verify(messageConsumer).deleteTask(task);
    }

    @Test
    void releaseResources_delegatesToResourceManager() {
        service.releaseResources(4);

        verify(resourceManager).release(4);
    }

    @Test
    void extendTaskVisibility_delegatesToMessageConsumer() {
        Task task = new Task("task-1", "echo hello", 2, "receipt-1");

        service.extendTaskVisibility(task, 30);

        verify(messageConsumer).extendVisibility(task, 30);
    }

    @Test
    void getResourceStatus_returnsFormattedString() {
        when(resourceManager.getAvailableCpus()).thenReturn(6);
        when(resourceManager.getTotalCpus()).thenReturn(8);

        String status = service.getResourceStatus();

        assertEquals("CPUs: 6/8 available", status);
    }

    @Test
    void getTotalCpus_delegatesToResourceManager() {
        when(resourceManager.getTotalCpus()).thenReturn(8);

        int result = service.getTotalCpus();

        assertEquals(8, result);
        verify(resourceManager).getTotalCpus();
    }

    @Test
    void failTaskImmediately_publishesFailedStatus_andDeletesTask() {
        Task task = new Task("task-1", "echo hello", 2, "receipt-1");

        service.failTaskImmediately(task, "Not enough CPUs");

        verify(messagePublisher).publishStatus(argThat(result ->
                result.getTaskId().equals("task-1") &&
                result.getStatus() == TaskStatus.FAILED &&
                result.getOutput().equals("Not enough CPUs") &&
                result.getExitCode() == -2));
        verify(messageConsumer).deleteTask(task);
    }
}
