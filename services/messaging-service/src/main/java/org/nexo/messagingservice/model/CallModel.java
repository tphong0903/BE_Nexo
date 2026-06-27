package org.nexo.messagingservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;
import org.nexo.messagingservice.enums.ECallStatus;
import org.nexo.messagingservice.enums.ECallType;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "calls")
public class CallModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "caller_user_id", nullable = false)
    private Long callerUserId;

    @Column(name = "callee_user_id")
    private Long calleeUserId;

    @Column(name = "is_group_call", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean isGroupCall = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", nullable = false)
    private ECallType callType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ECallStatus status;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    // duration in seconds
    @Column(name = "duration_seconds")
    private Long durationSeconds;
}
