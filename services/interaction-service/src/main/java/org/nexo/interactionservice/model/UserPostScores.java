package org.nexo.interactionservice.model;

import jakarta.persistence.Entity;
import lombok.*;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserPostScores extends AbstractEntity<Long> {
    private Long userId;
    private Long postId;
    private Long reelId;
    private Double scores;
}
