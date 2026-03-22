# RFC: Consumer Architecture Abstraction (JetStreamConsumerRegistry)

## Context
Currently, each NATS consumer (`MainConsumer`, `DlqConsumer`) manually manages its own NATS subscriptions, JSON deserialization, and retry/DLQ logic. This leads to code duplication and leaky abstractions.

## Proposal: Functional Consumer Registry
Introduce a `JetStreamConsumerRegistry` that provides a high-level API to register message handlers.

### Target Developer Experience
```java
registry.subscribe(
    "notifications.test", 
    "durable-name", 
    NotificationMessage.class, 
    msg -> {
        // Business logic here
        // If Exception is thrown, registry automatically handles Nak or DLQ
    }
);
```

### Key Responsibilities of the Registry (The Deep Module)
1. **Infrastructure Management**: Handles NATS `Connection`, `Dispatcher`, and `PushSubscribeOptions`.
2. **Lifecycle Support**: Ensures consumers are started during application startup.
3. **Resilience Policy**:
    * Automatically `Ack` on successful handler execution.
    * Automatically `Nak` on exception (triggering NATS redelivery).
    * Automatically move to `DLQ` after 3 (configurable) failed attempts.
4. **Serialization Isolation**: Uses the central `ObjectMapper` to hide byte-array conversions.

### Implementation Plan
1. Create `JetStreamConsumerRegistry` interface and implementation.
2. Define a `ReliableMessageHandler<T>` functional interface.
3. Refactor `MainConsumer` to remove low-level NATS code.
4. (Optional) Enhance the registry to support monitoring of active consumers.
