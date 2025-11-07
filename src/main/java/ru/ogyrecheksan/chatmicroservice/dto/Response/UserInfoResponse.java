package ru.ogyrecheksan.chatmicroservice.dto.Response;

import lombok.Data;

@Data
public class UserInfoResponse {
    private Long id;
    private String username;
    private String email;
    private String profilePicture;
}
