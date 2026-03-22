package com.ben.nat_jetstream_demo.controller;

import com.ben.nat_jetstream_demo.model.NotificationMessage;
import com.ben.nat_jetstream_demo.producer.NotificationProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationProducer producer;

    public NotificationController(NotificationProducer producer) {
        this.producer = producer;
    }

    @PostMapping("/send")
    public Map<String, String> sendNotification(@RequestBody NotificationMessage message) {
        try {
            log.info("[API] Received notification request: {}", message.getTitle());
            String messageId = producer.publish(message);
            return Map.of(
                    "status", "success",
                    "messageId", messageId
            );
        } catch (Exception e) {
            log.error("[API] Failed to send notification: {}", e.getMessage());
            return Map.of(
                    "status", "error",
                    "message", e.getMessage()
            );
        }
    }

    @PostMapping("/quick-send")
    public Map<String, String> quickSend(@RequestParam String title,
                                         @RequestParam String name,
                                         @RequestParam String content) {
        try {
            log.info("[API] Quick send request: {}", title);
            String messageId = producer.send(title, name, content);
            return Map.of(
                    "status", "success",
                    "messageId", messageId
            );
        } catch (Exception e) {
            log.error("[API] Failed to send notification: {}", e.getMessage());
            return Map.of(
                    "status", "error",
                    "message", e.getMessage()
            );
        }
    }

    @PostMapping("/test-bulk")
    public Map<String, Object> sendBulk(@RequestParam(defaultValue = "10") int count) {
        log.info("[API] Sending bulk notifications: {}", count);
        int success = 0;
        for (int i = 0; i < count; i++) {
            try {
                producer.send("Bulk Test " + i, "System", "Bulk message content " + i);
                success++;
            } catch (Exception e) {
                log.error("[API] Bulk send error at index {}: {}", i, e.getMessage());
            }
        }
        return Map.of(
                "requested", count,
                "successful", success
        );
    }
}
