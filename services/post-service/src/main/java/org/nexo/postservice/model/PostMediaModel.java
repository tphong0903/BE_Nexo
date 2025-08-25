package org.nexo.postservice.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.nexo.postservice.util.Enum.EMediaType;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PostMediaModel extends AbstractEntity<Long>{
    private String mediaUrl;
    private Integer mediaOrder;
    @Enumerated(EnumType.STRING)
    private EMediaType mediaType;

    @ManyToOne
    @JoinColumn(name = "post_id")
    @JsonIgnore
    private PostModel postModel;

}
