package ru.ogyrecheksan.chatmicroservice.dto.Response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Персональное уведомление, доставляемое в очередь пользователя /user/queue/notifications.
 *
 * type:
 *  - NEW_MESSAGE – новое сообщение в чате
 *  - NEW_CHAT    – пользователь добавлен в новый чат
 */
@Data
public class NotificationResponse {

    private String type;          // NEW_MESSAGE, NEW_CHAT
    private Long chatId;          // ID чата, к которому относится событие
    private UUID senderId;        // Кто инициировал событие (отправитель сообщения / создатель чата)
    private String preview;       // Первые ~50 символов контента или название чата
    private LocalDateTime timestamp; // Время события (sentAt сообщения или createdAt чата)
}


