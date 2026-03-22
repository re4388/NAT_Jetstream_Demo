package com.ben.nat_jetstream_demo.producer;

import com.ben.nat_jetstream_demo.config.JetStreamClient;
import com.ben.nat_jetstream_demo.model.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationProducer {
    private static final Logger log = LoggerFactory.getLogger(NotificationProducer.class);

    private final JetStreamClient jetStreamClient;

    public NotificationProducer(JetStreamClient jetStreamClient) {
        this.jetStreamClient = jetStreamClient;
    }

    public String publish(NotificationMessage message) {
        // Source will be automatically added by JetStreamClient via MessageEnvelope
        return jetStreamClient.publish(message);
    }

    public String send(String title, String name, String content) {
        NotificationMessage message = NotificationMessage.create(title, name, content);
        return publish(message);
    }
}
