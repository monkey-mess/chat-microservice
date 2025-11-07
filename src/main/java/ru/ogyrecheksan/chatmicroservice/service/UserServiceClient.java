package ru.ogyrecheksan.chatmicroservice.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.ogyrecheksan.chatmicroservice.dto.Response.UserInfoResponse;

import java.util.List;

@FeignClient(name = "user-service", url = "${auth.service.url}")
public interface UserServiceClient {

    @GetMapping("/api/users/{userId}")
    UserInfoResponse getUserById(@RequestHeader("Authorization") String token,
                                 @PathVariable Long userId);

    @PostMapping("/api/users/batch")
    List<UserInfoResponse> getUsersByIds(@RequestHeader("Authorization") String token,
                                         @RequestBody List<Long> userIds);

    @GetMapping("/api/users/profile")
    UserInfoResponse getCurrentUser(@RequestHeader("Authorization") String token);
}