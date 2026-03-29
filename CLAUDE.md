# CLAUDE.md

## Project Overview
NATS JetStream 訊息傳遞系統的 PoC 原型，展示可靠訊息傳遞、DLQ (Dead Letter Queue)、訊息重播和混沌測試等功能。

**Tech Stack:**
- Language: Java 17
- Framework: Spring Boot 3.2.4
- Messaging: NATS JetStream (jnats 2.20.1)
- Object Storage: MinIO 8.5.14 (S3 compatible)
- Other: Lombok, Jackson JSR310

**Architecture:** 標準 Spring 分層架構
- `Controller` → `Service`/`Producer`/`Consumer` → `Config`

## Folder Structure
```
src/main/java/com/ben/nat_jetstream_demo/
├── NatJetstreamDemoApplication.java    # Spring Boot 啟動類別
├── controller/                         # HTTP API 端點
│   ├── NotificationController.java      # 訊息發送 API
│   ├── ReplayController.java            # 訊息重播 API
│   └── ChaosController.java             # 混沌測試 API
├── service/                            # 商業邏輯
│   ├── ChaosService.java               # 混沌工程測試
│   └── ReplayService.java              # 訊息重播邏輯
├── producer/                           # 訊息發布
│   └── NotificationProducer.java
├── consumer/                           # 訊息消費
│   ├── MainConsumer.java               # 主 Consumer
│   └── DlqConsumer.java                # DLQ Consumer
├── config/                             # 配置類別
│   ├── NatsConfig.java                 # NATS 連線與 Stream 設定
│   ├── NatsJetStreamClient.java        # JetStream 客戶端封裝
│   ├── JetStreamClient.java            # 訊息發布/消費介面
│   ├── JetStreamConsumerRegistry.java  # Consumer 註冊管理
│   ├── ReliableMessageHandler.java     # 可靠訊息處理介面
│   └── I18nConfig.java                 # 國際化設定
├── model/                              # 資料模型
│   ├── NotificationMessage.java        # 通知訊息
│   ├── MessageEnvelope.java            # 訊息封裝 (含 metadata)
│   └── MessageMetadata.java            # 訊息元資料
└── exception/                          # 例外處理
    ├── AppException.java
    ├── ErrorResponse.java
    └── GlobalExceptionHandler.java
```

## Common Commands
```bash
# 啟動 NATS 服務 (需要先執行)
docker-compose up -d

# 啟動 Spring Boot 應用程式
./mvnw spring-boot:run

# 執行測試
./mvnw test

# 建構 JAR
./mvnw clean package
```

## Environment Setup
```bash
# 1. 啟動 NATS 服務
docker-compose up -d

# 2. 確認 NATS 已啟動
docker-compose ps

# 3. 啟動應用程式
./mvnw spring-boot:run
```

**Required environment:** (已在 `application.properties` 設定)
- `nats.url` — NATS 連線 URL (預設: nats://localhost:4222)
- `nats.subject` — 主題名稱 (預設: notifications.test)
- `server.port` — API 端口 (預設: 8081)

## Development Conventions

**Naming:**
- Classes: PascalCase (e.g., `NotificationProducer`)
- Methods: camelCase (e.g., `publishMessage`)
- Subjects/Streams: kebab-case (e.g., `notifications.test`)

**Code Style:**
- 使用 Lombok 簡化程式碼 (@Slf4j, @Service, @Value)
- 所有 NATS 操作透過 `JetStreamClient` 和 `ReliableMessageHandler` 介面
- Controller 只做參數驗證和轉發，不含商業邏輯

**Architecture Rules:**
- Controller → Service/Producer/Consumer → Config 單向依賴
- 訊息模型使用 `MessageEnvelope` 封裝攜帶 metadata

**Testing:**
- 整合測試優先 (`I18nIntegrationTest`)
- 測試目標：驗證 i18n 功能正常

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/notifications/send` | 發送通知訊息 |
| POST | `/api/notifications/quick-send` | 快速發送 (title, name, content) |
| POST | `/api/notifications/test-bulk?count=N` | 批量發送 N 條訊息 |
| POST | `/api/replay` | 重播指定時間範圍的訊息 |
| POST | `/api/chaos/toggle` | 開關混沌模式 |
| GET | `/api/chaos/status` | 查看混沌模式狀態 |

## Key Features
- **JetStream Consumer**: 支援重試 (maxDeliver) 和 Explicit ACK
- **DLQ (Dead Letter Queue)**: 消費失敗的訊息自動轉到 DLQ Stream
- **Message Replay**: 從指定時間範圍重播歷史訊息
- **Chaos Engineering**: 可控制性地模擬訊息處理失敗
- **i18n**: 支援英文和繁體中文
