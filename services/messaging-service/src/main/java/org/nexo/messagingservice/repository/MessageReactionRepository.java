package org.nexo.messagingservice.repository;

import org.nexo.messagingservice.model.MessageReactionModel;
import org.nexo.messagingservice.enums.EReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReactionModel, Long> {
    
    Optional<MessageReactionModel> findByMessageIdAndUserIdAndReactionType(
        Long messageId,
        Long userId,
        EReactionType reactionType
    );
    
    void deleteByMessageIdAndUserIdAndReactionType(
        Long messageId,
        Long userId,
        EReactionType reactionType
    );
    
    List<MessageReactionModel> findByMessageId(Long messageId);
    
    Optional<MessageReactionModel> findByMessageIdAndUserId(
        Long messageId,
        Long userId
    );

}