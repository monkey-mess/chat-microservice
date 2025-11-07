package ru.ogyrecheksan.chatmicroservice.dto.Response;

import lombok.Data;
import ru.ogyrecheksan.chatmicroservice.model.enums.MessageType;

import java.time.LocalDateTime;

@Data
public class MessageResponse {
    private Long id;
    private String content;
    private MessageType type;
    private UserInfoResponse sender;
    private MessageResponse replyTo;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
}
