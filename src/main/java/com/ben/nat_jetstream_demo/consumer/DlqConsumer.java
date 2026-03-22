package com.ben.nat_jetstream_demo.consumer;

import com.ben.nat_jetstream_demo.config.NatsConfig;
import com.ben.nat_jetstream_demo.model.NotificationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DlqConsumer {
    private static final Logger log = LoggerFactory.getLogger(DlqConsumer.class);

    private final NatsConfig natsConfig;
    private final ObjectMapper objectMapper;

    @Value("${nats.dlq-subject:notifications.dlq}")
    private String dlqSubject;

    private final List<DlqEntry> dlqMessages = new CopyOnWriteArrayList<>();
    private final AtomicInteger dlqCount = new AtomicInteger(0);

    public DlqConsumer(NatsConfig natsConfig, ObjectMapper objectMapper) {
        this.natsConfig = natsConfig;
        this.objectMapper = objectMapper;
    }

    @Bean
    public ApplicationRunner startDlqConsumer() {
        return args -> {
            Connection connection = natsConfig.natsConnection();
            JetStream js = natsConfig.jetStream();
            
            if (connection == null || js == null) {
                log.warn("[DlqConsumer] NATS not connected. DLQ monitoring will not be active.");
                return;
            }

            try {
                Dispatcher dispatcher = connection.createDispatcher();

                PushSubscribeOptions options = PushSubscribeOptions.builder()
                        .durable("dlq-monitor-consumer")
                        .deliverGroup("dlq-monitor-group")
                        .build();

                MessageHandler handler = msg -> {
                    try {
                        processDlqMessage(msg);
                    } catch (Exception e) {
                        log.error("[DlqConsumer] Error processing DLQ message: {}", e.getMessage());
                    }
                };

                js.subscribe(dlqSubject, dispatcher, handler, true, options);

                log.info("[DlqConsumer] DLQ monitoring started on subject: {}", dlqSubject);
            } catch (Exception e) {
                log.error("[DlqConsumer] Failed to start DLQ monitoring: {}", e.getMessage());
            }
        };
    }

    private void processDlqMessage(Message msg) {
        String payload = new String(msg.getData(), StandardCharsets.UTF_8);

        try {
            NotificationMessage notification = objectMapper.readValue(payload, NotificationMessage.class);

            DlqEntry entry = new DlqEntry(
                notification.getMessageId(),
                notification.getCorrelationId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getLastError(),
                notification.getRetryCount(),
                Instant.now()
            );

            dlqMessages.add(entry);
            int count = dlqCount.incrementAndGet();

            log.warn("[DlqConsumer] === DLQ ALERT ===");
            log.warn("[DlqConsumer] Message ID: {}", notification.getMessageId());
            log.warn("[DlqConsumer] Title: {}", notification.getTitle());
            log.warn("[DlqConsumer] Error: {}", notification.getLastError());
            log.warn("[DlqConsumer] Retry Count: {}", notification.getRetryCount());
            log.warn("[DlqConsumer] Total DLQ Messages: {}", count);
            log.warn("[DlqConsumer] =====================");

            msg.ack();

        } catch (Exception e) {
            log.error("[DlqConsumer] Failed to parse DLQ message: {}", e.getMessage());
            msg.ack();
        }
    }

    public List<DlqEntry> getDlqMessages() {
        return List.copyOf(dlqMessages);
    }

    public int getDlqCount() {
        return dlqCount.get();
    }

    public void clearDlqHistory() {
        dlqMessages.clear();
        log.info("[DlqConsumer] DLQ history cleared");
    }

    public static class DlqEntry {
        private String messageId;
        private String correlationId;
        private String title;
        private String content;
        private String lastError;
        private Integer retryCount;
        private Instant arrivedAt;

        public DlqEntry() {}

        public DlqEntry(String messageId, String correlationId, String title, String content, String lastError, Integer retryCount, Instant arrivedAt) {
            this.messageId = messageId;
            this.correlationId = correlationId;
            this.title = title;
            this.content = content;
            this.lastError = lastError;
            this.retryCount = retryCount;
            this.arrivedAt = arrivedAt;
        }

        public String getMessageId() { return messageId; }
        public String getCorrelationId() { return correlationId; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public String getLastError() { return lastError; }
        public Integer getRetryCount() { return retryCount; }
        public Instant getArrivedAt() { return arrivedAt; }
    }
}
