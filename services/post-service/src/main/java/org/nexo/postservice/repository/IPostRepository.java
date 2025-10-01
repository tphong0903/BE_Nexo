package org.nexo.postservice.repository;

import org.nexo.postservice.model.PostModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IPostRepository extends JpaRepository<PostModel, Long> {
    Page<PostModel> findByUserIdAndIsActive(Long id, Boolean isActive, Pageable pageable);

    Page<PostModel> findByUserId(Long id, Pageable pageable);

}
