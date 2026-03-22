package com.ben.nat_jetstream_demo.controller;

import com.ben.nat_jetstream_demo.consumer.DlqConsumer;
import com.ben.nat_jetstream_demo.service.ChaosService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chaos")
public class ChaosController {
    private static final Logger log = LoggerFactory.getLogger(ChaosController.class);

    private final ChaosService chaosService;
    private final DlqConsumer dlqConsumer;

    public ChaosController(ChaosService chaosService, DlqConsumer dlqConsumer) {
        this.chaosService = chaosService;
        this.dlqConsumer = dlqConsumer;
    }

    @PostMapping("/enable")
    public Map<String, Object> enableFailure(@RequestParam(defaultValue = "FAIL") String keyword) {
        log.info("[API] Enabling failure mode with keyword: {}", keyword);
        chaosService.enableFailure();
        chaosService.addFailureKeyword(keyword, "Chaos mode triggered");
        return Map.of(
                "status", "enabled",
                "keyword", keyword,
                "message", "Failure mode enabled. Messages containing '" + keyword + "' will fail processing."
        );
    }

    @PostMapping("/disable")
    public Map<String, Object> disableFailure() {
        log.info("[API] Disabling failure mode");
        chaosService.disableFailure();
        return Map.of(
                "status", "disabled",
                "message", "Failure mode disabled. All messages will process normally."
        );
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        ChaosService.ChaosStatus status = chaosService.getStatus();
        return Map.of(
                "failureEnabled", status.isFailureEnabled(),
                "failureCount", status.getFailureCount(),
                "maxFailures", status.getMaxFailures(),
                "failureKeywords", status.getFailureKeywords(),
                "dlqCount", dlqConsumer.getDlqCount(),
                "dlqMessages", dlqConsumer.getDlqMessages()
        );
    }

    @PostMapping("/keyword/add")
    public Map<String, Object> addKeyword(@RequestParam String keyword, 
                                          @RequestParam(defaultValue = "User defined") String reason) {
        log.info("[API] Adding failure keyword: {} -> {}", keyword, reason);
        chaosService.addFailureKeyword(keyword, reason);
        return Map.of(
                "status", "added",
                "keyword", keyword,
                "reason", reason
        );
    }

    @PostMapping("/keyword/remove")
    public Map<String, Object> removeKeyword(@RequestParam String keyword) {
        log.info("[API] Removing failure keyword: {}", keyword);
        chaosService.removeFailureKeyword(keyword);
        return Map.of(
                "status", "removed",
                "keyword", keyword
        );
    }

    @PostMapping("/keyword/clear")
    public Map<String, Object> clearKeywords() {
        log.info("[API] Clearing all failure keywords");
        chaosService.clearFailureKeywords();
        return Map.of(
                "status", "cleared",
                "message", "All failure keywords cleared."
        );
    }

    @PostMapping("/max-failures")
    public Map<String, Object> setMaxFailures(@RequestParam int max) {
        log.info("[API] Setting max failures to: {}", max);
        chaosService.setMaxFailures(max);
        return Map.of(
                "status", "set",
                "maxFailures", max
        );
    }

    @GetMapping("/dlq")
    public Map<String, Object> getDlqMessages() {
        return Map.of(
                "dlqCount", dlqConsumer.getDlqCount(),
                "messages", dlqConsumer.getDlqMessages()
        );
    }

    @PostMapping("/dlq/clear")
    public Map<String, Object> clearDlqHistory() {
        dlqConsumer.clearDlqHistory();
        return Map.of(
                "status", "cleared",
                "message", "DLQ history cleared."
        );
    }
}
