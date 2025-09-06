package org.nexo.postservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.nexo.postservice.util.Enum.EMediaType;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PostHashTagModel extends AbstractEntity<Long> {
    @ManyToOne
    @JoinColumn(name = "hashtag_id")
    @JsonIgnore
    private HashTagModel hashTagModel;

    @ManyToOne
    @JoinColumn(name = "post_id")
    @JsonIgnore
    private PostModel postModel;

    @ManyToOne
    @JoinColumn(name = "reel_id")
    @JsonIgnore
    private ReelModel reelModel;
}
