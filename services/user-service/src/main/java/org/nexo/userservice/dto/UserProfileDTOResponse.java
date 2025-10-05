package org.nexo.userservice.dto;

import lombok.Data;

@Data
public class UserProfileDTOResponse {
    private Long id;
    private String username;
    private String fullName;
    private String avatar;
    private String bio;
    private Boolean isPrivate;
    private Boolean isFollowing;
    private Boolean hasRequestedFollow;
    private Long followers;
    private Long following;

}
