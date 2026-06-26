package org.nexo.messagingservice.repository;

import org.nexo.messagingservice.model.CallParticipantModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CallParticipantRepository extends JpaRepository<CallParticipantModel, Long> {
    
    List<CallParticipantModel> findByCallId(Long callId);
    
    Optional<CallParticipantModel> findByCallIdAndUserId(Long callId, Long userId);
}
