package org.nexo.messagingservice.repository;

import org.nexo.messagingservice.model.MessageModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<MessageModel, Long> {

        Page<MessageModel> findByConversationIdAndIsActiveTrueOrderByCreatedAtDesc(
                        Long conversationId,
                        Pageable pageable);

        @Query("SELECT m FROM MessageModel m " +
                        "WHERE m.conversation.id = :conversationId " +
                        "AND m.isActive = true " +
                        "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "ORDER BY m.createdAt DESC")
        Page<MessageModel> searchMessagesByKeyword(
                        Long conversationId,
                        String keyword,
                        Pageable pageable);

        Optional<MessageModel> findFirstByConversationIdAndIsActiveTrueOrderByCreatedAtDesc(
                        Long conversationId);

        @Query("SELECT COUNT(m) FROM MessageModel m " +
                        "WHERE m.conversation.id = :conversationId " +
                        "AND m.senderUserId != :userId " +
                        "AND m.isActive = true " +
                        "AND m.id > :lastReadMessageId")
        Long countUnreadMessages(
                        Long conversationId,
                        Long userId,
                        Long lastReadMessageId);

        Optional<MessageModel> findByIdAndIsActiveTrue(Long messageId);
}