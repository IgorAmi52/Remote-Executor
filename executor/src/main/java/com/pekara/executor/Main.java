package com.pekara.executor;

import com.pekara.executor.config.ExecutorConfig;
import io.github.cdimascio.dotenv.Dotenv;
import org.aeonbits.owner.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Starting Executor Application...");

        try {
            // Load .env file if present (ignores if not found)
            // Try current directory first, then parent directory (for monorepo structure)
            Dotenv dotenv = Dotenv.configure()
                    .directory(".")
                    .ignoreIfMissing()
                    .load();

            // Set environment variables from .env file (use DECLARED_IN_ENV_FILE to include all entries)
            dotenv.entries(Dotenv.Filter.DECLARED_IN_ENV_FILE).forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
            );
            logger.info(".env file loaded with {} entries", dotenv.entries().size());

            ExecutorConfig config = ConfigFactory.create(ExecutorConfig.class);
            logger.info("Configuration loaded");

            try(ServiceContainer container = new ServiceContainer(config)){
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    logger.info("Shutdown signal received");
                    container.close();
                    logger.info("Executor Application shutdown complete");
                }));

                container.getPollerWorker().start();
                container.getPollerWorker().awaitTermination();
            }
        } catch (Exception e) {
            logger.error("Fatal error in Executor Application: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    public static int getCpuCount() {
        return Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
    }
}
