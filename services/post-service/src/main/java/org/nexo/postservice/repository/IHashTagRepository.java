package org.nexo.postservice.repository;

import org.nexo.postservice.model.HashTagModel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IHashTagRepository extends JpaRepository<HashTagModel, Long> {
    HashTagModel findByName(String name);

    @Query("""
                SELECT h FROM HashTagModel h
                WHERE h.isActive = true
                ORDER BY h.usageCount DESC
            """)
    List<HashTagModel> findTopTrendingHashtags(Pageable pageable);
}
