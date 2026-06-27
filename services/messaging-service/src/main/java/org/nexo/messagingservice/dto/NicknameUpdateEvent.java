package org.nexo.messagingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NicknameUpdateEvent {
    private Long conversationId;
    private Long targetUserId;
    private String nickname;
    private List<UserDTO> participants;
}
