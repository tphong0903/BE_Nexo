package org.nexo.messagingservice.service;

import java.util.List;

import org.nexo.messagingservice.dto.*;

public interface CallService {

    CallNotificationDTO initiateCall(CallInitiateRequest request, Long callerUserId);

    CallResponseDTO respondToCall(CallResponseRequest request, Long calleeUserId);

    CallSignalDTO relaySignal(CallSignalRequest request, Long senderUserId);

    CallEndedDTO endCall(CallEndRequest request, Long userId);

    CallNotificationDTO pingUser(Long callId, Long targetUserId, Long callerUserId);

    CallResponseDTO joinActiveCall(Long callId, Long userId);

    Long getOtherParticipantId(Long callId, Long currentUserId);

    List<CallEndedDTO> handleUserDisconnect(Long userId);
}
