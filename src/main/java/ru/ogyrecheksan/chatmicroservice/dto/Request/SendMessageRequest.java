package ru.ogyrecheksan.chatmicroservice.dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.ogyrecheksan.chatmicroservice.model.enums.MessageType;

@Data
public class SendMessageRequest {
    @NotNull
    private Long chatId;

    @NotBlank
    private String content;

    private MessageType type = MessageType.TEXT;
    private Long replyToMessageId;
}
