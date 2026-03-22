package com.ben.nat_jetstream_demo.config;

import com.ben.nat_jetstream_demo.model.MessageEnvelope;
import java.util.Map;

public interface JetStreamClient {
    /**
     * Publish a message to the default subject.
     * The message will be automatically wrapped in a MessageEnvelope.
     * @param message The business message object.
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
        /**
         * Sets the business payload. Will be wrapped in an envelope if go() is called.
         */
        PublishRequestBuilder payload(Object payload);
        
        /**
         * Directly provides a pre-wrapped envelope.
         */
        PublishRequestBuilder envelope(MessageEnvelope<?> envelope);
        
        PublishRequestBuilder header(String key, String value);
        PublishRequestBuilder headers(Map<String, String> headers);
        
        /**
         * Finalizes and sends the message.
         */
        String go();
    }
}
