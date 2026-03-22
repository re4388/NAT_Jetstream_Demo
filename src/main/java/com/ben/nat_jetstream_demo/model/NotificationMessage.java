package com.ben.nat_jetstream_demo.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private String messageId;
    private String correlationId;
    
    private String title;
    private String name;
    private String content;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "UTC")
    private Instant timestamp;
    
    private String source;
    private Priority priority;
    private List<String> tags;
    
    private Integer retryCount;
    private String lastError;
    
    public enum Priority {
        LOW, NORMAL, HIGH, CRITICAL
    }
    
    public static NotificationMessage create(String title, String name, String content) {
        return NotificationMessage.builder()
                .messageId(java.util.UUID.randomUUID().toString())
                .correlationId(java.util.UUID.randomUUID().toString())
                .title(title)
                .name(name)
                .content(content)
                .timestamp(Instant.now())
                .source("producer")
                .priority(Priority.NORMAL)
                .tags(List.of())
                .retryCount(0)
                .lastError(null)
                .build();
    }
    
    public static NotificationMessage createWithTags(String title, String name, String content, List<String> tags) {
        return NotificationMessage.builder()
                .messageId(java.util.UUID.randomUUID().toString())
                .correlationId(java.util.UUID.randomUUID().toString())
                .title(title)
                .name(name)
                .content(content)
                .timestamp(Instant.now())
                .source("producer")
                .priority(Priority.NORMAL)
                .tags(tags)
                .retryCount(0)
                .lastError(null)
                .build();
    }
}
