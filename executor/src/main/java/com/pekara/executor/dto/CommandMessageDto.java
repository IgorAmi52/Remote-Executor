package com.pekara.executor.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CommandMessageDto {

    private String taskId;
    private String script;
    private int requiredCpus;
}
