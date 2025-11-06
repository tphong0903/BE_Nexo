package org.nexo.userservice.model;

import org.hibernate.annotations.SQLRestriction;
import org.nexo.userservice.enums.EAccountStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UserModel {

    public static final String DEFAULT_AVATAR_URL = "https://www.wins.org/wp-content/themes/psBella/assets/img/fallbacks/user-avatar.jpg";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keycloak_user_id", unique = true)
    private String keycloakUserId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Column(name = "fullname", nullable = false)
    private String fullName;

    @Column(name = "avatar_url", length = 1024)
    @Builder.Default
    private String avatar = DEFAULT_AVATAR_URL;

    @Column(name = "bio")
    private String bio;

    @Column(name = "is_private")
    @Builder.Default
    private Boolean isPrivate = false;

    @Column(name = "online_status")
    @Builder.Default
    private Boolean onlineStatus = true;

    @Column(name = "account_status")
    @Enumerated(EnumType.STRING)
    private EAccountStatus accountStatus;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "follower", cascade = CascadeType.ALL, orphanRemoval = true)
    @SQLRestriction("status = 'ACTIVE'")
    private List<FollowModel> following;
    @OneToMany(mappedBy = "following", cascade = CascadeType.ALL, orphanRemoval = true)
    @SQLRestriction("status = 'ACTIVE'")
    private List<FollowModel> followers;
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserActivityLogModel> activityLogs;

    public boolean isDefaultAvatar() {
        return DEFAULT_AVATAR_URL.equals(this.avatar);
    }

}
