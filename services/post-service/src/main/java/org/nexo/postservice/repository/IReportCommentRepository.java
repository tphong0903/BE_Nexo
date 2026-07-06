package org.nexo.postservice.repository;

import org.nexo.postservice.dto.ReportCountProjection;
import org.nexo.postservice.dto.response.ReportSummaryProjection;
import org.nexo.postservice.model.ReportCommentModel;
import org.nexo.postservice.util.Enum.EReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface IReportCommentRepository extends JpaRepository<ReportCommentModel, Long> {

    boolean existsByUserIdAndCommentId(Long userId, Long commentId);

    ReportCommentModel findByUserIdAndCommentId(Long userId, Long commentId);

    Page<ReportCommentModel> findByReportStatus(EReportStatus status, Pageable pageable);

    @Query(value = """
            SELECT 
                r.id as id, 
                r.reason as reason, 
                r.report_status as reportStatus, 
                r.created_at as createdAt,
                r.owner_comment_name as ownerName,
                r.reporter_name as reporterName,
                r.predictai as predictAI,        
                r.confidence as confidence
                        
            FROM report_comment_model r 
            WHERE 
                (:status = 'ALL' OR r.report_status = :status)
                AND (
                    :keyword IS NULL OR :keyword = ''
                    OR r.reason ILIKE %:keyword%
                    OR r.owner_comment_name ILIKE %:keyword%
                    OR r.reporter_name ILIKE %:keyword%
                )
            """,
            countQuery = """
                    SELECT count(*) FROM report_comment_model r 
                    WHERE (:status = 'ALL' OR r.report_status = :status)
                    AND (
                        :keyword IS NULL OR :keyword = '' 
                        OR r.reason ILIKE %:keyword% 
                        OR r.owner_comment_name ILIKE %:keyword% 
                        OR r.reporter_name ILIKE %:keyword%
                    )
                    """,
            nativeQuery = true)
    Page<ReportSummaryProjection> searchReportCommentsNative(
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("SELECT CAST(r.createdAt AS date) AS reportDate, COUNT(r) " +
            "FROM ReportCommentModel r " +
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
            FROM report_comment_model
            """, nativeQuery = true)
    ReportCountProjection getReportQuantitySummary();
}