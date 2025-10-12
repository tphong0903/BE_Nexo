package org.nexo.postservice.model;

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
public class CollectionItemModel extends AbstractEntity<Long> {
    @ManyToOne()
    @JoinColumn(name = "story_id", nullable = false)
    private StoryModel storyModel;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false)
    private CollectionModel collectionModel;
}
