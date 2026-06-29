package org.nexo.messagingservice.repository;

import org.nexo.messagingservice.enums.ECallStatus;
import org.nexo.messagingservice.model.CallModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CallRepository extends JpaRepository<CallModel, Long> {

    @Query("SELECT c FROM CallModel c WHERE c.id = :callId AND (c.callerUserId = :userId OR c.calleeUserId = :userId OR (c.isGroupCall = true AND c.conversationId IN (SELECT cp.conversation.id FROM ConversationParticipantModel cp WHERE cp.userId = :userId)))")
    Optional<CallModel> findByIdAndParticipant(@Param("callId") Long callId, @Param("userId") Long userId);

    @Query("SELECT c FROM CallModel c WHERE (c.callerUserId = :userId OR c.calleeUserId = :userId OR (c.isGroupCall = true AND c.conversationId IN (SELECT cp.conversation.id FROM ConversationParticipantModel cp WHERE cp.userId = :userId))) AND c.status IN :statuses ORDER BY c.startedAt DESC")
    List<CallModel> findActiveCallsByUserId(@Param("userId") Long userId, @Param("statuses") List<ECallStatus> statuses);

    @Query("SELECT c FROM CallModel c WHERE c.conversationId = :conversationId ORDER BY c.startedAt DESC")
    List<CallModel> findByConversationIdOrderByStartedAtDesc(@Param("conversationId") Long conversationId);

    @Query("SELECT c FROM CallModel c WHERE c.conversationId = :conversationId AND c.status IN :statuses ORDER BY c.startedAt DESC")
    Optional<CallModel> findFirstByConversationIdAndStatusInOrderByStartedAtDesc(@Param("conversationId") Long conversationId, @Param("statuses") List<ECallStatus> statuses);
}
