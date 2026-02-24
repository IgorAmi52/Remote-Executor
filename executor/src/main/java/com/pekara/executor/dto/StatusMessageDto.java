package com.pekara.executor.dto;

import com.pekara.executor.domain.model.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class StatusMessageDto {

    private String taskId;
    private TaskStatus status;
    private String output;
    private int exitCode;
}
