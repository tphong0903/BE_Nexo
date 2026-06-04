package org.nexo.messagingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nexo.messagingservice.enums.ECallType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallInitiateRequest {
    private Long conversationId;
    // VIDEO_CALL (with cam) or AUDIO_CALL (no cam)
    private ECallType callType;
}
