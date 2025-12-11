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
public class ReportCommentModel extends AbstractEntity<Long> {

    private Long commentId;
    private Long userId;
    private Long ownerId;
    private String reason;
    private String content;
    private String detail;
    @Enumerated(EnumType.STRING)
    private EReportStatus reportStatus;
    private String reporterName;
    private String ownerCommentName;
    private String note;
}
