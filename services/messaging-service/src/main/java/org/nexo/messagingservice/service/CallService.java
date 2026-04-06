package org.nexo.messagingservice.service;

import org.nexo.messagingservice.dto.*;

public interface CallService {

    CallNotificationDTO initiateCall(CallInitiateRequest request, Long callerUserId);

    CallResponseDTO respondToCall(CallResponseRequest request, Long calleeUserId);

    CallSignalDTO relaySignal(CallSignalRequest request, Long senderUserId);

    CallEndedDTO endCall(CallEndRequest request, Long userId);

    Long getOtherParticipantId(Long callId, Long currentUserId);
}
