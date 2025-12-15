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
import ru.ogyrecheksan.chatmicroservice.model.Message;
import ru.ogyrecheksan.chatmicroservice.repository.ChatParticipantRepository;
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
        private final ChatParticipantRepository chatParticipantRepository;
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

        // Формируем DTO
        MessageResponse response = convertToResponse(savedMessage, authToken);

        // Отправляем через WebSocket в общий топик чата
        webSocketService.sendMessageToChat(chat.getId(), response);

        // Отправляем персональные уведомления всем участникам чата, кроме отправителя
        var participantIds = chatParticipantRepository.findActiveParticipantIds(chat.getId());
        webSocketService.sendNewMessageNotifications(chat.getId(), senderId, response, participantIds);

        return response;
    }

    public Page<MessageResponse> getChatMessages(Long chatId, UUID userId, int offset, int limit, String authToken) {
        // Проверяем доступ
        if (!chatRepository.existsById(chatId)) {
            throw new RuntimeException("Chat not found");
        }

        int pageIndex = offset / Math.max(limit, 1);
        Pageable pageable = PageRequest.of(pageIndex, limit, Sort.by("sentAt").descending());
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
        response.setId(String.valueOf(message.getId()));
        response.setConversationId(String.valueOf(message.getChat().getId()));
        response.setSenderId(message.getSenderId() != null ? message.getSenderId().toString() : null);
        response.setContent(message.getContent());
        response.setCreatedAt(message.getSentAt());

        return response;
    }
}