package com.pekara.controller.cli;

import com.pekara.controller.application.api.in.ExecutionQuery;
import com.pekara.controller.application.api.in.SubmitCommand;
import com.pekara.controller.cli.view.ExecutionDetailsView;
import com.pekara.controller.cli.view.ExecutionsTableView;
import com.pekara.controller.cli.view.SubmitScriptsView;
import lombok.RequiredArgsConstructor;

import java.util.Scanner;

import static com.pekara.controller.cli.ConsoleUtils.clearScreen;

@RequiredArgsConstructor
public class MainMenuView {

    private final Scanner scanner;
    private final SubmitCommand submitCommand;
    private final ExecutionQuery executionQuery;

    public void show() {
        boolean running = true;

        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    SubmitScriptsView submitView = new SubmitScriptsView(scanner, submitCommand);
                    submitView.show();
                }
                case "2" -> {
                    ExecutionsTableView tableView = new ExecutionsTableView(scanner, executionQuery);
                    tableView.show();
                }
                case "3" -> {
                    ExecutionDetailsView detailsView = new ExecutionDetailsView(scanner, executionQuery);
                    detailsView.show();
                }
                case "0" -> running = false;
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }

        System.out.println("\nGoodbye!");
    }

    private void printMenu() {
        clearScreen();
        System.out.println("=".repeat(40));
        System.out.println("   Remote Executor Controller");
        System.out.println("=".repeat(40));
        System.out.println("1. Submit scripts");
        System.out.println("2. View all executions");
        System.out.println("3. View execution details");
        System.out.println("0. Exit");
        System.out.print("\nChoice: ");
    }
}
