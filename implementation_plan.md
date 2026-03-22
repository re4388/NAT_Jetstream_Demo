# Implementation Plan

[Overview]
使用 Spring Boot 建立一個整合 NATS JetStream 與 MinIO 的通知系統，包含訊息生產者 (Producer) 與消費者 (Consumer)。

此專案旨在示範如何透過 API 接收通知請求，將其發送至 NATS JetStream 佇列，並由非同步的消費者接收後，依照特定格式命名並儲存至 MinIO 物件儲存空間。這能確保通知處理的可靠性與解耦合。

[Types]
定義通知訊息的數據結構。

- `NotificationMessage`: 
  - `title` (String): 通知標題
  - `name` (String): 寄件者名稱
  - `message` (String): 通知內容

[Files]
建立 Docker 配置與相關 Java 類別。

- `docker-compose.yml`: 設定 NATS (含 JetStream) 與 MinIO 容器環境。
- `pom.xml`: 加入 `jnats`, `minio`, `spring-boot-starter-web`, `lombok` 等依賴。
- `src/main/resources/application.properties`: 配置 NATS 與 MinIO 的連線資訊。
- `src/main/java/com/ben/nat_jetstream_demo/config/NatsConfig.java`: NATS 連線與 JetStream 管理配置。
- `src/main/java/com/ben/nat_jetstream_demo/config/MinioConfig.java`: MinIO 客戶端配置。
- `src/main/java/com/ben/nat_jetstream_demo/model/NotificationMessage.java`: 訊息資料模型。
- `src/main/java/com/ben/nat_jetstream_demo/controller/NotificationController.java`: 提供發送通知的 REST API。
- `src/main/java/com/ben/nat_jetstream_demo/producer/NotificationProducer.java`: 將訊息推播至 JetStream。
- `src/main/java/com/ben/nat_jetstream_demo/consumer/NotificationConsumer.java`: 監聽並處理訊息，存入 MinIO。

[Functions]
實現訊息的收發與儲存邏輯。

- `NotificationController.sendNotification(NotificationMessage)`: 接收 POST 請求。
- `NotificationProducer.publish(NotificationMessage)`: 封裝 NATS JetStream publish 邏輯。
- `NotificationConsumer.onMessage(Message)`: 實作 `MessageHandler` 介面，解析訊息並調用 MinIO 儲存。
- `NotificationConsumer.generateFileName()`: 依照 `msg_{year}_{date}_{hourmin}_sec` 格式生成檔名。

[Classes]
各組件的功能定義。

- `NatsConfig`: 初始化 `Connection` 並且預先建立 `NOTIFICATIONS` Stream。
- `MinioConfig`: 初始化 `MinioClient` 並且確保 `notifications` bucket 存在。
- `NotificationConsumer`: 使用 @PostConstruct 啟動 Push-based 或 Pull-based 訂閱。

[Dependencies]
新增必要的程式庫。

- `io.nats:jnats:2.17.x`: NATS Java 客戶端。
- `io.minio:minio:8.5.x`: MinIO Java 客戶端。
- `org.springframework.boot:spring-boot-starter-web`: API 支援。
- `org.projectlombok:lombok`: 簡化 POJO。

[Testing]
驗證流程正確性。

- 使用 Postman 測試 API Endpoint。
- 查看 NATS 管理介面 (可選) 或日誌確認訊息推播。
- 檢查 MinIO 控制台確認檔案是否按正確命名格式產生。

[Implementation Order]
開發與部署順序。

1. 撰寫 `docker-compose.yml` 並啟動 NATS 與 MinIO 服務。
2. 更新 `pom.xml` 加入所需依賴。
3. 在 `application.properties` 設定連線環境變數。
4. 建立 `NotificationMessage` 模型與 `NatsConfig`/`MinioConfig` 配置類。
5. 實作 `NotificationProducer` 與 `NotificationController` 並測試 API 到 NATS 的路徑。
6. 實作 `NotificationConsumer` 包含檔名生成邏輯與 MinIO 上傳功能。
7. 進行端到端 (E2E) 整合測試。
