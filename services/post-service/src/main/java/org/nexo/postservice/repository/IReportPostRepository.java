package org.nexo.postservice.repository;

import org.nexo.postservice.model.ReportPostModel;
import org.nexo.postservice.util.Enum.EReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IReportPostRepository extends JpaRepository<ReportPostModel, Long> {
    boolean existsByUserIdAndPostModel_Id(Long userId, Long postId);

    Page<ReportPostModel> findByReportStatus(EReportStatus status, Pageable pageable);

    @Query("""
                SELECT r FROM ReportPostModel r
                WHERE 
                    (:status IS NULL OR r.reportStatus = :status)
                    AND (
                        :keyword IS NULL 
                        OR LOWER(r.reason) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR CAST(r.userId AS string) LIKE CONCAT('%', :keyword, '%')
                    )
            """)
    Page<ReportPostModel> searchReports(
            @Param("status") EReportStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );


}
