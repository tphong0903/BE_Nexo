package org.nexo.postservice.model;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StoryViewModel extends AbstractEntity<Long> {
    private StoryModel storyModel;
    private Long seenUserId;
    private Boolean isLike;
    private LocalDateTime viewAt;
}
