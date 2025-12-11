package org.nexo.postservice.repository;

import org.nexo.postservice.model.CollectionItemModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ICollectionItemRepository extends JpaRepository<CollectionItemModel, Long> {

}
