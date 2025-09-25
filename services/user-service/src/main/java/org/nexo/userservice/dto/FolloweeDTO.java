package org.nexo.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolloweeDTO {
    private Long userId;
    private String userName;
    private String avatar;
    private boolean isCloseFriend;
}