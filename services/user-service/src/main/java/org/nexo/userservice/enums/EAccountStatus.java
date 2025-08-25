package org.nexo.userservice.enums;

public enum EAccountStatus {
    PENDING, // Chờ xác thực email
    ACTIVE, // Đã kích hoạt
    INACTIVE, // Không hoạt động
    BLOCKED // Bị khóa
}