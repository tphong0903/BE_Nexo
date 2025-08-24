package org.nexo.postservice.repository;

import org.nexo.postservice.model.PostMediaModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPostMediaRepository extends JpaRepository<PostMediaModel, Long> {
}
