package ru.ogyrecheksan.chatmicroservice.dto.Response;

import lombok.Data;

@Data
public class WebSocketMessage {
    private String type; // MESSAGE, TYPING, USER_JOINED, etc.
    private Object payload;
    private Long chatId;
    private Long timestamp = System.currentTimeMillis();
}
