package org.nexo.authservice.dto;

import lombok.Data;

@Data
public class SyncUserRequest {
    private String email;
    private String username;
    private String fullname;
    private String defaultPassword;
}