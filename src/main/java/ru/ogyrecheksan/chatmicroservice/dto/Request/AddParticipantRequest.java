package ru.ogyrecheksan.chatmicroservice.dto.Request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.ogyrecheksan.chatmicroservice.model.enums.ChatRole;

@Data
public class AddParticipantRequest {
    @NotNull
    private Long userId;

    private ChatRole role = ChatRole.MEMBER;
}
