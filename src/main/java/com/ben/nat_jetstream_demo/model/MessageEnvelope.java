package com.ben.nat_jetstream_demo.model;

/**
 * Container for both business data and system metadata.
 */
public class MessageEnvelope<T> {
    private MessageMetadata metadata;
    private T data;

    public MessageEnvelope() {}

    public MessageEnvelope(MessageMetadata metadata, T data) {
        this.metadata = metadata;
        this.data = data;
    }

    public MessageMetadata getMetadata() { return metadata; }
    public void setMetadata(MessageMetadata metadata) { this.metadata = metadata; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public static <T> MessageEnvelope<T> wrap(T data, String source) {
        MessageMetadata md = new MessageMetadata();
        md.setId(java.util.UUID.randomUUID().toString());
        md.setTimestamp(java.time.Instant.now());
        md.setSource(source);
        md.setType(data.getClass().getSimpleName());
        return new MessageEnvelope<>(md, data);
    }
}
