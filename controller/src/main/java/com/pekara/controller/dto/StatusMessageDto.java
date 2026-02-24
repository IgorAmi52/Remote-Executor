package com.pekara.controller.dto;

import com.pekara.controller.domain.model.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatusMessageDto {

    private String taskId;
    private ExecutionStatus status;
    private String output;
    private int exitCode;
}
