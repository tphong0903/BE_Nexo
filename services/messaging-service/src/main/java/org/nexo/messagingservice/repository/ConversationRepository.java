package org.nexo.messagingservice.repository;

import org.nexo.messagingservice.model.ConversationModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationModel, Long> {

    /**
     * Lấy danh sách conversations của user
     */
    @Query("SELECT c FROM ConversationModel c " +
            "JOIN c.participants p " +
            "WHERE p.userId = :userId " +
            "ORDER BY c.lastMessageAt DESC NULLS LAST")
    Page<ConversationModel> findByUserIdOrderByLastMessageAtDesc(
            @Param("userId") Long userId,
            Pageable pageable);

    /**
     * Tìm conversation 1-1 giữa 2 users
     */
    @Query("SELECT c FROM ConversationModel c " +
            "JOIN c.participants p1 " +
            "JOIN c.participants p2 " +
            "WHERE p1.userId = :userId1 " +
            "AND p2.userId = :userId2 " +
            "AND p1.userId != p2.userId")
    Optional<ConversationModel> findDirectConversationBetweenUsers(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2);

    /**
     * Check user có phải participant không
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM ConversationParticipantModel p " +
            "WHERE p.conversation.id = :conversationId " +
            "AND p.userId = :userId")
    Boolean isUserParticipant(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId);
}