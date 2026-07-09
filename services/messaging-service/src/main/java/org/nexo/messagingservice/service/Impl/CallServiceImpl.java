package org.nexo.messagingservice.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.messagingservice.dto.*;
import org.nexo.messagingservice.enums.ECallStatus;
import org.nexo.messagingservice.enums.ECallType;
import org.nexo.messagingservice.enums.EMessageType;
import org.nexo.messagingservice.exception.ResourceNotFoundException;
import org.nexo.messagingservice.grpc.UserGrpcClient;
import org.nexo.messagingservice.model.CallModel;
import org.nexo.messagingservice.model.CallParticipantModel;
import org.nexo.messagingservice.model.MessageModel;
import org.nexo.messagingservice.model.ConversationModel;
import org.nexo.messagingservice.repository.CallParticipantRepository;
import org.nexo.messagingservice.repository.CallRepository;
import org.nexo.messagingservice.repository.ConversationParticipantRepository;
import org.nexo.messagingservice.repository.ConversationRepository;
import org.nexo.messagingservice.repository.MessageRepository;
import org.nexo.messagingservice.service.CallService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CallServiceImpl implements CallService {

    private final CallRepository callRepository;
    private final CallParticipantRepository callParticipantRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserGrpcClient userGrpcClient;

    @Override
    public CallNotificationDTO initiateCall(CallInitiateRequest request, Long callerUserId) {
        ConversationModel conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        List<Long> participantIds = participantRepository
                .findActiveUserIdsByConversationId(request.getConversationId());
        if (!participantIds.contains(callerUserId)) {
            throw new AccessDeniedException("Caller is not a participant of this conversation");
        }

        boolean isGroupCall = participantIds.size() > 2;

        if (!isGroupCall && conversation.getStatus() == org.nexo.messagingservice.enums.EConversationStatus.PENDING) {
            throw new AccessDeniedException("Cannot initiate call for pending conversations");
        }
        Long calleeUserId = null;

        if (!isGroupCall) {
            calleeUserId = participantIds.stream()
                    .filter(id -> !id.equals(callerUserId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("No callee found in conversation"));

            if (userGrpcClient.isUserBlocked(callerUserId, calleeUserId)) {
                throw new AccessDeniedException("Cannot call a blocked user");
            }
        }

        // Dọn cuộc gọi cũ đang treo (nếu có) — try-catch để không ảnh hưởng flow chính
        try {
            List<ECallStatus> activeStatuses = List.of(ECallStatus.INITIATED, ECallStatus.RINGING);
            callRepository.findActiveCallsByUserId(callerUserId, activeStatuses)
                    .forEach(staleCall -> {
                        staleCall.setStatus(ECallStatus.MISSED);
                        staleCall.setEndedAt(LocalDateTime.now());
                        callRepository.save(staleCall);
                    });
        } catch (Exception e) {
            log.warn("Failed to cleanup stale calls for userId={}: {}", callerUserId, e.getMessage());
        }

        CallModel call = CallModel.builder()
                .conversationId(request.getConversationId())
                .callerUserId(callerUserId)
                .calleeUserId(calleeUserId)
                .isGroupCall(isGroupCall)
                .callType(request.getCallType())
                .status(ECallStatus.RINGING)
                .build();
        call = callRepository.save(call);

        // Tạo CallParticipantModel cho caller (ACCEPTED ngay)
        CallParticipantModel callerParticipant = CallParticipantModel.builder()
                .call(call)
                .userId(callerUserId)
                .status(ECallStatus.ACCEPTED)
                .joinedAt(LocalDateTime.now())
                .build();
        callParticipantRepository.save(callerParticipant);

        // Tạo CallParticipantModel cho callee (RINGING) — cần thiết để khi endCall,
        // findByCallId trả về cả callee để broadcast ended event tới họ.
        if (!isGroupCall && calleeUserId != null) {
            CallParticipantModel calleeParticipant = CallParticipantModel.builder()
                    .call(call)
                    .userId(calleeUserId)
                    .status(ECallStatus.RINGING)
                    .build();
            callParticipantRepository.save(calleeParticipant);
        }

        UserServiceProto.UserDTOResponse callerInfo = userGrpcClient.getUserById(callerUserId);

        return CallNotificationDTO.builder()
                .callId(call.getId())
                .conversationId(call.getConversationId())
                .callerId(callerUserId)
                .callerUsername(callerInfo.getUsername())
                .callerFullName(callerInfo.getFullName())
                .callerAvatarUrl(callerInfo.getAvatar())
                .callType(call.getCallType())
                .isGroupCall(isGroupCall)
                .startedAt(call.getStartedAt())
                .build();
    }

    @Override
    public CallNotificationDTO pingUser(Long callId, Long targetUserId, Long callerUserId) {
        CallModel call = callRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found"));
        if (!call.isGroupCall()) {
            throw new IllegalArgumentException("Ping is only available for group calls");
        }
        UserServiceProto.UserDTOResponse callerInfo = userGrpcClient.getUserById(callerUserId);
        return CallNotificationDTO.builder()
                .callId(call.getId())
                .conversationId(call.getConversationId())
                .callerId(callerUserId)
                .callerUsername(callerInfo.getUsername())
                .callerFullName(callerInfo.getFullName())
                .callerAvatarUrl(callerInfo.getAvatar())
                .callType(call.getCallType())
                .isGroupCall(true)
                .startedAt(call.getStartedAt())
                .build();
    }

    @Override
    public CallResponseDTO respondToCall(CallResponseRequest request, Long calleeUserId) {
        CallModel call = callRepository.findByIdAndParticipant(request.getCallId(), calleeUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found or access denied"));

        if (!call.isGroupCall() && call.getStatus() != ECallStatus.RINGING) {
            throw new IllegalStateException("Call is no longer ringing");
        }

        ECallStatus newStatus = Boolean.TRUE.equals(request.getAccepted()) ? ECallStatus.ACCEPTED
                : ECallStatus.REJECTED;
        LocalDateTime now = LocalDateTime.now();

        if (!call.isGroupCall()) {
            if (newStatus == ECallStatus.ACCEPTED)
                call.setAnsweredAt(now);
            else
                call.setEndedAt(now);
            call.setStatus(newStatus);
            callRepository.save(call);
        } else {
            CallParticipantModel cp = callParticipantRepository.findByCallIdAndUserId(call.getId(), calleeUserId)
                    .orElse(CallParticipantModel.builder().call(call).userId(calleeUserId).build());
            cp.setStatus(newStatus);
            if (newStatus == ECallStatus.ACCEPTED)
                cp.setJoinedAt(now);
            callParticipantRepository.save(cp);

            if (newStatus == ECallStatus.ACCEPTED && call.getStatus() == ECallStatus.RINGING) {
                call.setStatus(ECallStatus.ACCEPTED);
                call.setAnsweredAt(now);
                callRepository.save(call);
            }
        }

        UserServiceProto.UserDTOResponse calleeInfo = userGrpcClient.getUserById(calleeUserId);
        final MessageDTO[] savedCallMessageDTO = { null };
        if (!call.isGroupCall() && newStatus == ECallStatus.REJECTED) {
            savedCallMessageDTO[0] = saveCallMessage(call, ECallStatus.REJECTED, null, now);
        }

        return CallResponseDTO.builder()
                .callId(call.getId())
                .conversationId(call.getConversationId())
                .responderId(calleeUserId)
                .responderUsername(calleeInfo.getUsername())
                .status(newStatus)
                .isGroupCall(call.isGroupCall())
                .callMessage(savedCallMessageDTO[0])
                .build();
    }

    @Override
    public CallResponseDTO joinActiveCall(Long callId, Long userId) {
        CallModel call = callRepository.findByIdAndParticipant(callId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found or access denied"));
        if (!call.isGroupCall()
                || (call.getStatus() != ECallStatus.RINGING && call.getStatus() != ECallStatus.ACCEPTED)) {
            throw new IllegalStateException("Call is not active or not a group call");
        }

        LocalDateTime now = LocalDateTime.now();
        CallParticipantModel cp = callParticipantRepository.findByCallIdAndUserId(callId, userId)
                .orElse(CallParticipantModel.builder().call(call).userId(userId).build());
        cp.setStatus(ECallStatus.ACCEPTED);
        cp.setJoinedAt(now);
        callParticipantRepository.save(cp);

        if (call.getStatus() == ECallStatus.RINGING) {
            call.setStatus(ECallStatus.ACCEPTED);
            call.setAnsweredAt(now);
            callRepository.save(call);
        }

        UserServiceProto.UserDTOResponse calleeInfo = userGrpcClient.getUserById(userId);
        return CallResponseDTO.builder()
                .callId(call.getId())
                .conversationId(call.getConversationId())
                .responderId(userId)
                .responderUsername(calleeInfo.getUsername())
                .status(ECallStatus.ACCEPTED)
                .isGroupCall(true)
                .build();
    }

    @Override
    public CallSignalDTO relaySignal(CallSignalRequest request, Long senderUserId) {
        CallModel call = callRepository.findByIdAndParticipant(request.getCallId(), senderUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found or access denied"));

        if (call.getStatus() != ECallStatus.ACCEPTED && call.getStatus() != ECallStatus.RINGING) {
            throw new IllegalStateException("Call is not active (status: " + call.getStatus() + ")");
        }

        return CallSignalDTO.builder()
                .callId(call.getId())
                .senderId(senderUserId)
                .targetUserId(request.getTargetUserId())
                .isGroupCall(call.isGroupCall())
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

        if (call.isGroupCall()) {
            callParticipantRepository.findByCallIdAndUserId(call.getId(), userId).ifPresent(cp -> {
                cp.setStatus(ECallStatus.ENDED);
                cp.setLeftAt(now);
                callParticipantRepository.save(cp);
            });

            boolean activeRemain = callParticipantRepository.findByCallId(call.getId()).stream()
                    .anyMatch(cp -> cp.getStatus() == ECallStatus.ACCEPTED || cp.getStatus() == ECallStatus.RINGING);
            if (activeRemain && (!userId.equals(call.getCallerUserId()) || !Boolean.TRUE.equals(request.getEndForAll()))) {
                return CallEndedDTO.builder()
                        .callId(call.getId())
                        .conversationId(call.getConversationId())
                        .endedByUserId(userId)
                        .finalStatus(ECallStatus.ENDED)
                        .endedAt(now)
                        .build();
            }
            finalStatus = ECallStatus.ENDED;
        } else {
            if (call.getStatus() == ECallStatus.RINGING) {
                finalStatus = userId.equals(call.getCallerUserId()) ? ECallStatus.ENDED : ECallStatus.MISSED;
            } else if (call.getStatus() == ECallStatus.ACCEPTED) {
                finalStatus = ECallStatus.ENDED;
            } else {
                finalStatus = call.getStatus();
            }
        }

        final Long durationSeconds = call.getAnsweredAt() != null
                ? ChronoUnit.SECONDS.between(call.getAnsweredAt(), now)
                : null;

        call.setStatus(finalStatus);
        call.setEndedAt(now);
        call.setDurationSeconds(durationSeconds);
        callRepository.save(call);

        MessageDTO savedMsg = saveCallMessage(call, finalStatus, durationSeconds, now);

        return CallEndedDTO.builder()
                .callId(call.getId())
                .conversationId(call.getConversationId())
                .endedByUserId(userId)
                .finalStatus(finalStatus)
                .durationSeconds(durationSeconds)
                .endedAt(now)
                .callMessage(savedMsg)
                .build();
    }

    private MessageDTO saveCallMessage(CallModel call, ECallStatus status, Long durationSeconds, LocalDateTime now) {
        final MessageDTO[] saved = { null };
        conversationRepository.findById(call.getConversationId()).ifPresent(conversation -> {
            String content = buildCallMessageContent(call.getCallType(), status, durationSeconds);
            MessageModel callMessage = MessageModel.builder()
                    .conversation(conversation)
                    .senderUserId(call.getCallerUserId())
                    .content(content)
                    .messageType(EMessageType.CALL)
                    .isActive(true)
                    .build();
            messageRepository.save(callMessage);
            conversation.setLastMessageId(callMessage.getId());
            conversation.setLastMessageAt(now);
            conversationRepository.save(conversation);

            UserServiceProto.UserDTOResponse callerInfo = userGrpcClient.getUserById(call.getCallerUserId());
            UserDTO senderDTO = UserDTO.builder()
                    .id(call.getCallerUserId())
                    .username(callerInfo.getUsername())
                    .fullName(callerInfo.getFullName())
                    .avatarUrl(callerInfo.getAvatar())
                    .build();
            saved[0] = MessageDTO.builder()
                    .id(callMessage.getId())
                    .conversationId(conversation.getId())
                    .sender(senderDTO)
                    .content(content)
                    .messageType(EMessageType.CALL)
                    .mediaList(List.of())
                    .reactions(List.of())
                    .createdAt(callMessage.getCreatedAt() != null ? callMessage.getCreatedAt() : now)
                    .build();
        });
        return saved[0];
    }

    private String buildCallMessageContent(ECallType callType, ECallStatus status, Long durationSeconds) {
        String type = callType == ECallType.VIDEO_CALL ? "Video call" : "Cuộc gọi thoại";
        return switch (status) {
            case MISSED -> type + "|MISSED|0";
            case REJECTED -> type + "|REJECTED|0";
            case ENDED -> {
                long mins = durationSeconds != null ? durationSeconds / 60 : 0;
                long secs = durationSeconds != null ? durationSeconds % 60 : 0;
                yield type + "|ENDED|" + String.format("%02d:%02d", mins, secs);
            }
            default -> type + "|ENDED|0";
        };
    }

    @Override
    public Long getOtherParticipantId(Long callId, Long currentUserId) {
        CallModel call = callRepository.findByIdAndParticipant(callId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found or access denied"));
        return call.getCallerUserId().equals(currentUserId) ? call.getCalleeUserId() : call.getCallerUserId();
    }

    @Scheduled(fixedDelay = 30000)
    public void cleanupGhostCalls() {
        LocalDateTime timeoutLimit = LocalDateTime.now().minusSeconds(45);
        List<CallModel> ringingCalls = callRepository.findAll().stream()
                .filter(c -> c.getStatus() == ECallStatus.RINGING && c.getStartedAt().isBefore(timeoutLimit))
                .toList();

        for (CallModel call : ringingCalls) {
            call.setStatus(ECallStatus.MISSED);
            call.setEndedAt(LocalDateTime.now());
            callRepository.save(call);
            saveCallMessage(call, ECallStatus.MISSED, 0L, LocalDateTime.now());
            log.info("Cleaned up ghost ringing call ID: {}", call.getId());
        }
    }

    @Override
    public List<CallEndedDTO> handleUserDisconnect(Long userId) {
        List<CallEndedDTO> endedCalls = new ArrayList<>();
        List<CallParticipantModel> activeParticipations = callParticipantRepository.findByUserId(userId).stream()
                .filter(cp -> cp.getStatus() == ECallStatus.RINGING || cp.getStatus() == ECallStatus.ACCEPTED)
                .toList();

        for (CallParticipantModel cp : activeParticipations) {
            CallModel call = cp.getCall();
            if (call.getStatus() != ECallStatus.RINGING && call.getStatus() != ECallStatus.ACCEPTED) {
                continue;
            }

            CallEndRequest request = new CallEndRequest();
            request.setCallId(call.getId());
            try {
                CallEndedDTO dto = endCall(request, userId);
                if (dto != null) {
                    endedCalls.add(dto);
                }
                log.info("Auto-ended/left call {} for disconnected user {}", call.getId(), userId);
            } catch (Exception e) {
                log.error("Failed to auto-end/leave call {} on disconnect: {}", call.getId(), e.getMessage());
            }
        }
        return endedCalls;
    }
}
