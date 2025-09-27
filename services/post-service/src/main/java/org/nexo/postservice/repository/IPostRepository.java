package org.nexo.postservice.repository;

import org.nexo.postservice.model.PostModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IPostRepository extends JpaRepository<PostModel, Long> {
    List<PostModel> findByUserIdAndIsActive(Long id, Boolean isActive);

    List<PostModel> findByUserId(Long id, Boolean isActive);
}
