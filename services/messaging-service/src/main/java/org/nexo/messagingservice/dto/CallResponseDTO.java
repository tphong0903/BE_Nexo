package org.nexo.messagingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nexo.messagingservice.enums.ECallStatus;

/**
 * Sent to the caller after callee accepts or rejects.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallResponseDTO {
    private Long callId;
    private Long conversationId;
    private Long responderId;
    private String responderUsername;
    private ECallStatus status;
    private boolean isGroupCall;
    private MessageDTO callMessage;
}
