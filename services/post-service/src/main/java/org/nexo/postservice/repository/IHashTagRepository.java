package org.nexo.postservice.repository;

import org.nexo.postservice.model.HashTagModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IHashTagRepository extends JpaRepository<HashTagModel, Long> {
    HashTagModel findByName(String name);
}
