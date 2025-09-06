package org.nexo.postservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HashTagModel extends AbstractEntity<Long>{

    @Column(unique = true)
    private String name;
    private Long usageCount;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "hashTagModel", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<PostHashTagModel> postHashTagModel;
    private Boolean isActive;
}
