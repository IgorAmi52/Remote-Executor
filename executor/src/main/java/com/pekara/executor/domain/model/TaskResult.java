package com.pekara.executor.domain.model;

public class TaskResult {

    private final String taskId;
    private final TaskStatus status;
    private final String output;
    private final int exitCode;

    public TaskResult(String taskId, TaskStatus status, String output, int exitCode) {
        this.taskId = taskId;
        this.status = status;
        this.output = output;
        this.exitCode = exitCode;
    }

    public static TaskResult inProgress(String taskId) {
        return new TaskResult(taskId, TaskStatus.IN_PROGRESS, null, -1);
    }

    public static TaskResult finished(String taskId, String output, int exitCode) {
        return new TaskResult(taskId, TaskStatus.FINISHED, output, exitCode);
    }

    public static TaskResult failed(String taskId, String output, int exitCode) {
        return new TaskResult(taskId, TaskStatus.FAILED, output, exitCode);
    }

    public String getTaskId() {
        return taskId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getOutput() {
        return output;
    }

    public int getExitCode() {
        return exitCode;
    }

    @Override
    public String toString() {
        return "TaskResult{taskId='" + taskId + "', status=" + status + ", exitCode=" + exitCode + "}";
    }
}
