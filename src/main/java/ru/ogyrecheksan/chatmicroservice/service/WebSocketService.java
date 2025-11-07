package ru.ogyrecheksan.chatmicroservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ru.ogyrecheksan.chatmicroservice.dto.Response.MessageResponse;
import ru.ogyrecheksan.chatmicroservice.dto.Response.WebSocketMessage;


@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendMessageToChat(Long chatId, MessageResponse message) {
        WebSocketMessage wsMessage = new WebSocketMessage();
        wsMessage.setType("NEW_MESSAGE");
        wsMessage.setPayload(message);
        wsMessage.setChatId(chatId);

        messagingTemplate.convertAndSend("/topic/chat." + chatId, wsMessage);
    }

    public void sendTypingIndicator(Long chatId, Long userId, boolean typing) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("TYPING_INDICATOR");
        message.setPayload(new TypingEvent(userId, typing));
        message.setChatId(chatId);

        messagingTemplate.convertAndSend("/topic/chat." + chatId, message);
    }

    public void sendUserJoined(Long chatId, Long userId) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("USER_JOINED");
        message.setPayload(new UserEvent(userId, "JOINED"));
        message.setChatId(chatId);

        messagingTemplate.convertAndSend("/topic/chat." + chatId, message);
    }

    // Вспомогательные классы для событий
    public static class TypingEvent {
        public Long userId;
        public boolean typing;

        public TypingEvent(Long userId, boolean typing) {
            this.userId = userId;
            this.typing = typing;
        }
    }

    public static class UserEvent {
        public Long userId;
        public String action;

        public UserEvent(Long userId, String action) {
            this.userId = userId;
            this.action = action;
        }
    }
}