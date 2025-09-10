package org.nexo.postservice.repository;

import org.nexo.postservice.model.PostHashTagModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface IPostHashTagRepository extends JpaRepository<PostHashTagModel, Long> {

    @Transactional
    @Modifying
    @Query("DELETE FROM PostHashTagModel post WHERE (post.postModel.id = :postId OR post.reelModel.id = :postId) ")
    void deleteByPostId(@Param("postId") Long postId);
}
