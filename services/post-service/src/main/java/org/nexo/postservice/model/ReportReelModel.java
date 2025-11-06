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
public class ReportReelModel extends AbstractEntity<Long> {
    @ManyToOne()
    @JoinColumn(name = "reel_id")
    private ReelModel reelModel;
    private String reason;
    private Long userId;
    @Enumerated(EnumType.STRING)
    private EReportStatus reportStatus;
}
