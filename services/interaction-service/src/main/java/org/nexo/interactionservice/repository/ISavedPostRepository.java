package org.nexo.interactionservice.repository;

import org.nexo.interactionservice.model.SavedPostModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface ISavedPostRepository extends JpaRepository<SavedPostModel, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    Optional<SavedPostModel> findByUserIdAndPostId(Long userId, Long postId);

    Page<SavedPostModel> findByUserId(Long userId, Pageable pageable);

    List<SavedPostModel> findByUserIdAndPostIdIn(Long userId, List<Long> postIds);
}
