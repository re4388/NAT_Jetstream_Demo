## Problem Statement

目前系統雖然使用了 NATS JetStream，但尚未展示其最強大的功能之一：**訊息重播 (Message Replay)**。使用者（開發者或維運人員）需要一種方式來驗證系統能夠從歷史數據中的特定點（序列號或時間點）重新讀取並處理訊息，這在補償機制、錯誤恢復或系統遷移時非常重要。

## Solution

實作一個重播展示功能，包含：
1. **調整 Stream 配置**：將 `MY_STREAM` 的 `RetentionPolicy` 從 `WorkQueue` 改為 `Limits`。這是關鍵，因為 `WorkQueue` 模式下訊息一旦被 Ack 就會從 Stream 中刪除，無法重播。`Limits` 模式則允許訊息依據時間或大小限制保留在 Stream 中。
2. **Replay API**：提供 `/api/replay/sequence` 與 `/api/replay/time` 兩個端點。
3. **Ephemeral Consumer**：在觸發重播時，動態建立一個臨時的（Ephemeral）Push Consumer，並設定其 `DeliverPolicy` 以指向歷史起點。
4. **Log 輸出數據**：重播的訊息將直接輸出至應用程式日誌，標註 `[REPLAY-DATA]` 標籤以方便觀察。

## User Stories

1. 作為開發者，我希望能夠指定一個起始序號進行重播，以便我能檢查特定範圍的歷史訊息。
2. 作為維運人員，我希望能夠指定一個時間點進行重播，以便在系統發生故障後能夠重新讀取特定時段的訊息。
3. 作為使用者，我希望重播功能不會干擾現有的業務 Consumer，所以重播應該使用臨時的 Consumer。
4. 作為使用者，我希望重播過程中不需要手動 Ack 訊息，因為這通常是為了觀察或分析數據。

## Implementation Decisions

- **Stream 策略變更**：`RetentionPolicy.Limits` 將被用於展示。在生產環境中，這需要權衡磁碟空間與保留期的關係。
- **臨時 Dispatcher**：使用 `natsConnection.createDispatcher()` 建立獨立的線程處理重播訊息，避免阻塞主流程。
- **DeliverPolicy 配置**：
    - `DeliverPolicy.ByStartSequence` 用於序號重播。
    - `DeliverPolicy.ByStartTime` 用於時間重播。
- **無狀態消費**：`AckPolicy.None` 被選用，因為重播通常不改變 Stream 的消費狀態偏移量。

## Testing Decisions

- **日誌驗證**：檢查 Console 日誌中是否出現 `[REPLAY-DATA]` 開頭的內容。
- **序列號驗證**：確認輸出的 `streamSequence` 與重播要求的起始點一致。
- **副作用檢查**：確認重播後，原有的 `MainConsumer` 不會受到重複訊息的影響（因為這是新的 Ephemeral Consumer）。

## Out of Scope

- 重播訊息的自動補償處理（僅做展示輸出）。
- UI 界面集成。
- 大量數據下的重播性能優化。

## Further Notes

- 注意：一旦 Stream 從 `WorkQueue` 轉為 `Limits`，訊息將持續累積直至觸發 MaxAge 或 MaxBytes 限制。
- 如果之前已經存在的 Stream 是 `WorkQueue` 類型，可能需要先手動刪除或更新 Stream 配置才能生效。
