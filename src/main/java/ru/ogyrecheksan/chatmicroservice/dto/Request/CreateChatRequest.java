package ru.ogyrecheksan.chatmicroservice.dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.ogyrecheksan.chatmicroservice.model.enums.ChatType;

import java.util.List;

@Data
public class CreateChatRequest {
    @NotBlank
    private String name;

    @NotNull
    private ChatType type;

    private List<Long> participantIds;
}
