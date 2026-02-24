package com.pekara.controller.cli.view;

import com.pekara.controller.application.api.in.SubmitCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static com.pekara.controller.cli.ConsoleUtils.clearScreen;

@Slf4j
@RequiredArgsConstructor
public class SubmitScriptsView {

    private final Scanner scanner;
    private final SubmitCommand submitCommand;

    public void show() {
        clearScreen();
        System.out.println("=".repeat(40));
        System.out.println("   Submit Scripts");
        System.out.println("=".repeat(40));

        List<String> scriptPaths = collectScriptPaths();
        if (scriptPaths.isEmpty()) {
            System.out.println("No scripts to submit.");
            return;
        }

        Map<String, Integer> scriptsWithCpus = collectCpuRequirements(scriptPaths);
        submitScripts(scriptsWithCpus);
    }

    private List<String> collectScriptPaths() {
        List<String> paths = new ArrayList<>();
        System.out.println("\nEnter script file paths or folders (one per line).");
        System.out.println("Press Enter on an empty line when done:");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                break;
            }

            File file = new File(input);
            if (!file.exists()) {
                System.out.println("  Warning: Path does not exist: " + input);
            } else if (file.isDirectory()) {
                List<String> scriptsInFolder = findScriptsInFolder(file);
                if (scriptsInFolder.isEmpty()) {
                    System.out.println("  No .sh files found in folder: " + file.getName());
                } else {
                    paths.addAll(scriptsInFolder);
                    System.out.println("  Added " + scriptsInFolder.size() + " script(s) from folder " + file.getName() + ":");
                    for (String scriptPath : scriptsInFolder) {
                        System.out.println("    - " + new File(scriptPath).getName());
                    }
                }
            } else if (file.isFile()) {
                paths.add(input);
                System.out.println("  Added: " + file.getName());
            }
        }

        return paths;
    }

    private List<String> findScriptsInFolder(File folder) {
        List<String> scripts = new ArrayList<>();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".sh"));

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    scripts.add(file.getAbsolutePath());
                }
            }
        }

        return scripts;
    }

    private Map<String, Integer> collectCpuRequirements(List<String> scriptPaths) {
        Map<String, Integer> scriptsWithCpus = new LinkedHashMap<>();

        System.out.println("\nConfigure CPU requirements:");
        for (String path : scriptPaths) {
            File file = new File(path);
            System.out.print("CPUs for " + file.getName() + " (default 2): ");
            String input = scanner.nextLine().trim();

            int cpus = 2;
            if (!input.isEmpty()) {
                try {
                    cpus = Integer.parseInt(input);
                    if (cpus < 1) {
                        System.out.println("  Invalid CPU count, using default: 2");
                        cpus = 2;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("  Invalid number, using default: 2");
                }
            }

            scriptsWithCpus.put(path, cpus);
        }

        return scriptsWithCpus;
    }

    private void submitScripts(Map<String, Integer> scriptsWithCpus) {
        System.out.println("\nSubmitting scripts...");
        int successCount = 0;
        List<String> errors = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : scriptsWithCpus.entrySet()) {
            String path = entry.getKey();
            int cpus = entry.getValue();
            File file = new File(path);

            try {
                String scriptContent = Files.readString(file.toPath());
                submitCommand.submit(file.getName(), scriptContent, cpus);
                System.out.println("  ✓ " + file.getName());
                successCount++;

            } catch (IOException e) {
                log.error("Failed to read file: {}", file.getName(), e);
                errors.add(file.getName() + ": Failed to read file");
                System.out.println("  ✗ " + file.getName() + " (read error)");
            } catch (Exception e) {
                log.error("Failed to submit: {}", file.getName(), e);
                errors.add(file.getName() + ": Submission failed");
                System.out.println("  ✗ " + file.getName() + " (submission failed)");
            }
        }

        System.out.println("\nResult: Successfully submitted " + successCount + " script(s).");
        if (!errors.isEmpty()) {
            System.out.println("Errors:");
            for (String error : errors) {
                System.out.println("  - " + error);
            }
        }

        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }
}
