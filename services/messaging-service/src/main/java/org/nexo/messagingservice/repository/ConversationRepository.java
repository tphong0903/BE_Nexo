package org.nexo.messagingservice.repository;

import java.util.Optional;

import org.nexo.messagingservice.model.ConversationModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationModel, Long> {
        @Query("SELECT c FROM ConversationModel c " +
                        "JOIN c.participants p1 " +
                        "JOIN c.participants p2 " +
                        "WHERE p1.userId = :userId1 " +
                        "AND p2.userId = :userId2 " +
                        "AND p1.userId != p2.userId")
        Optional<ConversationModel> findDirectConversationBetweenUsers(
                        @Param("userId1") Long userId1,
                        @Param("userId2") Long userId2);

        // lay danh sach inbox normal
        @Query("SELECT c FROM ConversationModel c " +
                        "JOIN c.participants p " +
                        "WHERE p.userId = :userId " +
                        "AND p.isArchived = false " +
                        "AND c.status = 'NORMAL' " +
                        "ORDER BY c.lastMessageAt DESC NULLS LAST")
        Page<ConversationModel> findNormalConversationsByUserId(
                        @Param("userId") Long userId,
                        Pageable pageable);

        // lay danh sach pending cho recipient
        @Query("SELECT c FROM ConversationModel c " +
                        "JOIN c.participants p " +
                        "WHERE p.userId = :userId " +
                        "AND p.isRecipient = true " +
                        "AND c.status = 'PENDING' " +
                        "ORDER BY c.createdAt DESC")
        Page<ConversationModel> findPendingConversationsByRecipientId(
                        @Param("userId") Long userId,
                        Pageable pageable);

        // dem so luong pending request cho recipient
        @Query("SELECT COUNT(c) FROM ConversationModel c " +
                        "JOIN c.participants p " +
                        "WHERE p.userId = :userId " +
                        "AND p.isRecipient = true " +
                        "AND c.status = 'PENDING'")
        Long countPendingRequestsByRecipientId(@Param("userId") Long userId);

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

        @Query("SELECT c FROM ConversationModel c JOIN c.participants p " +
                        "WHERE p.userId = :userId AND c.status = 'PENDING' " +
                        "ORDER BY c.createdAt DESC")
        Page<ConversationModel> findPendingConversationsByUserId(@Param("userId") Long userId, Pageable pageable);
}