package com.ben.nat_jetstream_demo.controller;

import com.ben.nat_jetstream_demo.model.NotificationMessage;
import com.ben.nat_jetstream_demo.producer.NotificationProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Slf4j
public class NotificationController {

    private final NotificationProducer producer;

    public NotificationController(NotificationProducer producer) {
        this.producer = producer;
    }

    @PostMapping("/send")
    public Map<String, Object> send(@RequestBody NotificationMessage message) throws Exception {
        log.info("[API] Received send request: title={}", message.getTitle());
        String messageId = producer.publish(message);
        return Map.of(
                "status", "sent",
                "messageId", messageId
        );
    }

    @PostMapping("/send/simple")
    public Map<String, Object> sendSimple(
            @RequestParam(defaultValue = "Test Title") String title,
            @RequestParam(defaultValue = "Test User") String name,
            @RequestParam String content) throws Exception {
        log.info("[API] Received simple send request: title={}, content={}", title, content);
        String messageId = producer.send(title, name, content);
        return Map.of(
                "status", "sent",
                "messageId", messageId,
                "title", title,
                "content", content
        );
    }

    @PostMapping("/send/batch")
    public Map<String, Object> sendBatch(
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(defaultValue = "Batch Title") String title,
            @RequestParam(defaultValue = "batch-content") String content) throws Exception {
        log.info("[API] Received batch send request: count={}", count);
        List<String> messageIds = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            String id = producer.send(title + " #" + (i + 1), "BatchUser", content);
            messageIds.add(id);
        }
        return Map.of(
                "status", "sent",
                "count", count,
                "messageIds", messageIds
        );
    }
}
