package ru.ogyrecheksan.chatmicroservice.service;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реестр WebSocket-подписок:
 *  - хранит онлайн-пользователей по чатам
 *  - помогает восстанавливать подписки при переподключениях
 */
@Component
public class WebSocketSubscriptionRegistry {

    /**
     * chatId -> set of userIds, которые сейчас онлайн в этом чате
     */
    private final Map<Long, Set<UUID>> chatOnlineUsers = new ConcurrentHashMap<>();

    /**
     * sessionId -> userId
     */
    private final Map<String, UUID> sessionUsers = new ConcurrentHashMap<>();

    /**
     * sessionId -> set of chatIds, на которые подписан пользователь в рамках этой сессии
     */
    private final Map<String, Set<Long>> sessionChats = new ConcurrentHashMap<>();

    public void registerSession(String sessionId, UUID userId) {
        sessionUsers.put(sessionId, userId);
    }

    public void unregisterSession(String sessionId) {
        UUID userId = sessionUsers.remove(sessionId);
        if (userId == null) {
            return;
        }
        Set<Long> chats = sessionChats.remove(sessionId);
        if (chats != null) {
            for (Long chatId : chats) {
                removeOnlineUser(chatId, userId);
            }
        }
    }

    public void subscribeToChat(String sessionId, Long chatId, UUID userId) {
        registerSession(sessionId, userId);
        sessionChats.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(chatId);
        addOnlineUser(chatId, userId);
    }

    public void unsubscribeFromChat(String sessionId, Long chatId) {
        UUID userId = sessionUsers.get(sessionId);
        if (userId == null) {
            return;
        }
        Set<Long> chats = sessionChats.getOrDefault(sessionId, Collections.emptySet());
        chats.remove(chatId);
        removeOnlineUser(chatId, userId);
    }

    public Set<UUID> getOnlineUsers(Long chatId) {
        return chatOnlineUsers.getOrDefault(chatId, Collections.emptySet());
    }

    private void addOnlineUser(Long chatId, UUID userId) {
        chatOnlineUsers
                .computeIfAbsent(chatId, k -> ConcurrentHashMap.newKeySet())
                .add(userId);
    }

    private void removeOnlineUser(Long chatId, UUID userId) {
        Set<UUID> users = chatOnlineUsers.get(chatId);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) {
                chatOnlineUsers.remove(chatId);
            }
        }
    }
}


