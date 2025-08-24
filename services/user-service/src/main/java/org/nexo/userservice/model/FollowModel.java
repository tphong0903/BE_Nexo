package org.nexo.userservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "follow")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowModel {

    @EmbeddedId
    private FollowId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("followerId")
    @JoinColumn(name = "follower_id")
    private UserModel follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("followingId")
    @JoinColumn(name = "following_id")
    private UserModel following;

    @Column(name = "is_close_friend")
    private Boolean isCloseFriend;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

}
