package com.ben.nat_jetstream_demo.config;

import java.util.Map;

public interface JetStreamClient {
    /**
     * Publish a message to the default subject configured in the system.
     * @param message The message object to be serialized and sent.
     * @return The message ID.
     */
    String publish(Object message);

    /**
     * Start a fluent request to publish to a specific subject.
     * @param subject The NATS subject.
     * @return A builder to configure and send the message.
     */
    PublishRequestBuilder publishTo(String subject);

    interface PublishRequestBuilder {
        PublishRequestBuilder payload(Object payload);
        PublishRequestBuilder header(String key, String value);
        PublishRequestBuilder headers(Map<String, String> headers);
        String go();
    }
}
