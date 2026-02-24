package com.pekara.controller.cli;

import com.pekara.controller.application.api.in.ExecutionQuery;
import com.pekara.controller.application.api.in.SubmitCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Scanner;

@Slf4j
@RequiredArgsConstructor
public class CliApplication {

    private final SubmitCommand submitCommand;
    private final ExecutionQuery executionQuery;

    public void start() {
        Scanner scanner = new Scanner(System.in);
        MainMenuView mainMenu = new MainMenuView(
                scanner,
                submitCommand,
                executionQuery
        );

        mainMenu.show();
        scanner.close();
    }
}
