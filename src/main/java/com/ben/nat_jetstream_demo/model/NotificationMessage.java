package com.ben.nat_jetstream_demo.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure Domain Object: NotificationMessage.
 * Focuses on business data only. System concerns are moved to MessageMetadata.
 */
public class NotificationMessage {
    private String title;
    private String name;
    private String content;
    private List<String> tags;
    private String lastError;
    private Integer retryCount;

    public NotificationMessage() {}

    public NotificationMessage(String title, String name, String content, List<String> tags, String lastError, Integer retryCount) {
        this.title = title;
        this.name = name;
        this.content = content;
        this.tags = tags;
        this.lastError = lastError;
        this.retryCount = retryCount;
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
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public static class NotificationMessageBuilder {
        private String title;
        private String name;
        private String content;
        private List<String> tags;
        private String lastError;
        private Integer retryCount;

        NotificationMessageBuilder() {}

        public NotificationMessageBuilder title(String title) { this.title = title; return this; }
        public NotificationMessageBuilder name(String name) { this.name = name; return this; }
        public NotificationMessageBuilder content(String content) { this.content = content; return this; }
        public NotificationMessageBuilder tags(List<String> tags) { this.tags = tags; return this; }
        public NotificationMessageBuilder lastError(String lastError) { this.lastError = lastError; return this; }
        public NotificationMessageBuilder retryCount(Integer retryCount) { this.retryCount = retryCount; return this; }

        public NotificationMessage build() {
            return new NotificationMessage(title, name, content, tags, lastError, retryCount);
        }
    }

    public static NotificationMessageBuilder builder() {
        return new NotificationMessageBuilder();
    }
}
