GET /api/chats
Authorization: Bearer {accessToken}
Response: 200 OK
[
{
"id": "uuid",
"type": "private",
"name": "John Doe", // для приватных - имя собеседника
"description": null,
"avatarUrl": "https://minio.example.com/avatars/user1.jpg",
"lastMessage": {
"id": "uuid",
"content": "Hello there!",
"senderId": "uuid"
},
"participants": [
{
"userId": "uuid1",
"role": "member"
},
{
"userId": "uuid2",
"role": "member"
}
]
}
]

POST /api/chats
Authorization: Bearer {accessToken}
Content-Type: application/json
{
"type": "private",
"participantIds": ["uuid2"] // для приватного чата
}
// ИЛИ для группового
{
"type": "group",
"name": "Work Chat",
"description": "Team communication",
"participantIds": ["uuid2", "uuid3", "uuid4"]
}
Response: 201 Created
{
"id": "uuid",
"type": "private",
"name": "John Doe",
"participants": [...],
"createdAt": "2024-01-15T10:30:00Z"
}

GET /api/chats/{chatId}/messages
Authorization: Bearer {accessToken}
QueryParams: limit=50, offset=0
Response: 200 OK
[
{
"id": "uuid",
"conversationId": "uuid",
"senderId": "uuid",
"content": "Hello world!",
"createdAt": "2024-01-15T10:30:00Z"
}
]

POST /api/chats/{chatId}/messages
Authorization: Bearer {accessToken}
Content-Type: application/json
{
"content": "Hello everyone!"
}
Response: 201 Created
{
"id": "uuid",
"conversationId": "uuid",
"senderId": "uuid",
"content": "Hello everyone!",
"createdAt": "2024-01-15T10:30:00Z"
}

PUT /api/chats/{chatId}/participants
Authorization: Bearer {accessToken}
Content-Type: application/json
{
"action": "add",
"userId": "uuid3"
}
Response: 200 OK

POST /api/chats/{chatId}/avatar
Authorization: Bearer {accessToken}
Content-Type: multipart/form-data
File: chat-avatar.jpg
Response: 200 OK
{
"avatarUrl": "https://minio.example.com/chat-avatars/group1.jpg"
}

```

### Личный чат и получение chatId

```http
POST /api/chats/personal/{userId}
Authorization: Bearer {accessToken}
Response: 200 OK
{
  "id": 123,
  "type": "private",
  ...
}

GET /api/chats/personal/{userId}/id
Authorization: Bearer {accessToken}
Response: 200 OK
{
  "chatId": 123
}
// 404 Not Found — если личный чат еще не создан
```

### Онлайн-пользователи чата

```http
GET /api/chats/{chatId}/online
Authorization: Bearer {accessToken}
Response: 200 OK
{
  "chatId": 123,
  "onlineUserIds": ["uuid1", "uuid2"]
}
```

### Получение пропущенных сообщений

```http
GET /api/chats/{chatId}/missed-messages?since=2024-01-15T10:30:00Z
Authorization: Bearer {accessToken}
Response: 200 OK
[
  {
    "id": "uuid",
    "conversationId": "uuid",
    "senderId": "uuid",
    "content": "You missed this",
    "createdAt": "2024-01-15T10:35:00Z"
  }
]
```

## WebSocket / STOMP API

### Подключение

- **Endpoint**: `/ws-chat` (SockJS поддерживается)
- **Subprotocol**: STOMP
- **Авторизация**: передавать JWT как query-параметр или header (в зависимости от фронта; на бэке используется `Principal.getName()` как `userId`).

### Базовые destinations

- **Client → Server (`/app`)**
    - `/app/chat/{chatId}/sendMessage` — отправка сообщения
    - `/app/chat/{chatId}/typing` — индикатор набора текста
    - `/app/chat/{chatId}/loadHistory` — загрузка истории сообщений (WebSocket-пагинация)

- **Server → Client (`/topic`, `/user`)**
    - `/topic/chat/{chatId}` — события по чату (новые сообщения, статусы, typing и т.д.)
    - `/user/queue/notifications` — персональные уведомления (NEW_MESSAGE, NEW_CHAT)
    - `/user/queue/history` — ответы на запрос истории

### Форматы WebSocket-сообщений

Все события приходят в виде:

```json
{
  "type": "MESSAGE | TYPING | USER_ONLINE | USER_OFFLINE | HISTORY | MESSAGE_DELIVERED",
  "chatId": 123,
  "timestamp": 1710000000000,
  "payload": { ... }
}
```

#### MESSAGE

```json
{
  "type": "MESSAGE",
  "chatId": 123,
  "payload": {
    "id": "10",
    "conversationId": "123",
    "senderId": "uuid",
    "content": "Hello",
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

#### TYPING

```json
{
  "type": "TYPING",
  "chatId": 123,
  "payload": {
    "userId": "uuid",
    "typing": true
  }
}
```

#### USER_ONLINE / USER_OFFLINE

```json
{
  "type": "USER_ONLINE",
  "chatId": 123,
  "payload": {
    "userId": "uuid",
    "status": "ONLINE"
  }
}
```

#### HISTORY (ответ на `/app/chat/{chatId}/loadHistory`)

Клиент отправляет:

```json
// destination: /app/chat/{chatId}/loadHistory
{
  "offset": 0,
  "limit": 50,
  "beforeMessageId": null
}
```

Сервер шлет в `/user/queue/history`:

```json
{
  "type": "HISTORY",
  "chatId": 123,
  "payload": {
    "chatId": 123,
    "offset": 0,
    "limit": 50,
    "beforeMessageId": null,
    "messages": [ { ... MessageResponse ... } ]
  }
}
```

#### Персональные уведомления (NEW_MESSAGE / NEW_CHAT)

Очередь: `/user/queue/notifications`

```json
{
  "type": "NEW_MESSAGE",
  "chatId": 123,
  "senderId": "uuid-of-sender",
  "preview": "первые 50 символов сообщения",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

```json
{
  "type": "NEW_CHAT",
  "chatId": 456,
  "senderId": "uuid-of-creator",
  "preview": "Название чата",
  "timestamp": "2024-01-15T10:31:00Z"
}
```

#### MESSAGE_DELIVERED

Событие доставки сообщения конкретному пользователю (в топик чата):

```json
{
  "type": "MESSAGE_DELIVERED",
  "chatId": 123,
  "payload": {
    "messageId": 10,
    "userId": "uuid"
  }
}
```

### Как тестировать

#### REST

1. **Создать/получить токен** через `/auth/register` и `/auth/login`.
2. **Создать чат**: `POST /api/chats` или `POST /api/chats/personal/{userId}`.
3. **Получить список чатов**: `GET /api/chats`.
4. **Отправить сообщение**: `POST /api/chats/{chatId}/messages`.
5. **Забрать историю**: `GET /api/chats/{chatId}/messages?limit=50&offset=0`.
6. **Проверить личный чат**: `GET /api/chats/personal/{userId}/id`.
7. **Онлайн-статусы**: `GET /api/chats/{chatId}/online`.
8. **Пропущенные сообщения**: `GET /api/chats/{chatId}/missed-messages?since=...`.

#### WebSocket (например, через `webstomp-client` или браузерное расширение)

1. Подключиться к `ws://{host}:5252/ws-chat` (SockJS/отдельный STOMP-клиент).
2. После CONNECT:
    - Подписаться на `/topic/chat/{chatId}` для всех активных чатов.
    - Подписаться на `/user/queue/notifications` и `/user/queue/history`.
3. Отправка сообщений:
    - `SEND` в `/app/chat/{chatId}/sendMessage` с payload: `{ "content": "Hello", "type": "TEXT" }`.
4. Индикатор набора:
    - `SEND` в `/app/chat/{chatId}/typing` с payload: `true/false`.
5. Загрузка истории:
    - `SEND` в `/app/chat/{chatId}/loadHistory` с payload: `{ "offset": 0, "limit": 50 }` и ожидать `HISTORY` в `/user/queue/history`.
6. Проверка уведомлений:
    - Отправить сообщение из другого пользователя и убедиться, что текущий получает `NEW_MESSAGE` в `/user/queue/notifications` и `MESSAGE_DELIVERED` в `/topic/chat/{chatId}`.
7. Переподключение:
    - Разорвать соединение, затем подключиться снова, пересоздать подписки на `/topic/chat/{chatId}` и `/user/queue/notifications`, дернуть REST `/api/chats/{chatId}/missed-messages?since=lastSeenAt` для пропущенных сообщений.


Что сейчас реализовано:
REST‑API чатов
GET /api/chats – список чатов пользователя.
POST /api/chats – создание приватного/группового чата.
POST /api/chats/personal/{userId} – создать (или вернуть существующий) личный чат с пользователем.
GET /api/chats/personal/{userId}/id – получить chatId личного чата.
GET /api/chats/{chatId} – детали чата.
GET /api/chats/{chatId}/messages?limit=&offset= – история сообщений (REST).
POST /api/chats/{chatId}/messages – отправка сообщения.
PUT /api/chats/{chatId}/participants – добавить/удалить участника.
POST /api/chats/{chatId}/avatar – загрузка аватарки чата.
GET /api/chats/{chatId}/online – онлайн‑пользователи чата.
GET /api/chats/{chatId}/missed-messages?since=ISO_DATETIME – пропущенные сообщения за период.


WebSocket / STOMP
Endpoint: /ws-chat (SockJS, STOMP).
Префиксы: app (клиент → сервер), /topic и /queue (сервер → клиент), /user – персональные очереди.
Client → Server (/app):
/app/chat/{chatId}/sendMessage – отправка сообщения { content, type, replyToMessageId? }.
/app/chat/{chatId}/typing – { true | false }.
/app/chat/{chatId}/loadHistory – { offset, limit, beforeMessageId? }.


Server → Client:
/topic/chat/{chatId} – события по чату:
type: "MESSAGE" – новое сообщение.
type: "TYPING" – индикатор набора { userId, typing }.
type: "USER_ONLINE" | "USER_OFFLINE" – онлайн‑статусы { userId, status }.
type: "MESSAGE_DELIVERED" – событие доставки { messageId, userId }.
/user/queue/notifications – персональные уведомления:
type: "NEW_MESSAGE" – новое сообщение в любом из чатов.
type: "NEW_CHAT" – пользователь добавлен в новый чат.
Поля: chatId, senderId, preview (первые 50 символов/название), timestamp.
/user/queue/history – ответ на загрузку истории:
type: "HISTORY" + payload: { chatId, offset, limit, beforeMessageId, messages[] }.


Модель доставки/прочтения
В Message уже есть deliveredAt (агрегированно) и readAt.
Добавлена таблица message_read_status (MessageReadStatus):
message_id, user_id, delivered_at, read_at – статусы по каждому пользователю.
MessageService:
При выдаче истории помечает сообщения как доставленные для конкретного пользователя (и пишет в message_read_status).
При markMessagesAsRead(chatId, userId) помечает как прочитанные и обновляет таблицу статусов.


WebSocket:
При отправке нового сообщения:
Бродкаст MESSAGE в /topic/chat/{chatId}.
Персональные NEW_MESSAGE в /user/queue/notifications (всем участникам, кроме отправителя).
MESSAGE_DELIVERED в /topic/chat/{chatId} для каждого адресата.


Онлайн‑статусы и подписки
WebSocketSubscriptionRegistry:
Ведёт chatId → Set<userId> и sessionId → userId/chatIds.
Используется для отслеживания, кто онлайн в каком чате (используется в REST /api/chats/{chatId}/online, который ты можешь легко добавить, если нужно вывести онлайн‑лист, или уже добавлен в твоём коде).
WebSocketConfig:
Включен heartbeat setHeartbeatValue(new long[]{10000, 10000}) – помогает детектить "мертвые" соединения.
Подключён WebSocketSecurityInterceptor через configureClientInboundChannel.


Безопасность WebSocket
WebSocketSecurityInterceptor:
На SUBSCRIBE/SEND по адресам вида /topic/chat/{chatId} и /app/chat/{chatId}/...:
Достаёт chatId из destination.
Берёт userId из Principal.
Проверяет через ChatParticipantRepository.existsByChatIdAndUserIdAndLeftAtIsNull.
При отсутствии прав кидает AccessDeniedException и логирует попытку.


WebSocket‑история (пагинация)
Клиент шлёт в /app/chat/{chatId}/loadHistory:
{ "offset": 0, "limit": 50, "beforeMessageId": null }.
WebSocketController.loadHistory дергает MessageService.getChatMessages(...).
Ответ приходит в /user/queue/history как событие HISTORY с полями:
chatId, offset, limit, beforeMessageId, messages: MessageResponse[].


Автоподписка и переподключения
На стороне сервера:
Хранится информация о том, какие чаты у каких сессий (через WebSocketSubscriptionRegistry).
Сервер посылает уведомления NEW_CHAT, чтобы фронт мог автоматически подписывать пользователя на новые чаты.
Heartbeat настроен в WebSocketConfig.
На стороне клиента (ожидания):
При подключении:
REST: GET /api/chats → список чатов.
WebSocket: подписка на /topic/chat/{chatId} для всех чатов, /user/queue/notifications и /user/queue/history.
При переподключении:
Повторить шаги выше.
REST GET /api/chats/{chatId}/missed-messages?since={lastSeenAt} чтобы добрать пропущенные сообщения.


Как тестировать(Для Богдана):
REST (через Postman/curl)
Зарегистрировать/залогинить пользователя через /auth/register, /auth/login, получить accessToken.
Создать чат: POST /api/chats или POST /api/chats/personal/{userId}.
Проверить, что чат виден в GET /api/chats.
Отправить сообщения: POST /api/chats/{chatId}/messages.
Забрать историю: GET /api/chats/{chatId}/messages?limit=50&offset=0.
Проверить GET /api/chats/personal/{userId}/id — возвращается chatId.
В двух разных токенах (разные пользователи) вызвать GET /api/chats/{chatId}/online и убедиться, что оба отображаются как онлайн при активном WebSocket‑подключении.
Вызвать GET /api/chats/{chatId}/missed-messages?since=<timestamp> и убедиться, что приходят только сообщения после указанного времени.


WebSocket / STOMP
Подключиться к /ws-chat (через браузерный STOMP‑клиент, Insomnia, Postman или webstomp-client в фронте).
После CONNECT:
Подписаться на /topic/chat/{chatId} (для всех чатов пользователя).
Подписаться на /user/queue/notifications и /user/queue/history.
Отправить SEND в /app/chat/{chatId}/sendMessage с { "content": "Hello", "type": "TEXT" }:
Должно прийти событие MESSAGE в /topic/chat/{chatId}.
Всем другим участникам — NEW_MESSAGE в /user/queue/notifications и MESSAGE_DELIVERED в /topic/chat/{chatId}.
Отправить SEND в /app/chat/{chatId}/typing с true/false:
В /topic/chat/{chatId} придет TYPING с { userId, typing }.
Отправить SEND в /app/chat/{chatId}/loadHistory:
В /user/queue/history придет HISTORY с массивом сообщений.
Проверка безопасности:
Попробовать с токеном пользователя, не являющегося участником чата, подписаться на /topic/chat/{chatId} или отправить в /app/chat/{chatId}/sendMessage — должна прийти ошибка от STOMP (AccessDenied).
Проверка переподключения:
Отключить соединение, отправить несколько сообщений другим пользователем.
Подключиться снова, восстановить подписки и вызвать GET /api/chats/{chatId}/missed-messages?since=<timestamp_разрыва> — должны прийти пропущенные сообщения.