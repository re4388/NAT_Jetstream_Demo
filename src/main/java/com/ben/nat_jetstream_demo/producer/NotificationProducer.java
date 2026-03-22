package com.ben.nat_jetstream_demo.producer;

import com.ben.nat_jetstream_demo.config.NatsConfig;
import com.ben.nat_jetstream_demo.model.NotificationMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.api.PublishAck;
import io.nats.client.impl.NatsMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationProducer {

    private final NatsConfig natsConfig;
    private final ObjectMapper objectMapper;

    @Value("${nats.subject:notifications.test}")
    private String subject;

    public NotificationProducer(NatsConfig natsConfig, ObjectMapper objectMapper) {
        this.natsConfig = natsConfig;
        this.objectMapper = objectMapper;
    }

    public String publish(NotificationMessage message) throws JsonProcessingException, java.io.IOException, JetStreamApiException {
        JetStream js = natsConfig.jetStream();
        
        if (js == null) {
            throw new IllegalStateException("NATS JetStream is not available");
        }
        
        if (message.getMessageId() == null) {
            message.setMessageId(java.util.UUID.randomUUID().toString());
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(java.time.Instant.now());
        }
        if (message.getSource() == null) {
            message.setSource("producer");
        }

        String json = objectMapper.writeValueAsString(message);
        byte[] payload = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        try {
            PublishAck ack = js.publish(NatsMessage.builder()
                    .subject(subject)
                    .data(payload)
                    .build());
            log.info("[Producer] Message published - id: {}, subject: {}, seq: {}",
                    message.getMessageId(), subject, ack.getSeqno());
            return message.getMessageId();
        } catch (Exception e) {
            log.error("[Producer] Failed to publish message: {}", e.getMessage());
            throw e;
        }
    }

    public String send(String title, String name, String content) throws JsonProcessingException, java.io.IOException, JetStreamApiException {
        NotificationMessage message = NotificationMessage.create(title, name, content);
        return publish(message);
    }

    public String sendWithTags(String title, String name, String content, java.util.List<String> tags)
            throws JsonProcessingException, java.io.IOException, JetStreamApiException {
        NotificationMessage message = NotificationMessage.createWithTags(title, name, content, tags);
        return publish(message);
    }
}
