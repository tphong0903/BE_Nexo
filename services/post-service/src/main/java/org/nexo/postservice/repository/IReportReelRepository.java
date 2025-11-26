package org.nexo.postservice.repository;

import org.nexo.postservice.dto.response.ReportSummaryProjection;
import org.nexo.postservice.model.ReportReelModel;
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
public interface IReportReelRepository extends JpaRepository<ReportReelModel, Long> {
    boolean existsByUserIdAndReelModel_Id(Long userId, Long reelId);

    Page<ReportReelModel> findByReportStatus(EReportStatus status, Pageable pageable);

    @Query("""
                SELECT r FROM ReportReelModel r
                WHERE 
                    (:status IS NULL OR r.reportStatus = :status)
                    AND (
                        :keyword IS NULL 
                        OR LOWER(r.reason) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR CAST(r.userId AS string) LIKE CONCAT('%', :keyword, '%')
                    )
            """)
    Page<ReportReelModel> searchReports(
            @Param("status") EReportStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("SELECT DATE(r.createdAt) AS date, COUNT(r) AS total " +
            "FROM ReportReelModel r " +
            "WHERE r.createdAt BETWEEN :start AND :end " +
            "GROUP BY DATE(r.createdAt) " +
            "ORDER BY DATE(r.createdAt)")
    List<Object[]> countReportsByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);


    @Query(value = """
            SELECT * FROM report_reel_model r 
            WHERE 
                (:status = 'ALL' OR r.report_status = :status)
                AND (
                    :keyword IS NULL 
                    OR r.reason  ILIKE CONCAT('%', :keyword, '%')
                    OR r.owner_post_name  ILIKE CONCAT('%', :keyword, '%')
                    OR r.reporter_name ILIKE CONCAT('%', :keyword, '%')
                )
            ORDER BY r.id DESC
            """,
            nativeQuery = true)
    Page<ReportSummaryProjection> searchReportsReelsNative(
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query(value = """
            SELECT
                SUM(CASE WHEN report_status = 'PENDING' THEN 1 ELSE 0 END) AS pending_count,
                SUM(CASE WHEN report_status = 'IN_REVIEW' THEN 1 ELSE 0 END) AS in_review_count,
                SUM(CASE WHEN report_status = 'APPROVED' THEN 1 ELSE 0 END) AS approved_count,
                SUM(CASE WHEN report_status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected_count
            FROM report_reel_model r
            """,
            nativeQuery = true)
    List<Object[]> getReportQuantitySummary();
}
