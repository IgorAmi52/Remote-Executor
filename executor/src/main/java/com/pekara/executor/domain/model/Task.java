package com.pekara.executor.domain.model;

public class Task {

    private final String taskId;
    private final String script;
    private final int requiredCpus;
    private final String receiptHandle;

    public Task(String taskId, String script, int requiredCpus, String receiptHandle) {
        this.taskId = taskId;
        this.script = script;
        this.requiredCpus = requiredCpus;
        this.receiptHandle = receiptHandle;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getScript() {
        return script;
    }

    public int getRequiredCpus() {
        return requiredCpus;
    }

    public String getReceiptHandle() {
        return receiptHandle;
    }

    @Override
    public String toString() {
        return "Task{taskId='" + taskId + "', requiredCpus=" + requiredCpus + "}";
    }
}
