package org.nexo.postservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.nexo.postservice.util.Enum.EMediaType;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class PostMediaModel extends AbstractEntity<Long> {
    private String mediaUrl;
    private Integer mediaOrder;
    @Enumerated(EnumType.STRING)
    private EMediaType mediaType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    @JsonIgnore
    private PostModel postModel;

}
