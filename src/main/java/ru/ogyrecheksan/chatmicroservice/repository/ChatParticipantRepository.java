package ru.ogyrecheksan.chatmicroservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ogyrecheksan.chatmicroservice.model.ChatParticipant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    Optional<ChatParticipant> findByChatIdAndUserId(Long chatId, UUID userId);

    List<ChatParticipant> findByChatId(Long chatId);

    List<ChatParticipant> findByUserId(UUID userId);

    @Query("SELECT cp.userId FROM ChatParticipant cp WHERE cp.chat.id = :chatId AND cp.leftAt IS NULL")
    List<UUID> findActiveParticipantIds(@Param("chatId") Long chatId); // Изменено с List<Long> на List<UUID>

    boolean existsByChatIdAndUserIdAndLeftAtIsNull(Long chatId, UUID userId);
}