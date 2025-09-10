package org.nexo.interactionservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LikeCommentModel extends AbstractEntity<Long> {
    private Long userId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private CommentModel commentModel;
}
