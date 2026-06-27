package org.nexo.messagingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nexo.messagingservice.enums.ECallType;

import java.time.LocalDateTime;

/**
 * Sent to the callee when an incoming call arrives.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallNotificationDTO {
    private Long callId;
    private Long conversationId;
    private Long callerId;
    private String callerUsername;
    private String callerFullName;
    private String callerAvatarUrl;
    // VIDEO_CALL (with cam) or AUDIO_CALL (no cam)
    private ECallType callType;
    private boolean isGroupCall;
    private LocalDateTime startedAt;
}
