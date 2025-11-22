package ru.ogyrecheksan.chatmicroservice.Unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ru.ogyrecheksan.chatmicroservice.config.TestSecurityConfig;
import ru.ogyrecheksan.chatmicroservice.controller.ChatController;
import ru.ogyrecheksan.chatmicroservice.dto.Request.CreateChatRequest;
import ru.ogyrecheksan.chatmicroservice.dto.Response.ChatResponse;
import ru.ogyrecheksan.chatmicroservice.dto.Response.MessageResponse;
import ru.ogyrecheksan.chatmicroservice.dto.Response.UserInfoResponse;
import ru.ogyrecheksan.chatmicroservice.model.enums.ChatType;
import ru.ogyrecheksan.chatmicroservice.model.enums.MessageType;
import ru.ogyrecheksan.chatmicroservice.service.ChatService;
import ru.ogyrecheksan.chatmicroservice.service.MessageService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@Import(TestSecurityConfig.class)
@WithMockUser(username = "test@example.com")
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @MockBean
    private MessageService messageService;

    private String validToken;
    private String userEmail;
    private UUID userId;
    private ChatResponse chatResponse;
    private MessageResponse messageResponse;
    private UserInfoResponse userInfo;

    @BeforeEach
    void setUp() {
        validToken = "Bearer valid-jwt-token";
        userEmail = "test@example.com";
        userId = UUID.nameUUIDFromBytes(userEmail.getBytes());

        // Настройка SecurityContext
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(userEmail);

        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        userInfo = new UserInfoResponse();
        userInfo.setId(userId);
        userInfo.setEmail(userEmail);
        userInfo.setUsername("testuser");

        chatResponse = new ChatResponse();
        chatResponse.setId(1L);
        chatResponse.setName("Test Chat");
        chatResponse.setType(ChatType.GROUP);
        chatResponse.setCreatedBy(userInfo);
        chatResponse.setCreatedAt(LocalDateTime.now());
        chatResponse.setUpdatedAt(LocalDateTime.now());
        chatResponse.setUnreadCount(0);

        messageResponse = new MessageResponse();
        messageResponse.setId(1L);
        messageResponse.setContent("Test message");
        messageResponse.setType(MessageType.TEXT);
        messageResponse.setSender(userInfo);
        messageResponse.setSentAt(LocalDateTime.now());
    }

    @Test
    void getUserChats_WithValidToken_ShouldReturnChatsList() throws Exception {
        // Arrange
        List<ChatResponse> chats = Arrays.asList(chatResponse);
        when(chatService.getUserChats(eq(userId), anyString())).thenReturn(chats);

        // Act & Assert
        mockMvc.perform(get("/api/chats")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Test Chat"))
                .andExpect(jsonPath("$[0].type").value("GROUP"))
                .andExpect(jsonPath("$[0].unreadCount").value(0));

        // Verify
        Mockito.verify(chatService).getUserChats(userId, validToken);
    }

    @Test
    void createChat_WithValidRequest_ShouldReturnCreatedChat() throws Exception {
        // Arrange
        UUID participantId = UUID.randomUUID();
        CreateChatRequest createRequest = new CreateChatRequest();
        createRequest.setName("New Group Chat");
        createRequest.setType(ChatType.GROUP);
        createRequest.setParticipantIds(Arrays.asList(participantId));

        ChatResponse createdChat = new ChatResponse();
        createdChat.setId(2L);
        createdChat.setName("New Group Chat");
        createdChat.setType(ChatType.GROUP);
        createdChat.setCreatedBy(userInfo);
        createdChat.setCreatedAt(LocalDateTime.now());

        when(chatService.createGroupChat(any(CreateChatRequest.class), eq(userId), anyString()))
                .thenReturn(createdChat);

        // Act & Assert
        mockMvc.perform(post("/api/chats")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.name").value("New Group Chat"))
                .andExpect(jsonPath("$.type").value("GROUP"));

        // Verify
        Mockito.verify(chatService).createGroupChat(any(CreateChatRequest.class), eq(userId), eq(validToken));
    }

    @Test
    void createChat_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange - создаем невалидный запрос (без имени)
        CreateChatRequest invalidRequest = new CreateChatRequest();
        invalidRequest.setType(ChatType.GROUP);
        // name не установлено - должно вызвать ошибку валидации

        // Act & Assert
        mockMvc.perform(post("/api/chats")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createChat_WithoutType_ShouldReturnBadRequest() throws Exception {
        // Arrange - создаем невалидный запрос (без типа)
        CreateChatRequest invalidRequest = new CreateChatRequest();
        invalidRequest.setName("Test Chat");
        // type не установлено - должно вызвать ошибку валидации

        // Act & Assert
        mockMvc.perform(post("/api/chats")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPersonalChat_WithValidUserId_ShouldReturnPersonalChat() throws Exception {
        // Arrange
        UUID targetUserId = UUID.randomUUID();
        ChatResponse personalChat = new ChatResponse();
        personalChat.setId(3L);
        personalChat.setType(ChatType.PERSONAL);
        personalChat.setCreatedAt(LocalDateTime.now());
        personalChat.setCreatedBy(userInfo);

        when(chatService.createPersonalChat(eq(userId), eq(targetUserId), anyString()))
                .thenReturn(personalChat);

        // Act & Assert
        mockMvc.perform(post("/api/chats/personal/{userId}", targetUserId)
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.type").value("PERSONAL"));

        // Verify
        Mockito.verify(chatService).createPersonalChat(userId, targetUserId, validToken);
    }

    @Test
    void getChat_WithValidChatId_ShouldReturnChat() throws Exception {
        // Arrange
        Long chatId = 1L;
        when(chatService.getChat(eq(chatId), eq(userId), anyString())).thenReturn(chatResponse);

        // Act & Assert
        mockMvc.perform(get("/api/chats/{chatId}", chatId)
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Test Chat"))
                .andExpect(jsonPath("$.createdBy.id").value(userId.toString()))
                .andExpect(jsonPath("$.createdBy.email").value(userEmail));

        // Verify
        Mockito.verify(chatService).getChat(chatId, userId, validToken);
    }

    @Test
    void getChat_WithNonExistentChatId_ShouldReturnNotFound() throws Exception {
        // Arrange
        Long nonExistentChatId = 999L;
        when(chatService.getChat(eq(nonExistentChatId), eq(userId), anyString()))
                .thenThrow(new RuntimeException("Chat not found"));

        // Act & Assert
        mockMvc.perform(get("/api/chats/{chatId}", nonExistentChatId)
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        // Verify
        Mockito.verify(chatService).getChat(nonExistentChatId, userId, validToken);
    }

    @Test
    void getChatMessages_WithValidParameters_ShouldReturnPagedMessages() throws Exception {
        // Arrange
        Long chatId = 1L;
        int page = 0;
        int size = 20;

        List<MessageResponse> messages = Arrays.asList(messageResponse);
        Page<MessageResponse> messagePage = new PageImpl<>(messages, PageRequest.of(page, size), messages.size());

        when(messageService.getChatMessages(eq(chatId), eq(userId), eq(page), eq(size), anyString()))
                .thenReturn(messagePage);

        // Act & Assert
        mockMvc.perform(get("/api/chats/{chatId}/messages", chatId)
                        .header("Authorization", validToken)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].content").value("Test message"))
                .andExpect(jsonPath("$.content[0].type").value("TEXT"))
                .andExpect(jsonPath("$.content[0].sender.id").value(userId.toString()))
                .andExpect(jsonPath("$.content[0].sender.email").value(userEmail));

        // Verify
        Mockito.verify(messageService).getChatMessages(chatId, userId, page, size, validToken);
    }

    @Test
    void getChatMessages_WithDefaultPagination_ShouldUseDefaultValues() throws Exception {
        // Arrange
        Long chatId = 1L;
        List<MessageResponse> messages = Arrays.asList(messageResponse);
        Page<MessageResponse> messagePage = new PageImpl<>(messages, PageRequest.of(0, 50), messages.size());

        when(messageService.getChatMessages(eq(chatId), eq(userId), eq(0), eq(50), anyString()))
                .thenReturn(messagePage);

        // Act & Assert
        mockMvc.perform(get("/api/chats/{chatId}/messages", chatId)
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));

        // Verify
        Mockito.verify(messageService).getChatMessages(chatId, userId, 0, 50, validToken);
    }

    @Test
    void getChatMessages_WithInvalidPagination_ShouldHandleGracefully() throws Exception {
        // Arrange
        Long chatId = 1L;
        int invalidPage = -1;
        int invalidSize = 1000;

        Page<MessageResponse> emptyPage = new PageImpl<>(Collections.emptyList());
        when(messageService.getChatMessages(eq(chatId), eq(userId), eq(invalidPage), eq(invalidSize), anyString()))
                .thenReturn(emptyPage);

        // Act & Assert
        mockMvc.perform(get("/api/chats/{chatId}/messages", chatId)
                        .header("Authorization", validToken)
                        .param("page", String.valueOf(invalidPage))
                        .param("size", String.valueOf(invalidSize))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));

        // Verify
        Mockito.verify(messageService).getChatMessages(chatId, userId, invalidPage, invalidSize, validToken);
    }

    @Test
    void createChat_WithPersonalType_ShouldCreatePersonalChat() throws Exception {
        // Arrange
        UUID participantId = UUID.randomUUID();
        CreateChatRequest personalRequest = new CreateChatRequest();
        personalRequest.setName("Personal Chat");
        personalRequest.setType(ChatType.PERSONAL);
        personalRequest.setParticipantIds(Arrays.asList(participantId));

        ChatResponse personalChat = new ChatResponse();
        personalChat.setId(4L);
        personalChat.setName("Personal Chat");
        personalChat.setType(ChatType.PERSONAL);
        personalChat.setCreatedBy(userInfo);

        when(chatService.createGroupChat(any(CreateChatRequest.class), eq(userId), anyString()))
                .thenReturn(personalChat);

        // Act & Assert
        mockMvc.perform(post("/api/chats")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(personalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4L))
                .andExpect(jsonPath("$.type").value("PERSONAL"));

        // Verify
        Mockito.verify(chatService).createGroupChat(any(CreateChatRequest.class), eq(userId), eq(validToken));
    }
}