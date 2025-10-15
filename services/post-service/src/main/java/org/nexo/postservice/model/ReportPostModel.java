package org.nexo.postservice.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.nexo.postservice.util.Enum.EReportStatus;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReportPostModel extends AbstractEntity<Long> {
    @ManyToOne()
    @JoinColumn(name = "post_id")
    private PostModel postModel;
    private Long userId;
    private String reason;
    @Enumerated(EnumType.STRING)
    private EReportStatus reportStatus;
}
