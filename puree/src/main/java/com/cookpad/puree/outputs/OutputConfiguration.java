package com.cookpad.puree.outputs;

import com.cookpad.puree.storage.EnhancedPureeStorage;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class OutputConfiguration {
    private int flushIntervalMillis = 2 * 60 * 1000; // 2 minutes
    private int logsPerRequest = 100;
    private int maxRetryCount = 5;
    private EnhancedPureeStorage.Sort[] sorting = new EnhancedPureeStorage.Sort[] {};
    private long purgeAgeMillis = -1;

    OutputConfiguration() {
    }

    public int getFlushIntervalMillis() {
        return flushIntervalMillis;
    }

    public void setFlushIntervalMillis(int flushIntervalMillis) {
        this.flushIntervalMillis = flushIntervalMillis;
    }

    public int getLogsPerRequest() {
        return logsPerRequest;
    }

    public void setLogsPerRequest(int logsPerRequest) {
        this.logsPerRequest = logsPerRequest;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public EnhancedPureeStorage.Sort[] getSorting() {
        return sorting;
    }

    public void setSorting(EnhancedPureeStorage.Sort... sorting) {
        this.sorting = sorting;
    }

    public long getPurgeAgeMillis() {
        return purgeAgeMillis;
    }

    public void setPurgeAgeMillis(long purgeAgeMillis) {
        this.purgeAgeMillis = purgeAgeMillis;
    }
}
