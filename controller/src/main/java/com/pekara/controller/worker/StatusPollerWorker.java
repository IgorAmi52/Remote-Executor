package com.pekara.controller.worker;

import com.pekara.controller.application.api.in.ProcessStatusUpdate;
import com.pekara.controller.application.api.out.MessageConsumer;
import com.pekara.controller.application.api.out.MessageConsumer.ReceivedMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class StatusPollerWorker implements Runnable {

    private static final int MAX_MESSAGES = 10;
    private static final int WAIT_TIME_SECONDS = 20;
    private static final long ERROR_BACKOFF_MS = 5000;

    private final MessageConsumer messageConsumer;
    private final ProcessStatusUpdate processStatusUpdate;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public StatusPollerWorker(MessageConsumer messageConsumer, ProcessStatusUpdate processStatusUpdate) {
        this.messageConsumer = messageConsumer;
        this.processStatusUpdate = processStatusUpdate;
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            Thread workerThread = new Thread(this, "status-poller");
            workerThread.start();
            log.info("StatusPollerWorker started");
        }
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("StatusPollerWorker stopping...");
        }
    }

    @Override
    public void run() {
        log.info("Status polling loop started");

        while (running.get()) {
            try {
                List<ReceivedMessage> messages = messageConsumer.poll(MAX_MESSAGES, WAIT_TIME_SECONDS);

                for (ReceivedMessage message : messages) {
                    try {
                        processStatusUpdate.process(message.body());
                        messageConsumer.deleteMessage(message.receiptHandle());
                    } catch (Exception e) {
                        log.error("Error processing status message: {}", message.body(), e);
                    }
                }

            } catch (Exception e) {
                log.error("Error polling status queue", e);
                try {
                    Thread.sleep(ERROR_BACKOFF_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.info("Status polling loop ended");
    }
}
