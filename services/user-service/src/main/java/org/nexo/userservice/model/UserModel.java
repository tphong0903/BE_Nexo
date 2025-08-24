package org.nexo.userservice.model;

import org.nexo.userservice.enums.EAccountStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keycloak_user_id", unique = true)
    private String keycloakUserId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "fullname", nullable = false)
    private String fullName;

    @Column(name = "online_status")
    private Boolean onlineStatus;

    @Column(name = "account_status")
    @Enumerated(EnumType.STRING)
    private EAccountStatus accountStatus;

    @Lob
    private String settingsJson;

    @Column(name = "last_login")
    private OffsetDateTime lastLogin;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "follower", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FollowModel> following;

    @OneToMany(mappedBy = "following", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FollowModel> followers;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserActivityLogModel> activityLogs;

}
