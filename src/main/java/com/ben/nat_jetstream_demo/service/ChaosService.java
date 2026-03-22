package com.ben.nat_jetstream_demo.service;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ChaosService {
    private static final Logger log = LoggerFactory.getLogger(ChaosService.class);
    
    private final AtomicBoolean failureEnabled = new AtomicBoolean(false);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger maxFailures = new AtomicInteger(0);
    private final ConcurrentHashMap<String, String> failureKeywords = new ConcurrentHashMap<>();
    
    public void enableFailure() {
        failureEnabled.set(true);
        failureCount.set(0);
        log.info("[ChaosService] Failure mode ENABLED");
    }
    
    public void disableFailure() {
        failureEnabled.set(false);
        log.info("[ChaosService] Failure mode DISABLED");
    }
    
    public boolean isFailureEnabled() {
        return failureEnabled.get();
    }
    
    public void addFailureKeyword(String keyword, String reason) {
        failureKeywords.put(keyword, reason);
        log.info("[ChaosService] Added failure keyword: '{}' -> {}", keyword, reason);
    }
    
    public void removeFailureKeyword(String keyword) {
        failureKeywords.remove(keyword);
        log.info("[ChaosService] Removed failure keyword: {}", keyword);
    }
    
    public void clearFailureKeywords() {
        failureKeywords.clear();
        log.info("[ChaosService] Cleared all failure keywords");
    }
    
    public List<String> getFailureKeywords() {
        return List.copyOf(failureKeywords.keySet());
    }
    
    public void setMaxFailures(int max) {
        maxFailures.set(max);
        log.info("[ChaosService] Max failures set to: {}", max);
    }
    
    public int getMaxFailures() {
        return maxFailures.get();
    }
    
    public int getFailureCount() {
        return failureCount.get();
    }
    
    public void incrementFailureCount() {
        failureCount.incrementAndGet();
    }
    
    public boolean shouldFail(String content) {
        if (!failureEnabled.get()) {
            return false;
        }
        
        int max = maxFailures.get();
        if (max > 0 && failureCount.get() >= max) {
            log.info("[ChaosService] Max failures reached ({}), skipping failure", max);
            return false;
        }
        
        for (String keyword : failureKeywords.keySet()) {
            if (content != null && content.toUpperCase().contains(keyword.toUpperCase())) {
                incrementFailureCount();
                return true;
            }
        }
        
        return false;
    }
    
    public ChaosStatus getStatus() {
        return new ChaosStatus(
            this.failureEnabled.get(),
            this.failureCount.get(),
            this.maxFailures.get(),
            List.copyOf(failureKeywords.keySet())
        );
    }
    
    public static class ChaosStatus {
        private final boolean failureEnabled;
        private final int failureCount;
        private final int maxFailures;
        private final List<String> failureKeywords;

        public ChaosStatus(boolean failureEnabled, int failureCount, int maxFailures, List<String> failureKeywords) {
            this.failureEnabled = failureEnabled;
            this.failureCount = failureCount;
            this.maxFailures = maxFailures;
            this.failureKeywords = failureKeywords;
        }

        public boolean isFailureEnabled() { return failureEnabled; }
        public int getFailureCount() { return failureCount; }
        public int getMaxFailures() { return maxFailures; }
        public List<String> getFailureKeywords() { return failureKeywords; }
    }
}
