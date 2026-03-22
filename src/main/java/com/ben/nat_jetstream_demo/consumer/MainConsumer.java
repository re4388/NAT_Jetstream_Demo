package com.ben.nat_jetstream_demo.consumer;

import com.ben.nat_jetstream_demo.config.JetStreamConsumerRegistry;
import com.ben.nat_jetstream_demo.config.ReliableMessageHandler;
import com.ben.nat_jetstream_demo.model.MessageMetadata;
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
            consumerRegistry.subscribe(subject, consumerDurable, NotificationMessage.class, new ReliableMessageHandler<NotificationMessage>() {
                @Override
                public void handle(NotificationMessage notification) throws Exception {
                    // This will be called if handle(p, m) is not overridden, 
                    // but we override handle(p, m) below to get metadata.
                }

                @Override
                public void handle(NotificationMessage notification, MessageMetadata metadata) throws Exception {
                    log.info("[MainConsumer] Processing message - id: {}, source: {}, title: {}, retry: {}",
                            metadata.getId(), metadata.getSource(), notification.getTitle(), notification.getRetryCount());

                    if (chaosService.shouldFail(notification.getContent())) {
                        throw new RuntimeException("Simulated failure - chaos mode active");
                    }

                    processedCount++;
                    log.info("[MainConsumer] Successfully processed message - id: {}, count: {}",
                            metadata.getId(), processedCount);
                }
            });
        };
    }

    public int getProcessedCount() {
        return processedCount;
    }
}
