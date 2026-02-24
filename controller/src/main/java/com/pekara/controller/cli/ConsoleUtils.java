package com.pekara.controller.cli;

/**
 * Utility class for console operations in CLI views.
 */
public final class ConsoleUtils {

    private ConsoleUtils() {
        // Utility class
    }

    /**
     * Clears the console screen using ANSI escape codes.
     * Works on Unix/macOS and modern Windows terminals (Windows 10+).
     */
    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
