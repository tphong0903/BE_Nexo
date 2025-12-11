package org.nexo.userservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.nexo.userservice.enums.EReportStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_reports")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportUserModel {

    @EmbeddedId
    private ReportUserId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("reporterId")
    @JoinColumn(name = "reporter_id")
    private UserModel reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("reportedId")
    @JoinColumn(name = "reported_id")
    private UserModel reported;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EReportStatus status = EReportStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
