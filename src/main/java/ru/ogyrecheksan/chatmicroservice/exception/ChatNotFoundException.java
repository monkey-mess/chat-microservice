package ru.ogyrecheksan.chatmicroservice.exception;

public class ChatNotFoundException extends RuntimeException {
    public ChatNotFoundException(String message) {
        super(message);
    }

    public ChatNotFoundException(Long chatId) {
        super("Chat not found with id: " + chatId);
    }
}
