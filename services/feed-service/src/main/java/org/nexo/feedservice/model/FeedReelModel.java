package org.nexo.feedservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FeedReelModel extends AbstractEntity<Long> {
    @Column(nullable = false)
    private Long reelId;

    @Column(nullable = false)
    private Long followerId;

    @Column(nullable = false)
    private Long userId;
}
