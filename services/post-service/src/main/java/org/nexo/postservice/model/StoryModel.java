package org.nexo.postservice.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.nexo.postservice.util.Enum.EMediaType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StoryModel extends AbstractEntity<Long> {
    private Long userId;
    private String mediaURL;
    @Enumerated(EnumType.STRING)
    private EMediaType mediaType;
    private Boolean isClosedFriend;
    private LocalDateTime expiresAt;
    private Boolean isArchive;
    private Boolean isActive;

    @OneToMany(mappedBy = "storyModel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StoryViewModel> views = new ArrayList<>();
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "storyModel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CollectionItemModel> collectionItemModelList = new ArrayList<>();
}
