package org.nexo.postservice.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.nexo.postservice.util.Enum.EVisibilityPost;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractPost implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    protected Long id;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    protected LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    protected LocalDateTime updatedAt;
    private Long userId;
    private String caption;
    @Enumerated(EnumType.STRING)
    private EVisibilityPost visibility;
}
