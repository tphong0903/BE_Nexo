package org.nexo.postservice.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PostHashTagModel extends AbstractEntity<Long> {
    @ManyToOne
    @JoinColumn(name = "hashtag_id")
    private HashTagModel hashTagModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private PostModel postModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reel_id")
    private ReelModel reelModel;
}
