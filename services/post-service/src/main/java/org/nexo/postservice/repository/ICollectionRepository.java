package org.nexo.postservice.repository;

import org.nexo.postservice.model.CollectionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ICollectionRepository extends JpaRepository<CollectionModel, Long> {
    Page<CollectionModel> findByUserId(Long userId, Pageable pageable);

}
