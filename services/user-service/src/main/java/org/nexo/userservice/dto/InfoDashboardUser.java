package org.nexo.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfoDashboardUser {
    private Long totalUsers;
    private Long totalUsersActive;
    private Long totalUsersLocked;
    private Long totalUsersPending;
}
