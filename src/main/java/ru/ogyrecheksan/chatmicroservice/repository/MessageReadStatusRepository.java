package ru.ogyrecheksan.chatmicroservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ogyrecheksan.chatmicroservice.model.MessageReadStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MessageReadStatusRepository extends JpaRepository<MessageReadStatus, Long> {

    List<MessageReadStatus> findByMessageIdAndUserId(Long messageId, UUID userId);

    @Query("SELECT mrs FROM MessageReadStatus mrs " +
            "WHERE mrs.message.chat.id = :chatId " +
            "AND mrs.userId = :userId " +
            "AND (:since IS NULL OR mrs.deliveredAt > :since OR mrs.readAt > :since)")
    List<MessageReadStatus> findStatusesByChatAndUserSince(@Param("chatId") Long chatId,
                                                           @Param("userId") UUID userId,
                                                           @Param("since") LocalDateTime since);
}


