package com.ben.nat_jetstream_demo.service;

import io.nats.client.Dispatcher;
import io.nats.client.JetStream;
import io.nats.client.PushSubscribeOptions;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
public class ReplayService {
    private static final Logger log = LoggerFactory.getLogger(ReplayService.class);

    private final JetStream jetStream;
    private final io.nats.client.Connection natsConnection;

    @Value("${nats.subject}")
    private String subject;

    public ReplayService(JetStream jetStream, io.nats.client.Connection natsConnection) {
        this.jetStream = jetStream;
        this.natsConnection = natsConnection;
    }

    public void replayFromSequence(long sequence) {
        log.info("[ReplayService] Starting replay from sequence: {}", sequence);
        
        ConsumerConfiguration conf = ConsumerConfiguration.builder()
                .deliverPolicy(DeliverPolicy.ByStartSequence)
                .startSequence(sequence)
                .ackPolicy(io.nats.client.api.AckPolicy.None) // Replay usually doesn't need to ack
                .build();
        
        subscribeAndLog(conf);
    }

    public void replayFromTime(ZonedDateTime startTime) {
        log.info("[ReplayService] Starting replay from time: {}", startTime);
        
        ConsumerConfiguration conf = ConsumerConfiguration.builder()
                .deliverPolicy(DeliverPolicy.ByStartTime)
                .startTime(startTime)
                .ackPolicy(io.nats.client.api.AckPolicy.None)
                .build();
        
        subscribeAndLog(conf);
    }

    private void subscribeAndLog(ConsumerConfiguration conf) {
        try {
            PushSubscribeOptions pso = PushSubscribeOptions.builder()
                    .configuration(conf)
                    .build();

            // Create an ephemeral dispatcher to handle messages
            Dispatcher dispatcher = natsConnection.createDispatcher();
            
            dispatcher.subscribe(subject, String.valueOf(pso), msg -> {
                String payload = new String(msg.getData());
                log.info("[REPLAY-DATA] Seq: {}, Subject: {}, Payload: {}", 
                        msg.metaData().streamSequence(), 
                        msg.getSubject(), 
                        payload);
            });

            log.info("[ReplayService] Ephemeral subscription created for replay.");
            
            // In a real scenario, we might want to close this dispatcher after some time or condition
            // For demo purposes, we let it run.
            
        } catch (Exception e) {
            log.error("[ReplayService] Failed to start replay: {}", e.getMessage(), e);
        }
    }
}
