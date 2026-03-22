package com.ben.nat_jetstream_demo.config;

import com.ben.nat_jetstream_demo.model.MessageEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.JetStream;
import io.nats.client.impl.Headers;
import io.nats.client.impl.NatsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class NatsJetStreamClient implements JetStreamClient {
    private static final Logger log = LoggerFactory.getLogger(NatsJetStreamClient.class);

    private final JetStream jetStream;
    private final ObjectMapper objectMapper;

    @Value("${nats.subject:notifications.test}")
    private String defaultSubject;

    @Value("${spring.application.name:nat-jetstream-demo}")
    private String appName;

    public NatsJetStreamClient(JetStream jetStream, ObjectMapper objectMapper) {
        this.jetStream = jetStream;
        this.objectMapper = objectMapper;
    }

    @Override
    public String publish(Object message) {
        return publishTo(defaultSubject).payload(message).go();
    }

    @Override
    public PublishRequestBuilder publishTo(String subject) {
        return new NatsPublishRequestBuilder(subject);
    }

    private class NatsPublishRequestBuilder implements PublishRequestBuilder {
        private final String subject;
        private Object payload;
        private MessageEnvelope<?> envelope;
        private final Map<String, String> headersMap = new HashMap<>();

        public NatsPublishRequestBuilder(String subject) {
            this.subject = subject;
        }

        @Override
        public PublishRequestBuilder payload(Object payload) {
            this.payload = payload;
            return this;
        }

        @Override
        public PublishRequestBuilder envelope(MessageEnvelope<?> envelope) {
            this.envelope = envelope;
            return this;
        }

        @Override
        public PublishRequestBuilder header(String key, String value) {
            this.headersMap.put(key, value);
            return this;
        }

        @Override
        public PublishRequestBuilder headers(Map<String, String> headers) {
            if (headers != null) {
                this.headersMap.putAll(headers);
            }
            return this;
        }

        @Override
        public String go() {
            try {
                if (jetStream == null) {
                    throw new IllegalStateException("NATS JetStream is not available");
                }

                MessageEnvelope<?> finalEnvelope = (envelope != null) ? envelope : MessageEnvelope.wrap(payload, appName);
                byte[] data = objectMapper.writeValueAsBytes(finalEnvelope);
                
                Headers natsHeaders = new Headers();
                headersMap.forEach(natsHeaders::add);

                var ack = jetStream.publish(NatsMessage.builder()
                        .subject(subject)
                        .headers(natsHeaders)
                        .data(data)
                        .build());

                String msgId = finalEnvelope.getMetadata().getId();
                log.info("[JetStreamClient] Wrapped & Published to {}: id={}, seq={}", subject, msgId, ack.getSeqno());
                return msgId;
            } catch (Exception e) {
                log.error("[JetStreamClient] Failed to publish message: {}", e.getMessage());
                throw new RuntimeException("Messaging failure", e);
            }
        }
    }
}
