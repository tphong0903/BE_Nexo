package org.nexo.messagingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nexo.messagingservice.enums.ECallStatus;

import java.time.LocalDateTime;

/**
 * Broadcast to both participants when a call ends.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallEndedDTO {
    private Long callId;
    private Long endedByUserId;
    private ECallStatus finalStatus;
    // duration in seconds (null if call was never answered)
    private Long durationSeconds;
    private LocalDateTime endedAt;
}
