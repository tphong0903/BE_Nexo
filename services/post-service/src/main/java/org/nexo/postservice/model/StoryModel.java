package org.nexo.postservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.nexo.postservice.util.Enum.EMediaType;

import java.time.LocalDateTime;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StoryModel extends AbstractEntity<Long> {
    private Long userId;
    private String mediaURL;
    @Enumerated(EnumType.STRING)
    private EMediaType mediaType;
    private Boolean isClosedFriend;
    private LocalDateTime expiresAt;
    private Boolean isArchive;
    private Boolean isActive;
}
