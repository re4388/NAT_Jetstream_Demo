package com.ben.nat_jetstream_demo.config;

/**
 * Registry to manage NATS JetStream consumers with reliable message handling.
 */
public interface JetStreamConsumerRegistry {
    /**
     * Subscribe to a subject with automatic retry and DLQ logic.
     */
    <T> void subscribe(String subject,
                       String durable,
                       Class<T> payloadType,
                       ReliableMessageHandler<T> handler);
}
