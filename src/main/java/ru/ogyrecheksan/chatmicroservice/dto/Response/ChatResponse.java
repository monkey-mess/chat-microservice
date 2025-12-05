package ru.ogyrecheksan.chatmicroservice.dto.Response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ChatResponse {
    private Long id;
    private String type;
    private String name;
    private String description;
    private String avatarUrl;
    private LastMessage lastMessage;
    private List<Participant> participants;
    private LocalDateTime createdAt;

    @Data
    public static class LastMessage {
        private String id;
        private String content;
        private String senderId;
    }

    @Data
    public static class Participant {
        private UUID userId;
        private String role;
    }
}
