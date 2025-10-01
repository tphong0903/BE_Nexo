package org.nexo.userservice.dto;

import lombok.Data;

@Data
public class PrivateUserDTOResponse  {
    private Long id;
    private String username;
    private String fullName;
    private String avatar;
    private String bio;

}
