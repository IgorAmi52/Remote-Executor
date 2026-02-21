package com.pekara.executor.infrastructure.resource;

import com.pekara.executor.domain.service.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class CpuResourceManager implements ResourceManager {

    private static final Logger logger = LoggerFactory.getLogger(CpuResourceManager.class);

    private final int totalCpus;
    private final AtomicInteger usedCpus;

    public CpuResourceManager() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public CpuResourceManager(int totalCpus) {
        if (totalCpus <= 0) {
            throw new IllegalArgumentException("Total CPUs must be positive");
        }
        this.totalCpus = totalCpus;
        this.usedCpus = new AtomicInteger(0);
        logger.info("CpuResourceManager initialized with {} CPUs", totalCpus);
    }

    @Override
    public synchronized boolean tryAllocate(int cpuCount) {
        if (cpuCount <= 0) {
            throw new IllegalArgumentException("CPU count must be positive");
        }

        int currentUsed = usedCpus.get();
        int available = totalCpus - currentUsed;

        if (cpuCount <= available) {
            usedCpus.addAndGet(cpuCount);
            logger.debug("Allocated {} CPUs. Used: {}/{}", cpuCount, usedCpus.get(), totalCpus);
            return true;
        }

        logger.debug("Cannot allocate {} CPUs. Available: {}/{}", cpuCount, available, totalCpus);
        return false;
    }

    @Override
    public void release(int cpuCount) {
        if (cpuCount <= 0) {
            throw new IllegalArgumentException("CPU count must be positive");
        }

        int newUsed = usedCpus.addAndGet(-cpuCount);
        if (newUsed < 0) {
            usedCpus.set(0);
            logger.warn("Released more CPUs than allocated. Corrected to 0.");
        } else {
            logger.debug("Released {} CPUs. Used: {}/{}", cpuCount, newUsed, totalCpus);
        }
    }

    @Override
    public int getAvailableCpus() {
        return totalCpus - usedCpus.get();
    }

    @Override
    public int getTotalCpus() {
        return totalCpus;
    }
}
