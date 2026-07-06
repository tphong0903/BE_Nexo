package org.nexo.interactionservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(
        name = "saved_post",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_saved_post_user_post", columnNames = {"user_id", "post_id"})
        },
        indexes = {
                @Index(name = "idx_saved_post_user_created_at", columnList = "user_id, created_at"),
                @Index(name = "idx_saved_post_post_id", columnList = "post_id")
        }
)
public class SavedPostModel extends AbstractEntity<Long> {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "post_id", nullable = false)
    private Long postId;
}
