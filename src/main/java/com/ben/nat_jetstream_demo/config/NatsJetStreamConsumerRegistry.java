package com.ben.nat_jetstream_demo.config;

import com.ben.nat_jetstream_demo.model.MessageEnvelope;
import com.ben.nat_jetstream_demo.model.MessageMetadata;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class NatsJetStreamConsumerRegistry implements JetStreamConsumerRegistry {
    private static final Logger log = LoggerFactory.getLogger(NatsJetStreamConsumerRegistry.class);

    private final Connection connection;
    private final JetStream jetStream;
    private final JetStreamClient jetStreamClient;
    private final ObjectMapper objectMapper;

    @Value("${nats.consumer.maxDeliver:3}")
    private int maxDeliver;

    @Value("${nats.dlq-subject:notifications.dlq}")
    private String dlqSubject;

    public NatsJetStreamConsumerRegistry(Connection connection, 
                                        JetStream jetStream, 
                                        JetStreamClient jetStreamClient,
                                        ObjectMapper objectMapper) {
        this.connection = connection;
        this.jetStream = jetStream;
        this.jetStreamClient = jetStreamClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> void subscribe(String subject,
                              String durable,
                              Class<T> payloadType,
                              ReliableMessageHandler<T> handler) {
        if (connection == null || jetStream == null) {
            log.warn("[ConsumerRegistry] NATS not available, skipping subscription for {}", subject);
            return;
        }

        try {
            Dispatcher dispatcher = connection.createDispatcher();
            
            MessageHandler natsHandler = msg -> {
                handleIncomingMessage(msg, payloadType, handler);
            };

            PushSubscribeOptions options = PushSubscribeOptions.builder()
                    .durable(durable)
                    .build();

            jetStream.subscribe(subject, dispatcher, natsHandler, false, options);
            log.info("[ConsumerRegistry] Subscribed to {} with durable {}", subject, durable);

        } catch (Exception e) {
            log.error("[ConsumerRegistry] Failed to subscribe to {}: {}", subject, e.getMessage());
        }
    }

    private <T> void handleIncomingMessage(Message msg,
                                           Class<T> payloadType, 
                                           ReliableMessageHandler<T> handler) {
        T payload = null;
        MessageMetadata metadata = null;
        try {
            String rawData = new String(msg.getData(), StandardCharsets.UTF_8);
            
            // Try to detect if it's an envelope or raw payload
            JsonNode node = objectMapper.readTree(rawData);
            if (node.has("metadata") && node.has("data")) {
                // It's an envelope
                MessageEnvelope<T> envelope = objectMapper.readValue(rawData, 
                    objectMapper.getTypeFactory().constructParametricType(MessageEnvelope.class, payloadType));
                payload = envelope.getData();
                metadata = envelope.getMetadata();
            } else {
                // Fallback to direct mapping for backward compatibility or different senders
                payload = objectMapper.readValue(rawData, payloadType);
                // Synthesize basic metadata
                metadata = new MessageMetadata();
                metadata.setId("legacy-" + msg.metaData().streamSequence());
                metadata.setTimestamp(java.time.Instant.now());
                metadata.setType(payloadType.getSimpleName());
            }

            // Execute business logic with metadata
            handler.handle(payload, metadata);
            
            // Success -> Ack
            msg.ack();

        } catch (Exception e) {
            log.error("[ConsumerRegistry] Error processing message: {}", e.getMessage());
            handleFailure(msg, payload, e.getMessage());
        }
    }

    private void handleFailure(Message msg, Object payload, String error) {
        try {
            long delivered = msg.metaData().deliveredCount();
            
            if (delivered >= maxDeliver) {
                log.warn("[ConsumerRegistry] Max deliver reached ({}). Moving to DLQ.", delivered);
                if (payload != null) {
                    jetStreamClient.publishTo(dlqSubject).payload(payload).go();
                } else {
                    jetStream.publish(dlqSubject, msg.getData());
                }
                msg.ack();
            } else {
                log.info("[ConsumerRegistry] Nak sent for redelivery (Attempt {})", delivered);
                msg.nak();
            }
        } catch (Exception ex) {
            log.error("[ConsumerRegistry] Critical failure in error handler: {}", ex.getMessage());
        }
    }
}
