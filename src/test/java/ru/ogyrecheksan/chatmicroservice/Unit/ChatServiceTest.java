package ru.ogyrecheksan.chatmicroservice.Unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ogyrecheksan.chatmicroservice.dto.Request.CreateChatRequest;
import ru.ogyrecheksan.chatmicroservice.dto.Response.ChatResponse;
import ru.ogyrecheksan.chatmicroservice.dto.Response.UserInfoResponse;
import ru.ogyrecheksan.chatmicroservice.model.Chat;
import ru.ogyrecheksan.chatmicroservice.model.ChatParticipant;
import ru.ogyrecheksan.chatmicroservice.model.enums.ChatRole;
import ru.ogyrecheksan.chatmicroservice.model.enums.ChatType;
import ru.ogyrecheksan.chatmicroservice.repository.ChatParticipantRepository;
import ru.ogyrecheksan.chatmicroservice.repository.ChatRepository;
import ru.ogyrecheksan.chatmicroservice.service.ChatService;
import ru.ogyrecheksan.chatmicroservice.service.MessageService;
import ru.ogyrecheksan.chatmicroservice.service.UserServiceClient;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ChatParticipantRepository participantRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private ChatService chatService;

    private Chat testChat;
    private UUID testUserId1;
    private UUID testUserId2;
    private String testAuthToken;

    @BeforeEach
    void setUp() {
        testUserId1 = UUID.randomUUID();
        testUserId2 = UUID.randomUUID();
        testAuthToken = "Bearer test-token";

        testChat = new Chat();
        testChat.setId(1L);
        testChat.setName("Test Chat");
        testChat.setType(ChatType.GROUP);
        testChat.setCreatedBy(testUserId1);
    }

    @Test
    void testCreatePersonalChat_WhenChatNotExists_ShouldCreateNewChat() {
        // Arrange
        when(chatRepository.findPersonalChat(testUserId1, testUserId2))
                .thenReturn(Optional.empty());
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> {
            Chat chat = invocation.getArgument(0);
            chat.setId(1L);
            return chat;
        });
        when(participantRepository.findActiveParticipantIds(anyLong())).thenReturn(Arrays.asList(testUserId1, testUserId2));
        when(userServiceClient.getUserById(anyString(), any(UUID.class))).thenReturn(createUserInfo(testUserId1));
        when(userServiceClient.getUsersByIds(anyString(), anyList())).thenReturn(
                Arrays.asList(createUserInfo(testUserId1), createUserInfo(testUserId2))
        );
        when(participantRepository.findByChatIdAndUserId(anyLong(), any(UUID.class))).thenReturn(Optional.of(createParticipant()));
        when(chatRepository.countUnreadMessages(anyLong(), any(UUID.class))).thenReturn(0);

        // Act
        ChatResponse response = chatService.createPersonalChat(testUserId1, testUserId2, testAuthToken);

        // Assert
        assertNotNull(response);
        verify(chatRepository, times(1)).save(any(Chat.class));
        verify(participantRepository, times(2)).save(any(ChatParticipant.class));
    }

    @Test
    void testCreatePersonalChat_WhenChatExists_ShouldReturnExistingChat() {
        // Arrange
        when(chatRepository.findPersonalChat(testUserId1, testUserId2))
                .thenReturn(Optional.of(testChat));
        when(participantRepository.findActiveParticipantIds(anyLong())).thenReturn(Arrays.asList(testUserId1, testUserId2));
        when(userServiceClient.getUserById(anyString(), any(UUID.class))).thenReturn(createUserInfo(testUserId1));
        when(userServiceClient.getUsersByIds(anyString(), anyList())).thenReturn(
                Arrays.asList(createUserInfo(testUserId1), createUserInfo(testUserId2))
        );
        when(participantRepository.findByChatIdAndUserId(anyLong(), any(UUID.class))).thenReturn(Optional.of(createParticipant()));
        when(chatRepository.countUnreadMessages(anyLong(), any(UUID.class))).thenReturn(0);

        // Act
        ChatResponse response = chatService.createPersonalChat(testUserId1, testUserId2, testAuthToken);

        // Assert
        assertNotNull(response);
        verify(chatRepository, never()).save(any(Chat.class));
    }

    @Test
    void testCreateGroupChat_Success() {
        // Arrange
        CreateChatRequest request = new CreateChatRequest();
        request.setName("Test Group");
        request.setType(ChatType.GROUP);
        request.setParticipantIds(Arrays.asList(testUserId2));

        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> {
            Chat chat = invocation.getArgument(0);
            chat.setId(1L);
            return chat;
        });
        when(participantRepository.findActiveParticipantIds(anyLong())).thenReturn(Arrays.asList(testUserId1, testUserId2));
        when(userServiceClient.getUserById(anyString(), any(UUID.class))).thenReturn(createUserInfo(testUserId1));
        when(userServiceClient.getUsersByIds(anyString(), anyList())).thenReturn(
                Arrays.asList(createUserInfo(testUserId1), createUserInfo(testUserId2))
        );
        when(participantRepository.findByChatIdAndUserId(anyLong(), any(UUID.class))).thenReturn(Optional.of(createParticipant()));
        when(chatRepository.countUnreadMessages(anyLong(), any(UUID.class))).thenReturn(0);

        // Act
        ChatResponse response = chatService.createGroupChat(request, testUserId1, testAuthToken);

        // Assert
        assertNotNull(response);
        assertEquals("Test Group", response.getName());
        assertEquals(ChatType.GROUP, response.getType());
        verify(chatRepository, times(1)).save(any(Chat.class));
        verify(participantRepository, times(2)).save(any(ChatParticipant.class)); // creator + 1 participant
    }

    @Test
    void testGetUserChats_Success() {
        // Arrange
        List<Chat> chats = Arrays.asList(testChat);
        when(chatRepository.findUserChats(testUserId1)).thenReturn(chats);
        when(participantRepository.findActiveParticipantIds(anyLong())).thenReturn(Arrays.asList(testUserId1));
        when(userServiceClient.getUserById(anyString(), any(UUID.class))).thenReturn(createUserInfo(testUserId1));
        when(userServiceClient.getUsersByIds(anyString(), anyList())).thenReturn(Arrays.asList(createUserInfo(testUserId1)));
        when(participantRepository.findByChatIdAndUserId(anyLong(), any(UUID.class))).thenReturn(Optional.of(createParticipant()));
        when(chatRepository.countUnreadMessages(anyLong(), any(UUID.class))).thenReturn(0);

        // Act
        List<ChatResponse> responses = chatService.getUserChats(testUserId1, testAuthToken);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        verify(chatRepository, times(1)).findUserChats(testUserId1);
    }

    @Test
    void testGetChat_Success() {
        // Arrange
        when(chatRepository.findById(1L)).thenReturn(Optional.of(testChat));
        when(participantRepository.existsByChatIdAndUserIdAndLeftAtIsNull(1L, testUserId1)).thenReturn(true);
        when(participantRepository.findActiveParticipantIds(anyLong())).thenReturn(Arrays.asList(testUserId1));
        when(userServiceClient.getUserById(anyString(), any(UUID.class))).thenReturn(createUserInfo(testUserId1));
        when(userServiceClient.getUsersByIds(anyString(), anyList())).thenReturn(Arrays.asList(createUserInfo(testUserId1)));
        when(participantRepository.findByChatIdAndUserId(anyLong(), any(UUID.class))).thenReturn(Optional.of(createParticipant()));
        when(chatRepository.countUnreadMessages(anyLong(), any(UUID.class))).thenReturn(0);

        // Act
        ChatResponse response = chatService.getChat(1L, testUserId1, testAuthToken);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        verify(chatRepository, times(1)).findById(1L);
    }

    @Test
    void testGetChat_ChatNotFound_ShouldThrowException() {
        // Arrange
        when(chatRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            chatService.getChat(1L, testUserId1, testAuthToken);
        });
    }

    @Test
    void testGetChat_AccessDenied_ShouldThrowException() {
        // Arrange
        when(chatRepository.findById(1L)).thenReturn(Optional.of(testChat));
        when(participantRepository.existsByChatIdAndUserIdAndLeftAtIsNull(1L, testUserId1)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            chatService.getChat(1L, testUserId1, testAuthToken);
        });
    }

    private UserInfoResponse createUserInfo(UUID userId) {
        UserInfoResponse userInfo = new UserInfoResponse();
        userInfo.setId(userId);
        userInfo.setUsername("user" + userId);
        userInfo.setEmail("user" + userId + "@example.com");
        return userInfo;
    }

    private ChatParticipant createParticipant() {
        ChatParticipant participant = new ChatParticipant();
        participant.setId(1L);
        participant.setUserId(testUserId1);
        participant.setRole(ChatRole.OWNER);
        participant.setChat(testChat);
        return participant;
    }
}