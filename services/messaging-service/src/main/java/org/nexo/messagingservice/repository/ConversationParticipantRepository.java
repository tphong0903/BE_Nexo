package org.nexo.messagingservice.repository;

import org.nexo.messagingservice.model.ConversationParticipantModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipantModel, Long> {
    
    Optional<ConversationParticipantModel> findByConversationIdAndUserId(
        Long conversationId,
        Long userId
    );
    
    Boolean existsByConversationIdAndUserId(
        Long conversationId,
        Long userId
    );
    
    @Query("SELECT p.userId FROM ConversationParticipantModel p " +
           "WHERE p.conversation.id = :conversationId ")
    List<Long> findActiveUserIdsByConversationId(Long conversationId);
    
    List<ConversationParticipantModel> findByConversationId(
        Long conversationId
    );
}