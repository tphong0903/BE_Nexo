package org.nexo.messagingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallResponseRequest {
    private Long callId;
    // true = accepted, false = rejected
    private Boolean accepted;
}
