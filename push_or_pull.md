# 本專案

### 結論：目前使用的是 __Push-based__ 模式。

### 證據分析：

1. __關鍵類別與方法__：在 `NatsJetStreamConsumerRegistry.java` 中，我們可以看到以下實作：

   ```java
   PushSubscribeOptions options = PushSubscribeOptions.builder()
           .durable(durable)
           .build();

   jetStream.subscribe(subject, dispatcher, natsHandler, false, options);
   ```

   這裡明確使用了 `PushSubscribeOptions` 類別，這是 NATS Java Client 中用於建立 __Push Consumer__ 的設定。

2. __訂閱機制__：

    - 使用了 `Dispatcher` 與 `MessageHandler`。在 Push 模式下，NATS Server 會主動將訊息「推」送到配置好的 `Dispatcher`，並由 `MessageHandler` (即 `natsHandler`) 進行處理。
    - 如果是 Pull 模式，程式碼通常會使用 `jetStream.subscribe(subject, pullOptions)` 獲取一個 `JetStreamSubscription` 對象，然後在迴圈中主動呼叫 `sub.fetch(batchSize, timeout)`。

3. __訊息確認 (Ack)__：

    - 在 `handleIncomingMessage` 中使用了 `msg.ack()` 與 `msg.nak()`。雖然 Pull 模式也會使用這些，但在當前的非同步 `MessageHandler` 結構中，這完全符合 Push 消費者的典型行為。

### 總結：

當前的系統架構是採用 __Push Consumer__，由 NATS Server 決定發送速率並主動推送訊息給應用的訂閱者。

如果你需要切換到 Pull-based 模式（通常為了更好的流量控制/批次處理），或是需要更深入的架構調整，請讓我知道。


---

根據目前的程式碼實作，該系統使用的是 __At-least-once__ 傳遞語意。

### 詳細分析：

1. __為什麼是 At-least-once？__

    - __顯示確認機制 (Explicit Ack)__：在 `NatsJetStreamConsumerRegistry.java` 中，我們可以看到程式碼在處理完邏輯後手動呼叫了 `msg.ack()`。這保證了訊息只有在被應用程式成功處理後，NATS Server 才會將其標記為已處理。
    - __重試機制 (Nak/Redelivery)__：在發生異常時，程式碼會呼叫 `msg.nak()` (Negative Ack)，這會要求 NATS Server 重新發送該訊息。此外，JetStream 的 Consumer 通常會有 `AckWait` 設定，如果時間內沒有收到 `ack`，訊息也會被重發。
    - __持久化 (Durable)__：使用了 `Durable` 消費者，這意味著 NATS 會追蹤消費進度，即使應用程式重啟，也會從上次確認的位置繼續推波訊息。

2. __為什麼不是 At-most-once？__

    - At-most-once 通常指 Core NATS 的 Fire-and-forget 模式，或者 JetStream 設定了 `AckNone` 模式。
    - 目前的程式碼要求了 `ack`，確保了訊息不會因為網路暫態故障或應用程式崩潰而在未處理前就消失。

3. __為什麼不是 Exactly-once？__

    - NATS JetStream 要達成真正的 __Exactly-once__ 需要滿足兩個條件：

        1. __發送端去重 (Producer Deduplication)__：這需要 Producer 設置 `NatsMsgId` 標頭，且 Stream 配置了 `DuplicateWindow`。我們需要檢查 `NotificationProducer.java` 來確認。
        2. __接收端冪等性 (Consumer Idempotency)__：Consumer 需要具備處理重複訊息的能力。

    - 雖然 JetStream 提供了基礎設施，但僅憑目前的 `subscribe` 程式碼邏輯，它表現出的標準行為是 __At-least-once__（即保證收到，但可能重複處理）


檢查了 NotificationProducer.java 及其底層的 NatsJetStreamClient.java。

檢查結果與結論：
目前系統的傳遞語意確實是 At-least-once，且尚未達到 Exactly-once。

1. Producer 端分析 (NatsJetStreamClient.java)：
   缺乏 NatsMsgId Header：雖然程式碼中有 String msgId = finalEnvelope.getMetadata().getId(); 並將其紀錄在 Log 中，但它並沒有將此 ID 放入 NATS 協定要求的 Nats-Msg-Id Header 中。
   NATS 去重機制：NATS JetStream 依賴 Nats-Msg-Id Header 來進行伺服器端去重。目前的實作只是將 ID 封裝在 JSON Payload 內，這對 NATS Server 來說是不可見的，因此 Server 無法自動過濾重複發送的訊息。
   發送方式：使用的是 jetStream.publish(...)，這是同步發送並等待 Ack，符合可靠發送的基本要求，但不足以支援 Exactly-once。
2. Consumer 端分析 (NatsJetStreamConsumerRegistry.java)：
   手動確認 (Manual Ack)：確保了訊息不會遺失（At-least-once）。
   缺乏冪等處理：Consumer 接收到訊息後直接執行業務邏輯並 Ack，並沒有根據 MessageMetadata 中的 ID 去檢查是否已經處理過該訊息。
   總結比較：
   特性	目前實作狀態	Exactly-once 所需
   傳遞模式	At-least-once	Exactly-once
   發送端去重	❌ (僅 Log 紀錄 ID)	需要設置 Nats-Msg-Id Header
   Server 端配置	⚠️ 未知 (需檢查 Stream 配置)	需要配置 DuplicateWindow
   接收端冪等	❌ 尚未實作	需要根據 ID 檢查重複處理
   建議：
   如果您希望升級到 Exactly-once，需要進行以下修改：

修改 Producer：在傳送時將 MessageMetadata 的 ID 設置到 Nats-Msg-Id 標頭中。
修改 Consumer：實作簡單的去重機制（例如使用 Redis 或資料庫紀錄已處理的 Message ID）。



---


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

