package com.ben.nat_jetstream_demo.consumer;

import com.ben.nat_jetstream_demo.config.JetStreamConsumerRegistry;
import com.ben.nat_jetstream_demo.model.NotificationMessage;
import com.ben.nat_jetstream_demo.service.ChaosService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class MainConsumer {
    private static final Logger log = LoggerFactory.getLogger(MainConsumer.class);

    private final JetStreamConsumerRegistry consumerRegistry;
    private final ChaosService chaosService;

    @Value("${nats.subject:notifications.test}")
    private String subject;

    @Value("${nats.consumer.durable:demo-consumer-durable}")
    private String consumerDurable;

    private int processedCount = 0;

    public MainConsumer(JetStreamConsumerRegistry consumerRegistry, ChaosService chaosService) {
        this.consumerRegistry = consumerRegistry;
        this.chaosService = chaosService;
    }

    @Bean
    public ApplicationRunner startConsumer() {
        return args -> {
            consumerRegistry.subscribe(subject, consumerDurable, NotificationMessage.class, notification -> {
                log.info("[MainConsumer] Processing message - id: {}, title: {}, retry: {}",
                        notification.getMessageId(), notification.getTitle(), notification.getRetryCount());

                if (chaosService.shouldFail(notification.getContent())) {
                    throw new RuntimeException("Simulated failure - chaos mode active");
                }

                processedCount++;
                log.info("[MainConsumer] Successfully processed message - id: {}, count: {}",
                        notification.getMessageId(), processedCount);
            });
        };
    }

    public int getProcessedCount() {
        return processedCount;
    }
}
