package ru.ogyrecheksan.chatmicroservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ru.ogyrecheksan.chatmicroservice.dto.Response.MessageResponse;
import ru.ogyrecheksan.chatmicroservice.dto.Response.NotificationResponse;
import ru.ogyrecheksan.chatmicroservice.dto.Response.WebSocketMessage;
import ru.ogyrecheksan.chatmicroservice.model.Chat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Броадкаст нового сообщения в конкретный чат.
     * Отправляется в топик: /topic/chat/{chatId}
     *
     * type: MESSAGE
     * payload: MessageResponse
     */
    public void sendMessageToChat(Long chatId, MessageResponse message) {
        WebSocketMessage wsMessage = new WebSocketMessage();
        wsMessage.setType("MESSAGE");
        wsMessage.setPayload(message);
        wsMessage.setChatId(chatId);
        wsMessage.setTimestamp(System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/chat/" + chatId, wsMessage);
    }

    /**
     * Индикатор набора текста.
     *
     * type: TYPING
     * payload: { userId: UUID, typing: boolean }
     */
    public void sendTypingIndicator(Long chatId, UUID userId, Boolean typing) {
        WebSocketMessage wsMessage = new WebSocketMessage();
        wsMessage.setType("TYPING");

        TypingIndicator typingIndicator = new TypingIndicator();
        typingIndicator.setUserId(userId);
        typingIndicator.setTyping(typing);

        wsMessage.setPayload(typingIndicator);
        wsMessage.setChatId(chatId);
        wsMessage.setTimestamp(System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/chat/" + chatId, wsMessage);
    }

    /**
     * Персональное уведомление конкретному пользователю.
     *
     * Отправляется в: /user/{userId}/queue/notifications
     * Клиент подписывается на: /user/queue/notifications
     */
    public void sendNotificationToUser(UUID userId, NotificationResponse notification) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notification
        );
    }

    /**
     * Уведомления о новом сообщении всем участникам чата (кроме отправителя).
     *
     * type: NEW_MESSAGE + MESSAGE_DELIVERED
     * payload: NotificationResponse
     */
    public void sendNewMessageNotifications(Long chatId,
                                            UUID senderId,
                                            MessageResponse message,
                                            List<UUID> participantIds) {
        LocalDateTime timestamp = message.getCreatedAt();
        String content = message.getContent() != null ? message.getContent() : "";
        String preview = content.length() > 50 ? content.substring(0, 50) : content;

        for (UUID participantId : participantIds) {
            if (participantId.equals(senderId)) {
                continue; // не шлем уведомление самому себе
            }
            NotificationResponse notification = new NotificationResponse();
            notification.setType("NEW_MESSAGE");
            notification.setChatId(chatId);
            notification.setSenderId(senderId);
            notification.setPreview(preview);
            notification.setTimestamp(timestamp != null ? timestamp : LocalDateTime.now());

            sendNotificationToUser(participantId, notification);

            // событие о доставке для конкретного пользователя в топик чата
            WebSocketMessage delivered = new WebSocketMessage();
            delivered.setType("MESSAGE_DELIVERED");
            delivered.setChatId(chatId);
            delivered.setTimestamp(System.currentTimeMillis());

            DeliveryPayload deliveryPayload = new DeliveryPayload();
            deliveryPayload.setMessageId(Long.parseLong(message.getId()));
            deliveryPayload.setUserId(participantId);
            delivered.setPayload(deliveryPayload);

            messagingTemplate.convertAndSend("/topic/chat/" + chatId, delivered);
        }
    }

    /**
     * Уведомления о создании/подписке в новый чат.
     *
     * type: NEW_CHAT
     * payload: NotificationResponse
     */
    public void sendNewChatNotifications(Chat chat, UUID creatorId, Set<UUID> participantIds) {
        LocalDateTime timestamp = chat.getCreatedAt();
        String preview = chat.getName();

        for (UUID participantId : participantIds) {
            // создателю тоже может быть полезно уведомление (например, для синхронизации нескольких вкладок)
            NotificationResponse notification = new NotificationResponse();
            notification.setType("NEW_CHAT");
            notification.setChatId(chat.getId());
            notification.setSenderId(creatorId);
            notification.setPreview(preview);
            notification.setTimestamp(timestamp != null ? timestamp : LocalDateTime.now());

            sendNotificationToUser(participantId, notification);
        }
    }

    /**
     * Событие изменения онлайн-статуса участника чата.
     *
     * type: USER_ONLINE / USER_OFFLINE
     * payload: { userId: UUID, status: "ONLINE" | "OFFLINE" }
     */
    public void sendUserStatus(Long chatId, UUID userId, boolean online) {
        WebSocketMessage wsMessage = new WebSocketMessage();
        wsMessage.setType(online ? "USER_ONLINE" : "USER_OFFLINE");

        UserStatusPayload payload = new UserStatusPayload();
        payload.setUserId(userId);
        payload.setStatus(online ? "ONLINE" : "OFFLINE");

        wsMessage.setPayload(payload);
        wsMessage.setChatId(chatId);
        wsMessage.setTimestamp(System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/chat/" + chatId, wsMessage);
    }

    /**
     * Отправка истории сообщений в персональную очередь пользователя.
     *
     * type: HISTORY
     * payload: { chatId: Long, offset: int, limit: int, beforeMessageId: Long|null, messages: MessageResponse[] }
     */
    public void sendHistoryToUser(UUID userId,
                                  Long chatId,
                                  int offset,
                                  int limit,
                                  Long beforeMessageId,
                                  List<MessageResponse> messages) {
        HistoryPayload payload = new HistoryPayload();
        payload.setChatId(chatId);
        payload.setOffset(offset);
        payload.setLimit(limit);
        payload.setBeforeMessageId(beforeMessageId);
        payload.setMessages(messages);

        WebSocketMessage wsMessage = new WebSocketMessage();
        wsMessage.setType("HISTORY");
        wsMessage.setPayload(payload);
        wsMessage.setChatId(chatId);
        wsMessage.setTimestamp(System.currentTimeMillis());

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/history",
                wsMessage
        );
    }

    // Вспомогательные payload-классы для WebSocket событий
    public static class TypingIndicator {
        private UUID userId;
        private Boolean typing;

        public UUID getUserId() {
            return userId;
        }

        public void setUserId(UUID userId) {
            this.userId = userId;
        }

        public Boolean getTyping() {
            return typing;
        }

        public void setTyping(Boolean typing) {
            this.typing = typing;
        }
    }

    public static class UserStatusPayload {
        private UUID userId;
        private String status;

        public UUID getUserId() {
            return userId;
        }

        public void setUserId(UUID userId) {
            this.userId = userId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class HistoryPayload {
        private Long chatId;
        private int offset;
        private int limit;
        private Long beforeMessageId;
        private List<MessageResponse> messages;

        public Long getChatId() {
            return chatId;
        }

        public void setChatId(Long chatId) {
            this.chatId = chatId;
        }

        public int getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public Long getBeforeMessageId() {
            return beforeMessageId;
        }

        public void setBeforeMessageId(Long beforeMessageId) {
            this.beforeMessageId = beforeMessageId;
        }

        public List<MessageResponse> getMessages() {
            return messages;
        }

        public void setMessages(List<MessageResponse> messages) {
            this.messages = messages;
        }
    }

    public static class DeliveryPayload {
        private Long messageId;
        private UUID userId;

        public Long getMessageId() {
            return messageId;
        }

        public void setMessageId(Long messageId) {
            this.messageId = messageId;
        }

        public UUID getUserId() {
            return userId;
        }

        public void setUserId(UUID userId) {
            this.userId = userId;
        }
    }
}
