package org.nexo.userservice.repository;

import org.nexo.userservice.model.ReportUserId;
import org.nexo.userservice.model.ReportUserModel;
import org.nexo.userservice.enums.EReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportUserRepository extends JpaRepository<ReportUserModel, ReportUserId> {

    boolean existsById(ReportUserId id);

    Optional<ReportUserModel> findById(ReportUserId id);

    Page<ReportUserModel> findByIdReporterId(Long reporterId, Pageable pageable);

    Page<ReportUserModel> findByIdReportedId(Long reportedId, Pageable pageable);

    Page<ReportUserModel> findByStatus(EReportStatus status, Pageable pageable);

    @Query("SELECT r FROM ReportUserModel r " +
            "JOIN UserModel u ON r.id.reportedId = u.id " +
            "WHERE r.id.reporterId = :reporterId " +
            "AND (:search IS NULL OR :search = '' OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ReportUserModel> findByIdReporterIdWithSearch(@Param("reporterId") Long reporterId,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT r FROM ReportUserModel r " +
            "JOIN UserModel u ON r.id.reporterId = u.id " +
            "WHERE r.id.reportedId = :reportedId " +
            "AND (:search IS NULL OR :search = '' OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ReportUserModel> findByIdReportedIdWithSearch(@Param("reportedId") Long reportedId,
            @Param("search") String search,
            Pageable pageable);
}
