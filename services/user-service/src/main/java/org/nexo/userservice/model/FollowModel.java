package org.nexo.userservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "follow")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowModel {

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    protected LocalDateTime createdAt;
    @EmbeddedId
    private FollowId id;
    @ManyToOne()
    @MapsId("followerId")
    @JoinColumn(name = "follower_id")
    private UserModel follower;
    @ManyToOne()
    @MapsId("followingId")
    @JoinColumn(name = "following_id")
    private UserModel following;
    @Column(name = "is_close_friend")
    private Boolean isCloseFriend;

}
