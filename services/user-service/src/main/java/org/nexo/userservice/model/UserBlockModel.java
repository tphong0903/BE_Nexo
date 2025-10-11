package org.nexo.userservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_blocks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBlockModel {

    @EmbeddedId
    private UserBlockId id;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}