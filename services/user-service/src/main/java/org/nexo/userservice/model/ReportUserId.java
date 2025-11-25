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
@Check(constraints = "reporter_id <> reported_id")
public class ReportUserId implements Serializable {

    @Column(name = "reporter_id")
    private Long reporterId;

    @Column(name = "reported_id")
    private Long reportedId;
}
