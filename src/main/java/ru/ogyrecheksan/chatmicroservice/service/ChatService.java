package ru.ogyrecheksan.chatmicroservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ogyrecheksan.chatmicroservice.dto.Request.CreateChatRequest;
import ru.ogyrecheksan.chatmicroservice.dto.Response.ChatParticipantResponse;
import ru.ogyrecheksan.chatmicroservice.dto.Response.ChatResponse;
import ru.ogyrecheksan.chatmicroservice.dto.Response.UserInfoResponse;
import ru.ogyrecheksan.chatmicroservice.exception.AccessDeniedException;
import ru.ogyrecheksan.chatmicroservice.exception.ChatNotFoundException;
import ru.ogyrecheksan.chatmicroservice.model.Chat;
import ru.ogyrecheksan.chatmicroservice.model.ChatParticipant;
import ru.ogyrecheksan.chatmicroservice.model.enums.ChatRole;
import ru.ogyrecheksan.chatmicroservice.model.enums.ChatType;
import ru.ogyrecheksan.chatmicroservice.repository.ChatParticipantRepository;
import ru.ogyrecheksan.chatmicroservice.repository.ChatRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatParticipantRepository participantRepository;
    private final UserServiceClient userServiceClient;
    private final MessageService messageService;

    public ChatResponse createPersonalChat(UUID user1Id, UUID user2Id, String authToken) {
        // Проверяем, не существует ли уже личный чат
        var existingChat = chatRepository.findPersonalChat(user1Id, user2Id);
        if (existingChat.isPresent()) {
            return convertToResponse(existingChat.get(), user1Id, authToken);
        }

        // Создаем новый чат
        Chat chat = new Chat();
        chat.setName("Personal Chat");
        chat.setType(ChatType.PERSONAL);
        chat.setCreatedBy(user1Id);

        Chat savedChat = chatRepository.save(chat);

        // Добавляем участников
        addParticipant(savedChat, user1Id, ChatRole.OWNER);
        addParticipant(savedChat, user2Id, ChatRole.MEMBER);

        return convertToResponse(savedChat, user1Id, authToken);
    }

    public ChatResponse createGroupChat(CreateChatRequest request, UUID creatorId, String authToken) {
        Chat chat = new Chat();
        chat.setName(request.getName());
        chat.setType(request.getType());
        chat.setCreatedBy(creatorId);

        Chat savedChat = chatRepository.save(chat);

        // Добавляем создателя как владельца
        addParticipant(savedChat, creatorId, ChatRole.OWNER);

        // Добавляем других участников
        if (request.getParticipantIds() != null) {
            for (UUID participantId : request.getParticipantIds()) {
                if (!participantId.equals(creatorId)) {
                    addParticipant(savedChat, participantId, ChatRole.MEMBER);
                }
            }
        }

        return convertToResponse(savedChat, creatorId, authToken);
    }

    public List<ChatResponse> getUserChats(UUID userId, String authToken) {
        List<Chat> chats = chatRepository.findUserChats(userId);
        return chats.stream()
                .map(chat -> convertToResponse(chat, userId, authToken))
                .toList();
    }

    public ChatResponse getChat(Long chatId, UUID userId, String authToken) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException(chatId));

        // Проверяем, является ли пользователь участником
        if (!participantRepository.existsByChatIdAndUserIdAndLeftAtIsNull(chatId, userId)) {
            throw new AccessDeniedException("Access denied to chat: " + chatId);
        }

        return convertToResponse(chat, userId, authToken);
    }

    private void addParticipant(Chat chat, UUID userId, ChatRole role) {
        ChatParticipant participant = new ChatParticipant();
        participant.setChat(chat);
        participant.setUserId(userId);
        participant.setRole(role);
        participantRepository.save(participant);
    }

    private ChatResponse convertToResponse(Chat chat, UUID currentUserId, String authToken) {
        ChatResponse response = new ChatResponse();
        response.setId(chat.getId());
        response.setName(chat.getName());
        response.setType(chat.getType());
        response.setCreatedAt(chat.getCreatedAt());
        response.setUpdatedAt(chat.getUpdatedAt());

        // Получаем информацию о создателе
        try {
            UserInfoResponse creator = userServiceClient.getUserById(authToken, chat.getCreatedBy());
            response.setCreatedBy(creator);
        } catch (Exception e) {
            System.err.println("Failed to fetch creator info: " + e.getMessage());
        }

        // Получаем участников
        List<UUID> participantIds = participantRepository.findActiveParticipantIds(chat.getId());
        try {
            List<UserInfoResponse> users = userServiceClient.getUsersByIds(authToken, participantIds);
            List<ChatParticipantResponse> participants = new ArrayList<>();

            for (UUID participantId : participantIds) {
                UserInfoResponse user = users.stream()
                        .filter(u -> u.getId().equals(participantId))
                        .findFirst()
                        .orElse(null);

                if (user != null) {
                    ChatParticipantResponse participantResponse = new ChatParticipantResponse();
                    participantResponse.setUser(user);

                    // Получаем роль участника
                    participantRepository.findByChatIdAndUserId(chat.getId(), participantId)
                            .ifPresent(participant -> {
                                participantResponse.setId(participant.getId()); // Теперь это Long
                                participantResponse.setRole(participant.getRole());
                                participantResponse.setJoinedAt(participant.getJoinedAt());
                            });

                    participants.add(participantResponse);
                }
            }
            response.setParticipants(participants);
        } catch (Exception e) {
            System.err.println("Failed to fetch participant info: " + e.getMessage());
        }

        // Получаем последнее сообщение
        var lastMessage = messageService.getLastMessage(chat.getId());
        response.setLastMessage(lastMessage);

        // Считаем непрочитанные сообщения
        Integer unreadCount = chatRepository.countUnreadMessages(chat.getId(), currentUserId);
        response.setUnreadCount(unreadCount);

        return response;
    }
}