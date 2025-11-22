package ru.ogyrecheksan.chatmicroservice.dto.Response;

import lombok.Data;
import ru.ogyrecheksan.chatmicroservice.model.enums.ChatRole;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ChatParticipantResponse {
    private Long id;
    private UserInfoResponse user;
    private ChatRole role;
    private LocalDateTime joinedAt;
}