package org.nexo.interactionservice.model;

import jakarta.persistence.Entity;
import lombok.*;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LikeModel extends AbstractEntity<Long> {
    private Long userId;
    private Long postId;
    private Long reelId;
}
