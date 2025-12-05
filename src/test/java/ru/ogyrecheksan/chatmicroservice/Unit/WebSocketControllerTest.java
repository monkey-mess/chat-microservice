package ru.ogyrecheksan.chatmicroservice.Unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ogyrecheksan.chatmicroservice.controller.WebSocketController;
import ru.ogyrecheksan.chatmicroservice.dto.Request.SendMessageRequest;
import ru.ogyrecheksan.chatmicroservice.dto.Response.MessageResponse;
import ru.ogyrecheksan.chatmicroservice.service.MessageService;
import ru.ogyrecheksan.chatmicroservice.service.WebSocketService;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketControllerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private WebSocketController webSocketController;

    private Principal principal;
    private SendMessageRequest sendMessageRequest;
    private UUID userId;
    private MessageResponse messageResponse;

    @BeforeEach
    void setUp() {
        principal = () -> "test@example.com";
        userId = UUID.nameUUIDFromBytes("test@example.com".getBytes());

        sendMessageRequest = new SendMessageRequest();
        sendMessageRequest.setContent("Hello WebSocket");
        sendMessageRequest.setType(ru.ogyrecheksan.chatmicroservice.model.enums.MessageType.TEXT);

        messageResponse = new MessageResponse();
        messageResponse.setId("1");
        messageResponse.setConversationId("1");
        messageResponse.setSenderId(userId.toString());
        messageResponse.setContent("Hello WebSocket");
        messageResponse.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void sendMessage_WithValidRequest_ShouldProcessMessage() {
        // Arrange
        Long chatId = 1L;
        when(messageService.sendMessage(any(SendMessageRequest.class), any(UUID.class), isNull()))
                .thenReturn(messageResponse);

        // Act
        webSocketController.sendMessage(chatId, sendMessageRequest, principal);

        // Assert
        verify(messageService).sendMessage(any(SendMessageRequest.class), eq(userId), isNull());
        verify(messageService, times(1)).sendMessage(any(SendMessageRequest.class), any(UUID.class), isNull());
    }

    @Test
    void sendMessage_ShouldSetChatIdInRequest() {
        // Arrange
        Long chatId = 5L;
        when(messageService.sendMessage(any(SendMessageRequest.class), any(UUID.class), isNull()))
                .thenReturn(messageResponse);

        // Act
        webSocketController.sendMessage(chatId, sendMessageRequest, principal);

        // Assert
        verify(messageService).sendMessage(argThat(request ->
                request.getChatId().equals(5L) && "Hello WebSocket".equals(request.getContent())
        ), eq(userId), isNull());
    }

    @Test
    void handleTyping_WithTrue_ShouldSendTypingIndicator() {
        // Arrange
        Long chatId = 1L;
        Boolean typing = true;

        // Act
        webSocketController.handleTyping(chatId, typing, principal);

        // Assert
        verify(webSocketService).sendTypingIndicator(chatId, userId, typing);
        verify(webSocketService, times(1)).sendTypingIndicator(anyLong(), any(UUID.class), anyBoolean());
    }

    @Test
    void handleTyping_WithFalse_ShouldSendStopTypingIndicator() {
        // Arrange
        Long chatId = 1L;
        Boolean typing = false;

        // Act
        webSocketController.handleTyping(chatId, typing, principal);

        // Assert
        verify(webSocketService).sendTypingIndicator(chatId, userId, typing);
    }

    @Test
    void handleTyping_WithDifferentChatId_ShouldSendToCorrectChat() {
        // Arrange
        Long differentChatId = 99L;
        Boolean typing = true;

        // Act
        webSocketController.handleTyping(differentChatId, typing, principal);

        // Assert
        verify(webSocketService).sendTypingIndicator(differentChatId, userId, typing);
        verify(webSocketService, never()).sendTypingIndicator(eq(1L), any(UUID.class), anyBoolean());
    }

    @Test
    void handleTyping_WithNullPrincipal_ShouldHandleGracefully() {
        // Arrange
        Long chatId = 1L;
        Boolean typing = true;

        // Act
        webSocketController.handleTyping(chatId, typing, null);

        // Assert
        // Should use fallback UUID when principal is null
        verify(webSocketService).sendTypingIndicator(eq(chatId), any(UUID.class), eq(typing));
    }
}