package org.nexo.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchEvent {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String avatar;
    private String bio;
    private Boolean isPrivate;
    private String accountStatus;
    private String eventType;
}
