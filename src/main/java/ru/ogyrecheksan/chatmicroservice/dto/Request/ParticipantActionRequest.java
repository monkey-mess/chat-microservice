package ru.ogyrecheksan.chatmicroservice.dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ParticipantActionRequest {
    @NotBlank
    private String action; // add/remove

    @NotNull
    private UUID userId;
}



