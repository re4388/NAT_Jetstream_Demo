# RFC: Messaging Model Deepening (Envelope Pattern)

## Context
The existing `NotificationMessage` is a "shallow model" that mixes business data (title, content) with infrastructure metadata (messageId, timestamp, retryCount). This creates confusion for API users and makes it hard to reuse metadata logic for other message types.

## Proposal: Message Envelope Pattern
Introduce a generic `MessageEnvelope<T>` that wraps any business payload.

### Structure
1. **`MessageMetadata`**: Standard set of fields (id, type, timestamp, source, correlationId).
2. **`MessageEnvelope<T>`**: The container with `metadata` and `data`.
3. **Domain Messages**: Pure classes like `Notification` or `Order` without infra fields.

### Benefits
- **Clean Contracts**: API controllers and producers work with pure domain objects.
- **Consistency**: All messages in the system follow the same envelope structure, making logging and auditing trivial.
- **Deep Integration**: `JetStreamClient` can automatically wrap outgoing data, and `JetStreamConsumerRegistry` can automatically unwrap incoming data.

### Expected Result
```java
// Producer Side
client.publish(new Notification("Hi", "Body")); // Client wraps it automatically

// Consumer Side
registry.subscribe(..., Notification.class, note -> {
    // note is a pure Notification object
});
```

## Implementation Steps
1. Create `MessageMetadata` and `MessageEnvelope` classes.
2. Refactor `NotificationMessage` into a pure `Notification` model.
3. Update `JetStreamClient` to handle the wrapping logic.
4. Update `JetStreamConsumerRegistry` to handle the unwrapping logic.
