package ru.ogyrecheksan.chatmicroservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ogyrecheksan.chatmicroservice.dto.Request.CreateChatRequest;
import ru.ogyrecheksan.chatmicroservice.dto.Request.ParticipantActionRequest;
import ru.ogyrecheksan.chatmicroservice.dto.Response.ChatResponse;
import ru.ogyrecheksan.chatmicroservice.exception.AccessDeniedException;
import ru.ogyrecheksan.chatmicroservice.exception.ChatNotFoundException;
import ru.ogyrecheksan.chatmicroservice.model.Chat;
import ru.ogyrecheksan.chatmicroservice.model.ChatParticipant;
import ru.ogyrecheksan.chatmicroservice.model.enums.ChatRole;
import ru.ogyrecheksan.chatmicroservice.model.enums.ChatType;
import ru.ogyrecheksan.chatmicroservice.repository.ChatParticipantRepository;
import ru.ogyrecheksan.chatmicroservice.repository.ChatRepository;
import ru.ogyrecheksan.chatmicroservice.service.WebSocketService;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatParticipantRepository participantRepository;
    private final MessageService messageService;
        private final WebSocketService webSocketService;

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

        // Уведомляем участников о новом личном чате
        webSocketService.sendNewChatNotifications(
                savedChat,
                user1Id,
                Set.of(user1Id, user2Id)
        );

        return convertToResponse(savedChat, user1Id, authToken);
    }

    public ChatResponse createGroupChat(CreateChatRequest request, UUID creatorId, String authToken) {
        Chat chat = new Chat();
        if (request.getType() == ChatType.GROUP && (request.getName() == null || request.getName().isBlank())) {
            throw new IllegalArgumentException("Name is required for group chat");
        }
        chat.setName(request.getName());
        chat.setType(request.getType());
        chat.setCreatedBy(creatorId);
        chat.setDescription(request.getDescription());

        Chat savedChat = chatRepository.save(chat);

        // Добавляем создателя как владельца
        addParticipant(savedChat, creatorId, ChatRole.OWNER);

        // Добавляем других участников
        Set<UUID> participantIds = new java.util.HashSet<>();
        participantIds.add(creatorId);
        if (request.getParticipantIds() != null) {
            for (UUID participantId : request.getParticipantIds()) {
                if (!participantId.equals(creatorId)) {
                    addParticipant(savedChat, participantId, ChatRole.MEMBER);
                }
                participantIds.add(participantId);
            }
        }

        // Уведомляем всех участников о новом групповом чате
        webSocketService.sendNewChatNotifications(savedChat, creatorId, participantIds);

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

    public void updateParticipants(Long chatId, UUID actorId, ParticipantActionRequest request) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException(chatId));
        // Проверка что actor участник
        if (!participantRepository.existsByChatIdAndUserIdAndLeftAtIsNull(chatId, actorId)) {
            throw new AccessDeniedException("Access denied to chat: " + chatId);
        }
        if ("add".equalsIgnoreCase(request.getAction())) {
            addParticipant(chat, request.getUserId(), ChatRole.MEMBER);
        } else if ("remove".equalsIgnoreCase(request.getAction())) {
            participantRepository.findByChatIdAndUserId(chatId, request.getUserId())
                    .ifPresent(p -> {
                        p.setLeftAt(java.time.LocalDateTime.now());
                        participantRepository.save(p);
                    });
        } else {
            throw new IllegalArgumentException("Unsupported action");
        }
    }

    public String uploadAvatar(Long chatId, UUID actorId, MultipartFile file) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException(chatId));
        if (!participantRepository.existsByChatIdAndUserIdAndLeftAtIsNull(chatId, actorId)) {
            throw new AccessDeniedException("Access denied to chat: " + chatId);
        }
        try {
            Path uploadDir = Path.of("uploads");
            Files.createDirectories(uploadDir);
            String filename = "chat-" + chatId + "-" + file.getOriginalFilename();
            Path target = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String url = "/uploads/" + filename;
            chat.setAvatarUrl(url);
            chatRepository.save(chat);
            return url;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload avatar", e);
        }
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
        response.setType(chat.getType() == ChatType.PERSONAL ? "private" : "group");
        response.setDescription(null);
        response.setAvatarUrl(chat.getAvatarUrl());
        response.setCreatedAt(chat.getCreatedAt());

        // Участники
        List<UUID> participantIds = participantRepository.findActiveParticipantIds(chat.getId());
        List<ChatResponse.Participant> participants = new ArrayList<>();
        try {
            for (UUID participantId : participantIds) {
                ChatResponse.Participant p = new ChatResponse.Participant();
                p.setUserId(participantId);
                participantRepository.findByChatIdAndUserId(chat.getId(), participantId)
                        .ifPresent(participant -> p.setRole(participant.getRole().name().toLowerCase()));
                participants.add(p);
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch participant info: " + e.getMessage());
        }
        response.setParticipants(participants);

        // Последнее сообщение
        var lastMessage = messageService.getLastMessage(chat.getId());
        if (lastMessage != null) {
            ChatResponse.LastMessage lm = new ChatResponse.LastMessage();
            lm.setId(lastMessage.getId());
            lm.setContent(lastMessage.getContent());
            lm.setSenderId(lastMessage.getSenderId());
            response.setLastMessage(lm);
        }

        return response;
    }
}