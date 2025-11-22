package ru.ogyrecheksan.chatmicroservice.dto.Request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.ogyrecheksan.chatmicroservice.model.enums.ChatRole;

import java.util.UUID;

@Data
public class AddParticipantRequest {
    @NotNull
    private UUID userId;

    private ChatRole role = ChatRole.MEMBER;
}
