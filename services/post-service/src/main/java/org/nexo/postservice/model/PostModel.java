package org.nexo.postservice.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PostModel extends AbstractPost {
    private String tag;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "postModel", cascade = CascadeType.ALL)
    private List<PostHashTagModel> postHashTagModel;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "postModel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostMediaModel> postMediaModels;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "postModel", cascade = CascadeType.ALL)
    private List<ReportPostModel> reportPostModels;
}
