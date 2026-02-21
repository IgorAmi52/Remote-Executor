package com.pekara.executor.dto;

import com.pekara.executor.domain.model.TaskStatus;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class StatusMessageDto {

    private String taskId;
    private TaskStatus status;
    private String output;
    private int exitCode;

    public StatusMessageDto() {
    }

    public StatusMessageDto(String taskId, TaskStatus status, String output, int exitCode) {
        this.taskId = taskId;
        this.status = status;
        this.output = output;
        this.exitCode = exitCode;
    }

}
