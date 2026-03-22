package com.ben.nat_jetstream_demo.consumer;

import com.ben.nat_jetstream_demo.config.JetStreamConsumerRegistry;
import com.ben.nat_jetstream_demo.model.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DlqConsumer {
    private static final Logger log = LoggerFactory.getLogger(DlqConsumer.class);

    private final JetStreamConsumerRegistry consumerRegistry;

    @Value("${nats.dlq-subject:notifications.dlq}")
    private String dlqSubject;

    private final List<DlqEntry> dlqMessages = new CopyOnWriteArrayList<>();
    private final AtomicInteger dlqCount = new AtomicInteger(0);

    public DlqConsumer(JetStreamConsumerRegistry consumerRegistry) {
        this.consumerRegistry = consumerRegistry;
    }

    @Bean
    public ApplicationRunner startDlqConsumer() {
        return args -> {
            // DLQ monitor is special: it just views the failures
            consumerRegistry.subscribe(dlqSubject, "dlq-monitor-durable", NotificationMessage.class, notification -> {
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
                log.warn("[DlqConsumer] ID: {}, Title: {}, Error: {}, Retry: {}, Total: {}",
                        entry.messageId, entry.title, entry.lastError, entry.retryCount, count);
                log.warn("[DlqConsumer] =====================");
            });
        };
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
        public String messageId;
        public String correlationId;
        public String title;
        public String content;
        public String lastError;
        public Integer retryCount;
        public Instant arrivedAt;

        public DlqEntry(String messageId, String correlationId, String title, String content, String lastError, Integer retryCount, Instant arrivedAt) {
            this.messageId = messageId;
            this.correlationId = correlationId;
            this.title = title;
            this.content = content;
            this.lastError = lastError;
            this.retryCount = retryCount;
            this.arrivedAt = arrivedAt;
        }
    }
}
