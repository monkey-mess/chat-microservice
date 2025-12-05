package ru.ogyrecheksan.chatmicroservice.dto.Request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.ogyrecheksan.chatmicroservice.model.enums.ChatType;

import java.util.List;
import java.util.UUID;

@Data
public class CreateChatRequest {
    private String name;
    private String description;

    @NotNull
    private ChatType type;

    private List<UUID> participantIds;
}
