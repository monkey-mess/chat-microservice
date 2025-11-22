package ru.ogyrecheksan.chatmicroservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ogyrecheksan.chatmicroservice.dto.Request.SendMessageRequest;
import ru.ogyrecheksan.chatmicroservice.dto.Response.MessageResponse;
import ru.ogyrecheksan.chatmicroservice.dto.Response.UserInfoResponse;
import ru.ogyrecheksan.chatmicroservice.model.Message;
import ru.ogyrecheksan.chatmicroservice.repository.ChatRepository;
import ru.ogyrecheksan.chatmicroservice.repository.MessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final UserServiceClient userServiceClient;
    private final WebSocketService webSocketService;

    public MessageResponse sendMessage(SendMessageRequest request, UUID senderId, String authToken) {
        // Проверяем существование чата
        var chat = chatRepository.findById(request.getChatId())
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        // Создаем сообщение
        Message message = new Message();
        message.setContent(request.getContent());
        message.setType(request.getType());
        message.setChat(chat);
        message.setSenderId(senderId);
        message.setSentAt(LocalDateTime.now());

        // Обрабатываем ответ на сообщение
        if (request.getReplyToMessageId() != null) {
            Message replyTo = messageRepository.findById(request.getReplyToMessageId())
                    .orElseThrow(() -> new RuntimeException("Reply message not found"));
            message.setReplyTo(replyTo);
        }

        Message savedMessage = messageRepository.save(message);

        // Отправляем через WebSocket
        MessageResponse response = convertToResponse(savedMessage, authToken);
        webSocketService.sendMessageToChat(chat.getId(), response);

        return response;
    }

    public Page<MessageResponse> getChatMessages(Long chatId, UUID userId, int page, int size, String authToken) {
        // Проверяем доступ
        if (!chatRepository.existsById(chatId)) {
            throw new RuntimeException("Chat not found");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        Page<Message> messages = messageRepository.findByChatIdOrderBySentAtDesc(chatId, pageable);

        // Помечаем сообщения как доставленные
        markMessagesAsDelivered(chatId, userId);

        return messages.map(message -> convertToResponse(message, authToken));
    }

    public MessageResponse getLastMessage(Long chatId) {
        Pageable pageable = PageRequest.of(0, 1, Sort.by("sentAt").descending());
        List<Message> messages = messageRepository.findLastMessages(chatId, pageable);

        if (messages.isEmpty()) {
            return null;
        }

        return convertToResponse(messages.get(0), null);
    }

    public void markMessagesAsRead(Long chatId, UUID userId) {
        List<Message> unreadMessages = messageRepository
                .findByChatIdAndReadAtIsNullAndSenderIdNot(chatId, userId);

        for (Message message : unreadMessages) {
            message.setReadAt(LocalDateTime.now());
        }

        messageRepository.saveAll(unreadMessages);
    }

    private void markMessagesAsDelivered(Long chatId, UUID userId) {
        List<Message> undeliveredMessages = messageRepository
                .findByChatIdAndDeliveredAtIsNullAndSenderIdNot(chatId, userId);

        LocalDateTime now = LocalDateTime.now();
        for (Message message : undeliveredMessages) {
            message.setDeliveredAt(now);
        }

        messageRepository.saveAll(undeliveredMessages);
    }

    private MessageResponse convertToResponse(Message message, String authToken) {
        MessageResponse response = new MessageResponse();
        response.setId(message.getId());
        response.setContent(message.getContent());
        response.setType(message.getType());
        response.setSentAt(message.getSentAt());
        response.setDeliveredAt(message.getDeliveredAt());
        response.setReadAt(message.getReadAt());

        // Получаем информацию об отправителе
        if (authToken != null) {
            try {
                UserInfoResponse sender = userServiceClient.getUserById(authToken, message.getSenderId());
                response.setSender(sender);
            } catch (Exception e) {
                System.err.println("Failed to fetch sender info: " + e.getMessage());
            }
        }

        // Обрабатываем ответ на сообщение
        if (message.getReplyTo() != null) {
            response.setReplyTo(convertToResponse(message.getReplyTo(), null));
        }

        return response;
    }
}