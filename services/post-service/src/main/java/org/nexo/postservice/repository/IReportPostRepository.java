package org.nexo.postservice.repository;

import org.nexo.postservice.dto.ReportCountProjection;
import org.nexo.postservice.dto.response.ReportSummaryProjection;
import org.nexo.postservice.model.ReportPostModel;
import org.nexo.postservice.util.Enum.EReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IReportPostRepository extends JpaRepository<ReportPostModel, Long> {

    boolean existsByUserIdAndPostModel_Id(Long userId, Long postId);

    ReportPostModel findByUserIdAndPostModel_Id(Long userId, Long postId);

    Page<ReportPostModel> findByReportStatus(EReportStatus status, Pageable pageable);

    @Query(value = """
            SELECT 
                r.id as id, 
                r.reason as reason, 
                r.report_status as reportStatus, 
                r.created_at as createdAt,
                r.owner_post_name as ownerName,
                r.reporter_name as reporterName,
                r.predictai as predictAI,        
                r.confidence as confidence
            FROM report_post_model r 
            WHERE 
                (:status = 'ALL' OR r.report_status = :status)
                AND (
                    :keyword IS NULL OR :keyword = ''
                    OR r.reason ILIKE %:keyword%
                    OR r.owner_post_name ILIKE %:keyword%
                    OR r.reporter_name ILIKE %:keyword%
                )
            """,
            countQuery = """
                    SELECT count(*) FROM report_post_model r 
                    WHERE (:status = 'ALL' OR r.report_status = :status)
                    AND (:keyword IS NULL OR :keyword = '' OR r.reason ILIKE %:keyword%)
                    """,
            nativeQuery = true)
    Page<ReportSummaryProjection> searchReportPostsNative(
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("SELECT CAST(r.createdAt AS date) as reportDate, COUNT(r) " +
            "FROM ReportPostModel r " +
            "WHERE r.createdAt BETWEEN :start AND :end " +
            "GROUP BY CAST(r.createdAt AS date) " +
            "ORDER BY reportDate ASC")
    List<Object[]> countReportsByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
            SELECT
                COUNT(*) FILTER (WHERE report_status = 'PENDING') AS pendingCount,
                COUNT(*) FILTER (WHERE report_status = 'IN_REVIEW') AS inReviewCount,
                COUNT(*) FILTER (WHERE report_status = 'APPROVED') AS approvedCount,
                COUNT(*) FILTER (WHERE report_status = 'REJECTED') AS rejectedCount
            FROM report_post_model
            """, nativeQuery = true)
    ReportCountProjection getReportQuantitySummary();
}
