package com.pekara.controller;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Starting Remote Executor Controller...");

        try {
            // Load .env file if present (ignores if not found)
            Dotenv dotenv = Dotenv.configure()
                    .directory(".")
                    .ignoreIfMissing()
                    .load();

            // Set environment variables from .env file (use DECLARED_IN_ENV_FILE to include all entries)
            dotenv.entries(Dotenv.Filter.DECLARED_IN_ENV_FILE).forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
            );
            logger.info(".env file loaded with {} entries", dotenv.entries().size());

            ServiceContainer container = new ServiceContainer();

            container.getStatusPollerWorker().start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutdown signal received");
                container.getStatusPollerWorker().stop();
                container.shutdown();
            }));

            container.getCliApplication().start();
            logger.info("Remote Executor Controller stopped");
        } catch (Exception e) {
            logger.error("Fatal error in Controller Application: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
