package ru.ogyrecheksan.chatmicroservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import ru.ogyrecheksan.chatmicroservice.repository.ChatParticipantRepository;

import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Перехватчик STOMP-сообщений для проверки прав доступа:
 *  - проверяет принадлежность пользователя к чату перед SUBSCRIBE/SEND
 *  - логирует попытки несанкционированного доступа
 */
@Component
@RequiredArgsConstructor
public class WebSocketSecurityInterceptor implements ChannelInterceptor {

    private final ChatParticipantRepository participantRepository;

    private static final Pattern CHAT_TOPIC_PATTERN =
            Pattern.compile("^/topic/chat/(\\d+)$");

    private static final Pattern CHAT_APP_PATTERN =
            Pattern.compile("^/app/chat/(\\d+).*$");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (command == null) {
            return message;
        }

        if (command == StompCommand.SUBSCRIBE || command == StompCommand.SEND) {
            String destination = accessor.getDestination();
            Principal principal = accessor.getUser();

            if (destination == null || principal == null) {
                return message;
            }

            Long chatId = extractChatId(destination);
            if (chatId == null) {
                return message;
            }

            UUID userId = extractUserId(principal);

            boolean isParticipant = participantRepository
                    .existsByChatIdAndUserIdAndLeftAtIsNull(chatId, userId);

            if (!isParticipant) {
                System.out.printf("Unauthorized WebSocket access: user=%s, chatId=%d, destination=%s%n",
                        userId, chatId, destination);
                throw new ru.ogyrecheksan.chatmicroservice.exception.AccessDeniedException(
                        "Access denied to chat: " + chatId);
            }
        }

        return message;
    }

    private Long extractChatId(String destination) {
        Matcher topicMatcher = CHAT_TOPIC_PATTERN.matcher(destination);
        if (topicMatcher.matches()) {
            return Long.parseLong(topicMatcher.group(1));
        }
        Matcher appMatcher = CHAT_APP_PATTERN.matcher(destination);
        if (appMatcher.matches()) {
            return Long.parseLong(appMatcher.group(1));
        }
        return null;
    }

    private UUID extractUserId(Principal principal) {
        try {
            return UUID.fromString(principal.getName());
        } catch (IllegalArgumentException e) {
            return UUID.nameUUIDFromBytes(principal.getName().getBytes());
        }
    }
}


