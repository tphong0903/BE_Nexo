package org.nexo.messagingservice.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceUpdateDTO {
    private Long userId;
    private Boolean isOnline;
    private String lastSeen;
    private LocalDateTime timestamp;
}
