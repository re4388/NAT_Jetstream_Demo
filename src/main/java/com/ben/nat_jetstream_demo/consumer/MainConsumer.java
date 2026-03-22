package com.ben.nat_jetstream_demo.consumer;

import com.ben.nat_jetstream_demo.config.NatsConfig;
import com.ben.nat_jetstream_demo.config.JetStreamClient;
import com.ben.nat_jetstream_demo.model.NotificationMessage;
import com.ben.nat_jetstream_demo.service.ChaosService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class MainConsumer {
    private static final Logger log = LoggerFactory.getLogger(MainConsumer.class);

    private final NatsConfig natsConfig;
    private final JetStreamClient jetStreamClient;
    private final ObjectMapper objectMapper;
    private final ChaosService chaosService;

    @Value("${nats.subject:notifications.test}")
    private String subject;

    @Value("${nats.dlq-subject:notifications.dlq}")
    private String dlqSubject;

    @Value("${nats.consumer.durable:demo-consumer-durable}")
    private String consumerDurable;

    @Value("${nats.consumer.maxDeliver:3}")
    private int maxDeliver;

    private int processedCount = 0;

    @Autowired
    public MainConsumer(NatsConfig natsConfig, JetStreamClient jetStreamClient, ObjectMapper objectMapper, ChaosService chaosService) {
        this.natsConfig = natsConfig;
        this.jetStreamClient = jetStreamClient;
        this.objectMapper = objectMapper;
        this.chaosService = chaosService;
    }

    @Bean
    public ApplicationRunner startConsumer() {
        return args -> {
            Connection connection = natsConfig.natsConnection();
            JetStream js = natsConfig.jetStream();
            
            if (connection == null || js == null) {
                log.warn("[MainConsumer] NATS not connected. Consumer will not be active.");
                return;
            }

            try {
                Dispatcher dispatcher = connection.createDispatcher();
                
                MessageHandler handler = msg -> {
                    try {
                        processMessage(msg);
                    } catch (Exception e) {
                        log.error("[MainConsumer] Error processing message: {}", e.getMessage());
                    }
                };

                PushSubscribeOptions pushOptions = PushSubscribeOptions.builder()
                        .durable(consumerDurable)
                        .build();

                js.subscribe(subject, dispatcher, handler, true, pushOptions);

                log.info("[MainConsumer] Subscribed to {} with durable {} (push mode)", subject, consumerDurable);
            } catch (Exception e) {
                log.error("[MainConsumer] Failed to start subscription: {}", e.getMessage());
            }
        };
    }

    private void processMessage(Message msg) {
        String payload = new String(msg.getData(), StandardCharsets.UTF_8);

        try {
            NotificationMessage notification = objectMapper.readValue(payload, NotificationMessage.class);
            log.info("[MainConsumer] Processing message - id: {}, title: {}, retryCount: {}",
                    notification.getMessageId(), notification.getTitle(), notification.getRetryCount());

            if (chaosService.shouldFail(notification.getContent())) {
                handleFailure(msg, notification, "Simulated failure - chaos mode active");
                return;
            }

            processedCount++;
            log.info("[MainConsumer] Successfully processed message - id: {}, count: {}",
                    notification.getMessageId(), processedCount);
            msg.ack();

        } catch (Exception e) {
            log.error("[MainConsumer] Error processing message: {}", e.getMessage());
            handleUnexpectedError(msg, e);
        }
    }

    private void handleFailure(Message msg, NotificationMessage notification, String errorMsg) {
        log.warn("[MainConsumer] FAILURE triggered - id: {}, reason: {}",
                notification.getMessageId(), errorMsg);

        notification.setLastError(errorMsg);
        int currentRetry = notification.getRetryCount() == null ? 0 : notification.getRetryCount();
        notification.setRetryCount(currentRetry + 1);

        try {
            if (notification.getRetryCount() >= maxDeliver) {
                log.warn("[MainConsumer] Max retries reached ({}). Moving to DLQ: {}", maxDeliver, dlqSubject);
                sendToDlq(notification);
                msg.ack(); // Acknowledge from main stream as it's now in DLQ
            } else {
                msg.nak();
                log.info("[MainConsumer] NAK sent - message will be redelivered, retryCount: {}",
                        notification.getRetryCount());
            }
        } catch (Exception e) {
            log.error("[MainConsumer] Failed to handle failure (DLQ/NAK): {}", e.getMessage());
        }
    }

    private void sendToDlq(NotificationMessage notification) {
        try {
            jetStreamClient.publishTo(dlqSubject)
                    .payload(notification)
                    .go();
            log.info("[MainConsumer] Successfully sent message {} to DLQ", notification.getMessageId());
        } catch (Exception e) {
            log.error("[MainConsumer] Critical error: failed to send to DLQ: {}", e.getMessage());
        }
    }

    private void handleUnexpectedError(Message msg, Exception e) {
        log.error("[MainConsumer] Unexpected error - attempting NAK: {}", e.getMessage());
        try {
            msg.nak();
        } catch (Exception ex) {
            log.error("[MainConsumer] Failed to send NAK: {}", ex.getMessage());
        }
    }

    public int getProcessedCount() {
        return processedCount;
    }
}
