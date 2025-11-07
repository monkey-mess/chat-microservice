package ru.ogyrecheksan.chatmicroservice.dto.Response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import ru.ogyrecheksan.chatmicroservice.model.enums.ChatType;

@Data
public class ChatResponse {
    private Long id;
    private String name;
    private ChatType type;
    private UserInfoResponse createdBy;
    private List<ChatParticipantResponse> participants;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private MessageResponse lastMessage;
    private Integer unreadCount;
}
