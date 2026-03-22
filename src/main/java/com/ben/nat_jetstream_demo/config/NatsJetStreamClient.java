package com.ben.nat_jetstream_demo.config;

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
import java.util.UUID;
import java.lang.reflect.Method;

@Component
public class NatsJetStreamClient implements JetStreamClient {
    private static final Logger log = LoggerFactory.getLogger(NatsJetStreamClient.class);

    private final JetStream jetStream;
    private final ObjectMapper objectMapper;

    @Value("${nats.subject:notifications.test}")
    private String defaultSubject;

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

                ensureMetadata(payload);
                byte[] data = objectMapper.writeValueAsBytes(payload);
                
                Headers natsHeaders = new Headers();
                headersMap.forEach(natsHeaders::add);

                var ack = jetStream.publish(NatsMessage.builder()
                        .subject(subject)
                        .headers(natsHeaders)
                        .data(data)
                        .build());

                String msgId = extractMessageId(payload);
                log.info("[JetStreamClient] Published to {}: id={}, seq={}", subject, msgId, ack.getSeqno());
                return msgId;
            } catch (Exception e) {
                log.error("[JetStreamClient] Failed to publish message: {}", e.getMessage());
                throw new RuntimeException("Messaging failure", e);
            }
        }

        private void ensureMetadata(Object obj) {
            try {
                Class<?> clazz = obj.getClass();
                trySet(obj, clazz, "setMessageId", String.class, UUID.randomUUID().toString());
                trySet(obj, clazz, "setTimestamp", java.time.Instant.class, java.time.Instant.now());
            } catch (Exception ignored) {}
        }

        private void trySet(Object target, Class<?> clazz, String methodName, Class<?> paramType, Object value) {
            try {
                String getterName = "get" + methodName.substring(3);
                Method getter = clazz.getMethod(getterName);
                if (getter.invoke(target) == null) {
                    Method setter = clazz.getMethod(methodName, paramType);
                    setter.invoke(target, value);
                }
            } catch (Exception ignored) {}
        }

        private String extractMessageId(Object obj) {
            try {
                Method getter = obj.getClass().getMethod("getMessageId");
                Object val = getter.invoke(obj);
                return val != null ? val.toString() : "unknown";
            } catch (Exception e) {
                return "unknown";
            }
        }
    }
}
