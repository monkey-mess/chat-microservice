package ru.ogyrecheksan.chatmicroservice.Unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ogyrecheksan.chatmicroservice.service.MessageService;
import ru.ogyrecheksan.chatmicroservice.repository.MessageRepository;
import ru.ogyrecheksan.chatmicroservice.repository.ChatRepository;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatRepository chatRepository;

    @InjectMocks
    private MessageService messageService;

    @Test
    void sendMessage_WithValidRequest_ShouldSaveMessage() {
        // Тестируем логику отправки сообщений
        // ...
    }
}