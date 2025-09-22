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
    private String userId;
    private String userName;
    private String avatar;
    private boolean isCloseFriend;
}