package org.nexo.interactionservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CommentModel extends AbstractEntity<Long> {
    private Long userId;
    private Long postId;
    private Long reelId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private CommentModel parentComment;
    private String content;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "commentModel", cascade = CascadeType.ALL)
    private List<CommentMentionModel> mentionList;
}
