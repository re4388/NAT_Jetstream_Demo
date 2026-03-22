package com.ben.nat_jetstream_demo.config;

import io.nats.client.Connection;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.StreamConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.time.Duration;

@Configuration
public class NatsConfig {
    private static final Logger log = LoggerFactory.getLogger(NatsConfig.class);

    @Value("${nats.url}")
    private String natsUrl;

    @Value("${nats.stream}")
    private String streamName;

    @Value("${nats.subject}")
    private String subject;

    @Value("${nats.dlq-stream}")
    private String dlqStreamName;

    @Value("${nats.dlq-subject}")
    private String dlqSubject;

    @Value("${nats.consumer.durable}")
    private String consumerDurable;

    @Value("${nats.consumer.maxDeliver}")
    private int maxDeliver;

    private Connection natsConnection;
    private JetStreamManagement jsm;
    private io.nats.client.JetStream jetStream;

    @PostConstruct
    public void init() {
        connectToNats();
    }

    private void connectToNats() {
        try {
            log.info("[NatsConfig] Connecting to NATS at {}", natsUrl);
            Options options = new Options.Builder()
                    .server(natsUrl)
                    .connectionTimeout(Duration.ofSeconds(5))
                    .reconnectWait(Duration.ofSeconds(1))
                    .maxReconnects(-1)
                    .build();
            
            natsConnection = Nats.connect(options);
            log.info("[NatsConfig] Successfully connected to NATS");
            
            jsm = natsConnection.jetStreamManagement();
            setupStreams();
            
            jetStream = natsConnection.jetStream();
            log.info("[NatsConfig] JetStream initialized successfully");
            
        } catch (Exception e) {
            log.warn("[NatsConfig] Failed to connect to NATS: {}. Application will continue but messaging features will be unavailable.", e.getMessage());
        }
    }

    private void setupStreams() {
        if (jsm == null) return;
        
        createMainStream();
        createDlqStream();
    }

    private void createMainStream() {
        if (jsm == null) return;
        
        try {
            jsm.getStreamInfo(streamName);
            log.info("[NatsConfig] Stream {} already exists.", streamName);
        } catch (Exception e) {
            log.info("[NatsConfig] Creating main stream: {}", streamName);
            StreamConfiguration sc = StreamConfiguration.builder()
                    .name(streamName)
                    .subjects(subject)
                    .retentionPolicy(io.nats.client.api.RetentionPolicy.Limits)
                    .maxConsumers(10)
                    .maxMessages(-1)
                    .maxBytes(-1)
                    .build();
            try {
                jsm.addStream(sc);
                log.info("[NatsConfig] Main stream created successfully.");
            } catch (Exception ex) {
                log.warn("[NatsConfig] Failed to create main stream: {}", ex.getMessage());
            }
        }
    }

    private void createDlqStream() {
        if (jsm == null) return;
        
        try {
            jsm.getStreamInfo(dlqStreamName);
            log.info("[NatsConfig] DLQ Stream {} already exists.", dlqStreamName);
        } catch (Exception e) {
            log.info("[NatsConfig] Creating DLQ stream: {}", dlqStreamName);
            StreamConfiguration dlqConfig = StreamConfiguration.builder()
                    .name(dlqStreamName)
                    .subjects(dlqSubject)
                    .retentionPolicy(io.nats.client.api.RetentionPolicy.Limits)
                    .maxConsumers(-1)
                    .build();
            try {
                jsm.addStream(dlqConfig);
                log.info("[NatsConfig] DLQ stream created successfully.");
            } catch (Exception ex) {
                log.warn("[NatsConfig] Failed to create DLQ stream: {}", ex.getMessage());
            }
        }
    }

    @Bean
    public Connection natsConnection() {
        return natsConnection;
    }

    @Bean
    public JetStreamManagement jetStreamManagement() {
        return jsm;
    }

    @Bean
    public io.nats.client.JetStream jetStream() {
        return jetStream;
    }

    public boolean isConnected() {
        return natsConnection != null && natsConnection.getStatus() == Connection.Status.CONNECTED;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        return mapper;
    }

    @Bean
    public ConsumerConfiguration mainConsumerConfig() {
        return ConsumerConfiguration.builder()
                .durable(consumerDurable)
                .maxDeliver(maxDeliver)
                .ackPolicy(io.nats.client.api.AckPolicy.Explicit)
                .build();
    }
}
