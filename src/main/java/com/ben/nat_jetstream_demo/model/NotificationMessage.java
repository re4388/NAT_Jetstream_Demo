package com.ben.nat_jetstream_demo.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Common notification message model.
 * Fields: title, name, content, timestamp, tags, lastError, retryCount, messageId, correlationId, source.
 */
public class NotificationMessage {
    private String messageId;
    private String correlationId;
    private String title;
    private String name;
    private String content;
    private Instant timestamp;
    private List<String> tags;
    private String lastError;
    private Integer retryCount;
    private String source;

    public NotificationMessage() {}

    public NotificationMessage(String messageId, String correlationId, String title, String name, String content, Instant timestamp, List<String> tags, String lastError, Integer retryCount, String source) {
        this.messageId = messageId;
        this.correlationId = correlationId;
        this.title = title;
        this.name = name;
        this.content = content;
        this.timestamp = timestamp;
        this.tags = tags;
        this.lastError = lastError;
        this.retryCount = retryCount;
        this.source = source;
    }

    public static NotificationMessage create(String title, String name, String content) {
        NotificationMessage msg = new NotificationMessage();
        msg.setTitle(title);
        msg.setName(name);
        msg.setContent(content);
        msg.setTags(new ArrayList<>());
        msg.setRetryCount(0);
        return msg;
    }

    public static NotificationMessage createWithTags(String title, String name, String content, List<String> tags) {
        NotificationMessage msg = create(title, name, content);
        msg.setTags(tags != null ? tags : new ArrayList<>());
        return msg;
    }

    // Getters and Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public static class NotificationMessageBuilder {
        private String messageId;
        private String correlationId;
        private String title;
        private String name;
        private String content;
        private Instant timestamp;
        private List<String> tags;
        private String lastError;
        private Integer retryCount;
        private String source;

        NotificationMessageBuilder() {}

        public NotificationMessageBuilder messageId(String messageId) { this.messageId = messageId; return this; }
        public NotificationMessageBuilder correlationId(String correlationId) { this.correlationId = correlationId; return this; }
        public NotificationMessageBuilder title(String title) { this.title = title; return this; }
        public NotificationMessageBuilder name(String name) { this.name = name; return this; }
        public NotificationMessageBuilder content(String content) { this.content = content; return this; }
        public NotificationMessageBuilder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public NotificationMessageBuilder tags(List<String> tags) { this.tags = tags; return this; }
        public NotificationMessageBuilder lastError(String lastError) { this.lastError = lastError; return this; }
        public NotificationMessageBuilder retryCount(Integer retryCount) { this.retryCount = retryCount; return this; }
        public NotificationMessageBuilder source(String source) { this.source = source; return this; }

        public NotificationMessage build() {
            return new NotificationMessage(messageId, correlationId, title, name, content, timestamp, tags, lastError, retryCount, source);
        }
    }

    public static NotificationMessageBuilder builder() {
        return new NotificationMessageBuilder();
    }
}
