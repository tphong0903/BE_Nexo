package org.nexo.postservice.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CollectionModel extends AbstractEntity<Long> {
    private String collectionName;
    private Long userId;
    @OneToMany(mappedBy = "collectionModel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CollectionItemModel> collectionItemModelList = new ArrayList<>();
    ;
}
