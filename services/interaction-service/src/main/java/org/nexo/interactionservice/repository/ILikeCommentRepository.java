package org.nexo.interactionservice.repository;

import org.nexo.interactionservice.model.LikeCommentModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ILikeCommentRepository extends JpaRepository<LikeCommentModel, Long> {
    LikeCommentModel findByCommentModelIdAndAndUserId(Long cmtId, Long userId);

    Page<LikeCommentModel> findByCommentModelId(Long id, Pageable pageable);

    Long countByCommentModel_Id(Long id);

    boolean existsByCommentModelIdAndUserId(Long commentId, Long userId);

}
