package org.nexo.messagingservice.model;
import org.nexo.messagingservice.enums.EMessageType;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "message_media")
public class MessageMediaModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private MessageModel message;

    @Column(name = "media_url", nullable = false, length = 2048)
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private EMessageType mediaType;
}
