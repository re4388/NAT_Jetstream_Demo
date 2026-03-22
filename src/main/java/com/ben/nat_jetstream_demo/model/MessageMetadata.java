package com.ben.nat_jetstream_demo.model;

import java.time.Instant;

/**
 * Standard metadata for all messages in the system.
 */
public class MessageMetadata {
    private String id;
    private Instant timestamp;
    private String source;
    private String correlationId;
    private String type;

    public MessageMetadata() {}

    public MessageMetadata(String id, Instant timestamp, String source, String correlationId, String type) {
        this.id = id;
        this.timestamp = timestamp;
        this.source = source;
        this.correlationId = correlationId;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
