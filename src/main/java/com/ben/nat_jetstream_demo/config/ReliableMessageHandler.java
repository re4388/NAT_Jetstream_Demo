package com.ben.nat_jetstream_demo.config;

import com.ben.nat_jetstream_demo.model.MessageMetadata;

/**
 * Functional interface for handling a message from JetStream with metadata.
 * @param <T> The expected payload type.
 */
@FunctionalInterface
public interface ReliableMessageHandler<T> {
    /**
     * Handles the business payload.
     */
    void handle(T payload) throws Exception;

    /**
     * Optional: Handles the message with metadata. 
     * Default implementation just calls handle(payload).
     */
    default void handle(T payload, MessageMetadata metadata) throws Exception {
        handle(payload);
    }
}
