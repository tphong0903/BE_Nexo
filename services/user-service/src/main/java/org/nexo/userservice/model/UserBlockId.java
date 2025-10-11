package org.nexo.userservice.model;

import lombok.*;
import java.io.Serializable;

import org.hibernate.annotations.Check;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
@Check(constraints = "blocker_id <> blocked_id")
public class UserBlockId implements Serializable {

    @Column(name = "blocker_id")
    private Long blockerId;

    @Column(name = "blocked_id")
    private Long blockedId;
}