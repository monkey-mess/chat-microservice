package ru.ogyrecheksan.chatmicroservice.Unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ogyrecheksan.chatmicroservice.service.MessageService;
import ru.ogyrecheksan.chatmicroservice.repository.MessageRepository;
import ru.ogyrecheksan.chatmicroservice.repository.ChatRepository;
import ru.ogyrecheksan.chatmicroservice.service.WebSocketService;
import ru.ogyrecheksan.chatmicroservice.dto.Request.SendMessageRequest;
import ru.ogyrecheksan.chatmicroservice.model.Chat;
import ru.ogyrecheksan.chatmicroservice.model.Message;
import ru.ogyrecheksan.chatmicroservice.model.enums.MessageType;
import ru.ogyrecheksan.chatmicroservice.dto.Response.MessageResponse;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private MessageService messageService;

    @Test
    void sendMessage_WithValidRequest_ShouldSaveMessage() {
        SendMessageRequest req = new SendMessageRequest();
        req.setChatId(1L);
        req.setContent("Hello");
        req.setType(MessageType.TEXT);

        Chat chat = new Chat();
        chat.setId(1L);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(10L);
            return m;
        });

        MessageResponse resp = messageService.sendMessage(req, UUID.randomUUID(), "Bearer token");

        assertNotNull(resp);
        assertEquals("Hello", resp.getContent());
        assertEquals("1", resp.getConversationId());
        verify(webSocketService).sendMessageToChat(eq(1L), any(MessageResponse.class));
    }
}