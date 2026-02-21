package com.pekara.executor.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CommandMessageDto {

    private String taskId;
    private String script;
    private int requiredCpus;

    public CommandMessageDto() {
    }

    public CommandMessageDto(String taskId, String script, int requiredCpus) {
        this.taskId = taskId;
        this.script = script;
        this.requiredCpus = requiredCpus;
    }

}
