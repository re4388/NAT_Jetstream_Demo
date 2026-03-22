package com.ben.nat_jetstream_demo.config;

/**
 * Functional interface for handling a message from JetStream uniquely.
 * @param <T> The expected payload type.
 */
@FunctionalInterface
public interface ReliableMessageHandler<T> {
    void handle(T payload) throws Exception;
}
