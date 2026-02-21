package com.pekara.executor.domain.service;

public interface ResourceManager {

    boolean tryAllocate(int cpuCount);

    void release(int cpuCount);

    int getAvailableCpus();

    int getTotalCpus();
}
