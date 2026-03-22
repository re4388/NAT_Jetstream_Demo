# RFC: Deepen NATS Client Architecture (JetStreamClient)

## Context
The current implementation of `NotificationProducer` and `NatsConfig` are tightly coupled with the low-level `io.nats.client` library. This creates 'shallow modules' where the complexity of NATS (serialization, message metadata, connection handling) is exposed to business logic providers.

## Proposal: Deepen NATS Integration using JetStreamClient

Introduce a **Deep Module** `JetStreamClient` that encapsulates the NATS JetStream complexity.

### Target Interface
```java
public interface JetStreamClient {
    /** Simple publish to default subject */
    String publish(Object message);
    
    /** Fluent API for advanced scenarios */
    PublishRequestBuilder publishTo(String subject);
}
```

### Key Improvements
1. **Encapsulation**: Hide serialization (Jackson) and NATS-specific classes (PublishAck, NatsMessage) behind a domain-friendly interface.
2. **Metadata Automation**: Automatically inject `messageId` and `timestamp` if missing, reducing boilerplate in producers.
3. **Resilience**: Centralize error handling and logging for all NATS publish operations.
4. **Testability**: Enabling easy mocking/stubbing of the entire messaging boundary without mocking complex NATS internals.

### Affected Components
- `NatsConfig`: Reduce responsibility to connection management only.
- `NotificationProducer`: Refactor to use `JetStreamClient`.
- `MainConsumer`: Move DLQ publishing logic to `JetStreamClient`.

## Definition of Done
- [ ] New `JetStreamClient` implementation.
- [ ] Refactored `NotificationProducer`.
- [ ] Unit tests for `JetStreamClient` boundaries.
