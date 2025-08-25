package org.nexo.postservice.repository;

import org.nexo.postservice.model.PostMediaModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IPostMediaRepository extends JpaRepository<PostMediaModel, Long> {

    List<PostMediaModel> findAllByPostModel_Id(Long postId);
    List<PostMediaModel> findAllByPostModel_IdAndMediaUrl(Long postId,String url);
}
