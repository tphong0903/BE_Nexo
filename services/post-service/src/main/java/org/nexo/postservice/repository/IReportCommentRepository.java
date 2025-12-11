package org.nexo.postservice.repository;

import org.nexo.postservice.dto.response.ReportSummaryProjection;
import org.nexo.postservice.model.ReportCommentModel;
import org.nexo.postservice.model.ReportPostModel;
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

    Page<ReportPostModel> findByReportStatus(EReportStatus status, Pageable pageable);

    @Query(value = """
            SELECT * FROM report_comment_model r 
            WHERE 
                (:status = 'ALL' OR r.report_status = :status)
                AND (
                    :keyword IS NULL 
                    OR r.reason  ILIKE CONCAT('%', :keyword, '%')
                    OR r.owner_comment_name  ILIKE CONCAT('%', :keyword, '%')
                    OR r.reporter_name ILIKE CONCAT('%', :keyword, '%')
                )
            ORDER BY r.id DESC
            """,
            nativeQuery = true)
    Page<ReportSummaryProjection> searchReportCommentsNative(
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("SELECT DATE(r.createdAt) AS date, COUNT(r) AS total " +
            "FROM ReportPostModel r " +
            "WHERE r.createdAt BETWEEN :start AND :end " +
            "GROUP BY DATE(r.createdAt) " +
            "ORDER BY DATE(r.createdAt)")
    List<Object[]> countReportsByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
            SELECT
                COUNT(*) FILTER (WHERE report_status = 'PENDING') AS pending_count,
                COUNT(*) FILTER (WHERE report_status = 'IN_REVIEW') AS in_review_count,
                COUNT(*) FILTER (WHERE report_status = 'APPROVED') AS approved_count,
                COUNT(*) FILTER (WHERE report_status = 'REJECTED') AS rejected_count
            FROM report_comment_model r
            """,
            nativeQuery = true)
    List<Object[]> getReportQuantitySummary();
}
