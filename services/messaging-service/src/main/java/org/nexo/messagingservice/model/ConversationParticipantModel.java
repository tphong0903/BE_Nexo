package org.nexo.messagingservice.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

@Data
@Entity
@Table(name = "conversation_participants", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "conversation_id", "user_id" })
})
public class ConversationParticipantModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationModel conversation;

    @JoinColumn(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "nickname", length = 100)
    private String nickname;
    
    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "is_muted", nullable = false)
    private boolean isMuted = false;

    @Column(name = "is_archived", nullable = false)
    private boolean isArchived = false;

}
