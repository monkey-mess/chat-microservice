package ru.ogyrecheksan.chatmicroservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ogyrecheksan.chatmicroservice.model.Message;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByChatIdOrderBySentAtDesc(Long chatId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId ORDER BY m.sentAt DESC")
    List<Message> findLastMessages(@Param("chatId") Long chatId, Pageable pageable);

    List<Message> findByChatIdAndReadAtIsNullAndSenderIdNot(Long chatId, UUID senderId);

    // Добавьте этот метод для markMessagesAsDelivered
    List<Message> findByChatIdAndDeliveredAtIsNullAndSenderIdNot(Long chatId, UUID senderId);
}