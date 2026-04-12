package org.nexo.messagingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebRTC signal relayed from one peer to the other.
 * type: OFFER | ANSWER | ICE_CANDIDATE
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallSignalDTO {
    private Long callId;
    private Long senderId;
    private String type;
    private String sdp;
    private String candidate;
}
