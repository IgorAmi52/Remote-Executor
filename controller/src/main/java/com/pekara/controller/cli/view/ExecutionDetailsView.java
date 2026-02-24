package com.pekara.controller.cli.view;

import com.pekara.controller.application.api.in.ExecutionQuery;
import com.pekara.controller.domain.model.Execution;
import com.pekara.controller.domain.model.StatusTransition;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import static com.pekara.controller.cli.ConsoleUtils.clearScreen;

@RequiredArgsConstructor
public class ExecutionDetailsView {

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final Scanner scanner;
    private final ExecutionQuery executionQuery;

    public void show() {
        System.out.print("\nEnter execution ID (or partial ID): ");
        String executionId = scanner.nextLine().trim();

        if (executionId.isEmpty()) {
            System.out.println("No ID provided.");
            return;
        }

        boolean viewing = true;
        while (viewing) {
            Optional<Execution> executionOpt = executionQuery.findByPartialId(executionId);

            if (executionOpt.isEmpty()) {
                System.out.println("Execution not found: " + executionId);
                return;
            }

            displayDetails(executionOpt.get());

            System.out.println("\nOptions: [R]efresh | [B]ack to menu");
            System.out.print("Choice: ");
            String choice = scanner.nextLine().trim().toLowerCase();

            switch (choice) {
                case "r", "refresh" -> {
                    // Loop continues, will refresh
                }
                case "b", "back" -> viewing = false;
                default -> System.out.println("Invalid choice. Press R to refresh or B to go back.");
            }
        }
    }

    private void displayDetails(Execution execution) {
        clearScreen();
        String taskIdShort = execution.getId().substring(0, Math.min(8, execution.getId().length()));

        System.out.println("=".repeat(80));
        System.out.println("   Execution Details: " + taskIdShort);
        System.out.println("=".repeat(80));

        // Basic information
        System.out.println("\n--- Information ---");
        System.out.println("Full ID:       " + execution.getId());
        System.out.println("Script Name:   " + execution.getScriptName());
        System.out.println("CPUs Required: " + execution.getRequiredCpus());
        System.out.println("Current Status: " + execution.getCurrentStatus());
        System.out.println("Exit Code:     " + (execution.getExitCode() != null ? execution.getExitCode() : "-"));
        System.out.println("Created:       " + DATETIME_FORMATTER.format(execution.getCreatedAt()));
        System.out.println("Updated:       " + DATETIME_FORMATTER.format(execution.getUpdatedAt()));

        // Status history
        List<StatusTransition> history = executionQuery.getStatusHistory(execution.getId());
        System.out.println("\n--- Status History ---");
        if (history.isEmpty()) {
            System.out.println("No status history available.");
        } else {
            for (int i = 0; i < history.size(); i++) {
                StatusTransition transition = history.get(i);
                String timestamp = DATETIME_FORMATTER.format(transition.getTimestamp());

                StringBuilder line = new StringBuilder();
                line.append(String.format("%-15s → %s", transition.getStatus(), timestamp));

                // Calculate duration from previous status
                if (i > 0) {
                    StatusTransition previous = history.get(i - 1);
                    Duration duration = Duration.between(previous.getTimestamp(), transition.getTimestamp());
                    long seconds = duration.getSeconds();
                    line.append(String.format("  (took %ds)", seconds));
                }

                System.out.println(line.toString());
            }
        }

        // Output
        System.out.println("\n--- Output ---");
        String output = execution.getOutput();
        if (output == null || output.trim().isEmpty()) {
            System.out.println("(no output yet)");
        } else {
            System.out.println(output);
        }

        System.out.println("\n" + "-".repeat(80));
    }
}
