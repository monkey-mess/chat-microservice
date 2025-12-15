package ru.ogyrecheksan.chatmicroservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import ru.ogyrecheksan.chatmicroservice.dto.Request.SendMessageRequest;
import ru.ogyrecheksan.chatmicroservice.dto.Response.MessageResponse;
import ru.ogyrecheksan.chatmicroservice.service.MessageService;
import ru.ogyrecheksan.chatmicroservice.service.WebSocketService;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final MessageService messageService;
    private final WebSocketService webSocketService;

    @MessageMapping("/chat/{chatId}/sendMessage")
    public void sendMessage(
            @DestinationVariable Long chatId,
            @Payload SendMessageRequest request,
            Principal principal) {

        UUID userId = extractUserIdFromPrincipal(principal);
        request.setChatId(chatId);
        messageService.sendMessage(request, userId, null); // authToken = null для WebSocket
    }

    @MessageMapping("/chat/{chatId}/typing")
    public void handleTyping(
            @DestinationVariable Long chatId,
            @Payload Boolean typing,
            Principal principal) {

        UUID userId = extractUserIdFromPrincipal(principal);
        webSocketService.sendTypingIndicator(chatId, userId, typing);
    }

    /**
     * Загрузка истории сообщений через WebSocket.
     *
     * Клиент отправляет:
     *  destination: /app/chat/{chatId}/loadHistory
     *  payload: { "offset": 0, "limit": 50, "beforeMessageId": null }
     *
     * Ответ придет в персональную очередь пользователя:
     *  /user/queue/history
     */
    @MessageMapping("/chat/{chatId}/loadHistory")
    public void loadHistory(
            @DestinationVariable Long chatId,
            @Payload LoadHistoryRequest request,
            Principal principal) {

        UUID userId = extractUserIdFromPrincipal(principal);
        int offset = request.getOffset() != null ? request.getOffset() : 0;
        int limit = request.getLimit() != null ? request.getLimit() : 50;

        var page = messageService.getChatMessages(chatId, userId, offset, limit, null);
        List<MessageResponse> messages = page.getContent();

        webSocketService.sendHistoryToUser(
                userId,
                chatId,
                offset,
                limit,
                request.getBeforeMessageId(),
                messages
        );
    }

    private UUID extractUserIdFromPrincipal(Principal principal) {
        if (principal != null && principal.getName() != null) {
            try {
                return UUID.fromString(principal.getName());
            } catch (IllegalArgumentException e) {
                return UUID.nameUUIDFromBytes(principal.getName().getBytes());
            }
        }
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    // DTO для запроса истории
    public static class LoadHistoryRequest {
        private Integer offset;
        private Integer limit;
        private Long beforeMessageId;

        public Integer getOffset() {
            return offset;
        }

        public void setOffset(Integer offset) {
            this.offset = offset;
        }

        public Integer getLimit() {
            return limit;
        }

        public void setLimit(Integer limit) {
            this.limit = limit;
        }

        public Long getBeforeMessageId() {
            return beforeMessageId;
        }

        public void setBeforeMessageId(Long beforeMessageId) {
            this.beforeMessageId = beforeMessageId;
        }
    }
}