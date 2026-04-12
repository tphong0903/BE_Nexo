package org.nexo.messagingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebRTC signaling payload relayed between caller and callee.
 * type: OFFER | ANSWER | ICE_CANDIDATE
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallSignalRequest {
    private Long callId;
    // OFFER, ANSWER, or ICE_CANDIDATE
    private String type;
    // SDP for OFFER/ANSWER
    private String sdp;
    // ICE candidate JSON string for ICE_CANDIDATE
    private String candidate;
}
