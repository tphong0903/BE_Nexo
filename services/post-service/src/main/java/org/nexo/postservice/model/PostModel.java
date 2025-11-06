package org.nexo.postservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
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
public class PostModel extends AbstractPost {
    private String tag;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "postModel", cascade = CascadeType.ALL)
    private List<PostHashTagModel> postHashTagModel;
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "postModel", cascade = CascadeType.ALL)
    private List<PostMediaModel> postMediaModels;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "postModel", cascade = CascadeType.ALL)
    private List<ReportPostModel> reportPostModels;
}
