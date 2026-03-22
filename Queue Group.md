# 本專案

目前的實作屬於 __Queue Group（Load Balancing）__ 模式的變體，但在 JetStream 中稱為 __Shared Durable Consumer__。

### 詳細歸類分析：

1. __為什麼是 Queue Group / Load Balancing 效果？__

    - 在 `NatsConfig.java` 中，Consumer 被配置為使用相同的 `Durable Name`。
    - 在 JetStream 中，當多個實例使&#x7528;__&#x540C;一個 Durable 名&#x7A31;__&#x8A02;閱同一個 Stream 時，NATS Server 會自動將訊息分派給這些實例。這在功能上等同於 Core NATS 的 `Queue Group`，達到了負載平衡的效果。
    - __證據__：`NatsConfig.java` 中定義了 `consumerDurable` (例如 `notification-consumer`)，且 `NatsJetStreamConsumerRegistry.java` 使用此 Durable 名稱進行訂閱。

2. __為什麼不是其他的模式？__

    - __Subject-based Pub/Sub (Fan-out)__：

        - 在 Core NATS 中，Pub/Sub 預設是 Fan-out（一對多）。
        - 但在當前的 JetStream 配置中，因為指定了 `Durable Name`，如果啟動多個應用程式實例，訊息會被「分散」處理（Load Balanced），而不是每個實例都收到同一份訊息。
        - __注意__：如果您想要 Fan-out（每個實例都收到），每個實例必須使&#x7528;__&#x4E0D;&#x540C;__&#x7684; Durable 名稱（例如包含 Hostname 或 UUID）。

    - __Traditional Topic Pub/Sub__：這是 Core NATS 的基本行為，不具備訊息持久化。目前的程式碼使用了 JetStream (Durable)，所以屬於更進階的持久化消費模式。

    - __Request/Reply__：

        - 程式碼中沒有看到 `connection.request()` 或 `msg.respond()` 的調用。
        - 這是非同步的事件驅動架構，發送端不需要等待接收端的即時回覆。

### 總結歸類：

目前的模式是 __JetStream Push-based Durable Consumer__。

- &#x5F9E;__&#x5206;發行&#x70BA;__&#x4F86;看：它表現為 __Queue Group (Load Balancing)__。
- &#x5F9E;__&#x5E95;層機&#x5236;__&#x4F86;看：它利用了 NATS JetStream 的 __Shared Subscription__ 特性。

__實作細節提醒__： 如果您目前只啟動了一個實例，它看起來像單純的 Pub/Sub；但只要啟動第二個實例並給予同樣的 Durable ID，它們就會自動開始協同處理（分擔流量），這就是 Load Balancing 的運作方式。

如果您需要調整為 __Fan-out__ 模式（讓所有實體都收到每一條訊息），我們需要修改 Consumer 註冊邏輯，使其 Durable 名稱具有唯一性。

