package ru.ogyrecheksan.chatmicroservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ogyrecheksan.chatmicroservice.model.Chat;
import java.util.List;
import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    @Query("SELECT c FROM Chat c JOIN c.participants p WHERE p.userId = :userId AND p.leftAt IS NULL")
    List<Chat> findUserChats(@Param("userId") Long userId);

    @Query("SELECT c FROM Chat c JOIN c.participants p1 JOIN c.participants p2 " +
            "WHERE c.type = 'PERSONAL' AND p1.userId = :user1Id AND p2.userId = :user2Id " +
            "AND p1.leftAt IS NULL AND p2.leftAt IS NULL")
    Optional<Chat> findPersonalChat(@Param("user1Id") Long user1Id,
                                    @Param("user2Id") Long user2Id);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chat.id = :chatId AND m.readAt IS NULL AND m.senderId != :userId")
    Integer countUnreadMessages(@Param("chatId") Long chatId, @Param("userId") Long userId);
}