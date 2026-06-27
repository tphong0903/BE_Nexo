package org.nexo.messagingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.messagingservice.dto.CallEndedDTO;
import org.nexo.messagingservice.dto.MessageDTO;
import org.nexo.messagingservice.dto.UserDTO;
import org.nexo.messagingservice.enums.ECallStatus;
import org.nexo.messagingservice.enums.EMessageType;
import org.nexo.messagingservice.enums.ECallType;
import org.nexo.messagingservice.grpc.UserGrpcClient;
import org.nexo.messagingservice.model.CallModel;
import org.nexo.messagingservice.model.CallParticipantModel;
import org.nexo.messagingservice.model.ConversationModel;
import org.nexo.messagingservice.model.MessageModel;
import org.nexo.messagingservice.repository.CallParticipantRepository;
import org.nexo.messagingservice.repository.CallRepository;
import org.nexo.messagingservice.repository.ConversationRepository;
import org.nexo.messagingservice.repository.MessageRepository;
import org.nexo.grpc.user.UserServiceProto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Job chạy mỗi 10 giây để phát hiện cuộc gọi RINGING quá lâu (timeout).
 * Trường hợp: caller gọi rồi mất mạng/đóng tab, FE không gửi endCall → cuộc gọi
 * treo mãi ở RINGING. Job này sẽ tự kết thúc cuộc gọi đó với status MISSED và
 * lưu tin nhắn lịch sử.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CallTimeoutJob {

    private final CallRepository callRepository;
    private final CallParticipantRepository callParticipantRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserGrpcClient userGrpcClient;

    @Value("${call.ringing-timeout-seconds:30}")
    private int ringingTimeoutSeconds;

    @Scheduled(fixedDelay = 10000) // mỗi 10 giây
    @Transactional
    public void timeoutRingingCalls() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(ringingTimeoutSeconds);

        // Query chỉ lấy các call RINGING đã quá timeout — không load toàn bộ bảng
        List<CallModel> timedOutCalls = callRepository.findByStatusAndStartedAtBefore(
                ECallStatus.RINGING, threshold);

        for (CallModel call : timedOutCalls) {
            try {
                LocalDateTime now = LocalDateTime.now();
                call.setStatus(ECallStatus.MISSED);
                call.setEndedAt(now);
                call.setDurationSeconds(null);
                callRepository.save(call);

                // Đánh dấu tất cả participants là MISSED
                callParticipantRepository.findByCallId(call.getId()).forEach(cp -> {
                    if (cp.getStatus() == ECallStatus.RINGING || cp.getStatus() == ECallStatus.ACCEPTED) {
                        cp.setStatus(ECallStatus.MISSED);
                        cp.setLeftAt(now);
                        callParticipantRepository.save(cp);
                    }
                });

                // Lưu tin nhắn lịch sử cuộc gọi nhỡ
                MessageDTO savedMsg = saveCallMessage(call, now);

                // Broadcast ended event tới tất cả participants
                CallEndedDTO endedDTO = CallEndedDTO.builder()
                        .callId(call.getId())
                        .conversationId(call.getConversationId())
                        .endedByUserId(call.getCallerUserId())
                        .finalStatus(ECallStatus.MISSED)
                        .durationSeconds(null)
                        .endedAt(now)
                        .callMessage(savedMsg)
                        .build();

                callParticipantRepository.findByCallId(call.getId()).forEach(cp -> {
                    messagingTemplate.convertAndSendToUser(
                            cp.getUserId().toString(), "/queue/call/ended", endedDTO);
                });

                // Broadcast tin nhắn vào conversation
                if (savedMsg != null) {
                    messagingTemplate.convertAndSend(
                            "/topic/conversation/" + call.getConversationId(), savedMsg);
                }

                log.info("[CALL-TIMEOUT] Call {} marked as MISSED (ringing since {})",
                        call.getId(), call.getStartedAt());
            } catch (Exception e) {
                log.error("[CALL-TIMEOUT] Error processing call {}: {}", call.getId(), e.getMessage());
            }
        }
    }

    private MessageDTO saveCallMessage(CallModel call, LocalDateTime now) {
        return conversationRepository.findById(call.getConversationId()).map(conversation -> {
            String type = call.getCallType() == ECallType.VIDEO_CALL ? "Video call" : "Cuộc gọi thoại";
            String content = type + "|MISSED|0";

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

            return MessageDTO.builder()
                    .id(callMessage.getId())
                    .conversationId(conversation.getId())
                    .sender(senderDTO)
                    .content(content)
                    .messageType(EMessageType.CALL)
                    .mediaList(List.of())
                    .reactions(List.of())
                    .createdAt(callMessage.getCreatedAt() != null ? callMessage.getCreatedAt() : now)
                    .build();
        }).orElse(null);
    }
}
