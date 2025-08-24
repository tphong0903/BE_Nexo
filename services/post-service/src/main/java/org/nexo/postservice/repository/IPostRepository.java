package org.nexo.postservice.repository;

import org.nexo.postservice.model.PostModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IPostRepository extends JpaRepository<PostModel, Long> {
}
