package org.nexo.messagingservice.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.messagingservice.dto.*;
import org.nexo.messagingservice.enums.ECallStatus;
import org.nexo.messagingservice.exception.ResourceNotFoundException;
import org.nexo.messagingservice.grpc.UserGrpcClient;
import org.nexo.messagingservice.model.CallModel;
import org.nexo.messagingservice.repository.CallRepository;
import org.nexo.messagingservice.repository.ConversationParticipantRepository;
import org.nexo.messagingservice.service.CallService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CallServiceImpl implements CallService {

    private final CallRepository callRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserGrpcClient userGrpcClient;

    @Override
    public CallNotificationDTO initiateCall(CallInitiateRequest request, Long callerUserId) {
        List<Long> participantIds = participantRepository
                .findActiveUserIdsByConversationId(request.getConversationId());
        if (!participantIds.contains(callerUserId)) {
            throw new AccessDeniedException("Caller is not a participant of this conversation");
        }

        Long calleeUserId = participantIds.stream()
                .filter(id -> !id.equals(callerUserId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No callee found in conversation"));

        if (userGrpcClient.isUserBlocked(callerUserId, calleeUserId)) {
            throw new AccessDeniedException("Cannot call a blocked user");
        }

        List<ECallStatus> activeStatuses = List.of(ECallStatus.INITIATED, ECallStatus.RINGING);
        callRepository.findActiveCallsByUserId(callerUserId, activeStatuses)
                .forEach(staleCall -> {
                    staleCall.setStatus(ECallStatus.MISSED);
                    staleCall.setEndedAt(LocalDateTime.now());
                    callRepository.save(staleCall);
                });

        CallModel call = CallModel.builder()
                .conversationId(request.getConversationId())
                .callerUserId(callerUserId)
                .calleeUserId(calleeUserId)
                .callType(request.getCallType())
                .status(ECallStatus.RINGING)
                .build();
        call = callRepository.save(call);

        UserServiceProto.UserDTOResponse callerInfo = userGrpcClient.getUserById(callerUserId);

        return CallNotificationDTO.builder()
                .callId(call.getId())
                .conversationId(call.getConversationId())
                .callerId(callerUserId)
                .callerUsername(callerInfo.getUsername())
                .callerFullName(callerInfo.getFullName())
                .callerAvatarUrl(callerInfo.getAvatar())
                .callType(call.getCallType())
                .startedAt(call.getStartedAt())
                .build();
    }

    @Override
    public CallResponseDTO respondToCall(CallResponseRequest request, Long calleeUserId) {
        CallModel call = callRepository.findByIdAndParticipant(request.getCallId(), calleeUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found or access denied"));

        if (call.getStatus() != ECallStatus.RINGING) {
            throw new IllegalStateException("Call is no longer ringing (status: " + call.getStatus() + ")");
        }

        ECallStatus newStatus;
        if (Boolean.TRUE.equals(request.getAccepted())) {
            newStatus = ECallStatus.ACCEPTED;
            call.setAnsweredAt(LocalDateTime.now());
        } else {
            newStatus = ECallStatus.REJECTED;
            call.setEndedAt(LocalDateTime.now());
        }
        call.setStatus(newStatus);
        callRepository.save(call);

        UserServiceProto.UserDTOResponse calleeInfo = userGrpcClient.getUserById(calleeUserId);

        return CallResponseDTO.builder()
                .callId(call.getId())
                .responderId(calleeUserId)
                .responderUsername(calleeInfo.getUsername())
                .status(newStatus)
                .build();
    }

    @Override
    public CallSignalDTO relaySignal(CallSignalRequest request, Long senderUserId) {
        CallModel call = callRepository.findByIdAndParticipant(request.getCallId(), senderUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found or access denied"));

        if (call.getStatus() != ECallStatus.ACCEPTED) {
            throw new IllegalStateException("Call is not active (status: " + call.getStatus() + ")");
        }

        return CallSignalDTO.builder()
                .callId(call.getId())
                .senderId(senderUserId)
                .type(request.getType())
                .sdp(request.getSdp())
                .candidate(request.getCandidate())
                .build();
    }

    @Override
    public CallEndedDTO endCall(CallEndRequest request, Long userId) {
        CallModel call = callRepository.findByIdAndParticipant(request.getCallId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found or access denied"));

        ECallStatus finalStatus;
        LocalDateTime now = LocalDateTime.now();

        if (call.getStatus() == ECallStatus.RINGING) {
            finalStatus = userId.equals(call.getCallerUserId()) ? ECallStatus.ENDED : ECallStatus.MISSED;
        } else if (call.getStatus() == ECallStatus.ACCEPTED) {
            finalStatus = ECallStatus.ENDED;
        } else {
            finalStatus = call.getStatus();
        }

        Long durationSeconds = null;
        if (call.getAnsweredAt() != null) {
            durationSeconds = ChronoUnit.SECONDS.between(call.getAnsweredAt(), now);
        }

        call.setStatus(finalStatus);
        call.setEndedAt(now);
        call.setDurationSeconds(durationSeconds);
        callRepository.save(call);

        return CallEndedDTO.builder()
                .callId(call.getId())
                .endedByUserId(userId)
                .finalStatus(finalStatus)
                .durationSeconds(durationSeconds)
                .endedAt(now)
                .build();
    }

    @Override
    public Long getOtherParticipantId(Long callId, Long currentUserId) {
        CallModel call = callRepository.findByIdAndParticipant(callId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found or access denied"));
        return call.getCallerUserId().equals(currentUserId) ? call.getCalleeUserId() : call.getCallerUserId();
    }
}
