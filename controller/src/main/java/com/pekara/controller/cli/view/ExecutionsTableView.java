package com.pekara.controller.cli.view;

import com.pekara.controller.application.api.in.ExecutionQuery;
import com.pekara.controller.domain.model.Execution;
import lombok.RequiredArgsConstructor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

import static com.pekara.controller.cli.ConsoleUtils.clearScreen;

@RequiredArgsConstructor
public class ExecutionsTableView {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final int REFRESH_INTERVAL_MS = 2000;

    private final Scanner scanner;
    private final ExecutionQuery executionQuery;

    public void show() {
        boolean viewing = true;
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (viewing) {
            displayTable();
            System.out.println("\nPress [B] + Enter to go back.");
            System.out.print("Choice: ");

            // Wait for input with timeout using polling
            long startTime = System.currentTimeMillis();
            while (viewing && System.currentTimeMillis() - startTime < REFRESH_INTERVAL_MS) {
                try {
                    if (reader.ready()) {
                        String choice = reader.readLine();
                        if (choice != null && (choice.trim().equalsIgnoreCase("b") || choice.trim().equalsIgnoreCase("back"))) {
                            viewing = false;
                        }
                    }
                    Thread.sleep(100);
                } catch (IOException | InterruptedException e) {
                    Thread.currentThread().interrupt();
                    viewing = false;
                }
            }
        }
    }

    private void displayTable() {
        clearScreen();
        List<Execution> executions = executionQuery.findAll();

        System.out.println("=".repeat(120));
        System.out.println("   All Executions");
        System.out.println("=".repeat(120));

        if (executions.isEmpty()) {
            System.out.println("No executions found.");
            return;
        }

        // Header
        System.out.printf("%-12s %-25s %-15s %-6s %-8s %-12s%n",
                "Task ID", "Script Name", "Status", "CPUs", "Exit", "Submitted");
        System.out.println("-".repeat(120));

        // Rows
        for (Execution exec : executions) {
            String taskIdShort = exec.getId().substring(0, Math.min(8, exec.getId().length()));
            String scriptName = truncate(exec.getScriptName(), 25);
            String status = exec.getCurrentStatus().toString();
            String exitCodeStr = exec.getExitCode() != null ? exec.getExitCode().toString() : "-";
            String submittedTime = TIME_FORMATTER.format(exec.getCreatedAt());

            System.out.printf("%-12s %-25s %-15s %-6d %-8s %-12s%n",
                    taskIdShort, scriptName, status, exec.getRequiredCpus(), exitCodeStr, submittedTime);
        }

        System.out.println("-".repeat(120));
        System.out.println("Total: " + executions.size() + " execution(s)");
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
}
