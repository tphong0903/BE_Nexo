package org.nexo.userservice.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_activity_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityLogModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserModel user;

    @Column(nullable = false, length = 100)
    private String action;

    @Lob
    @Column(name = "details_json")
    private String detailsJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}