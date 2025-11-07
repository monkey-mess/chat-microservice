package ru.ogyrecheksan.chatmicroservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ogyrecheksan.chatmicroservice.model.ChatParticipant;
import java.util.List;
import java.util.Optional;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    Optional<ChatParticipant> findByChatIdAndUserId(Long chatId, Long userId);

    List<ChatParticipant> findByChatId(Long chatId);

    List<ChatParticipant> findByUserId(Long userId);

    @Query("SELECT cp.userId FROM ChatParticipant cp WHERE cp.chat.id = :chatId AND cp.leftAt IS NULL")
    List<Long> findActiveParticipantIds(@Param("chatId") Long chatId);

    boolean existsByChatIdAndUserIdAndLeftAtIsNull(Long chatId, Long userId);
}
