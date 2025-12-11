package org.nexo.userservice.dto;

import org.nexo.userservice.enums.EAccountStatus;

import lombok.Data;

@Data
public class UserProfileMeDTO {
    private Long id;
    private String username;
    private String fullName;
    private String avatar;
    private String bio;
    private Boolean isPrivate;
    private Boolean onlineStatus;
    private Long followers;
    private Long following;
    private EAccountStatus accountStatus;
}
