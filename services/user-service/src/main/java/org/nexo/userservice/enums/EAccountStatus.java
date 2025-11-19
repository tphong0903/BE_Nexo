package org.nexo.userservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EAccountStatus {
    PENDING, // Chờ xác thực email
    ACTIVE, // Đã kích hoạt
    INACTIVE, // Không hoạt động
    LOCKED; // Bị khóa

    @JsonCreator
    public static EAccountStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ACTIVE; 
        }
        try {
            return EAccountStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ACTIVE; 
        }
    }
}