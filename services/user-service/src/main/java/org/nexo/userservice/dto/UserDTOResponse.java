package org.nexo.userservice.dto;

import lombok.Data;

@Data
public class UserDTOResponse {
    private Long id;
    private String username;
    private String fullName;
    private String avatar;
    private String bio;
    private Boolean isPrivate;
    private Boolean onlineStatus;
    private Long followers;
    private Long following;

}
