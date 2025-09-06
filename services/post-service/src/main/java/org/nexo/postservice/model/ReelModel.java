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
public class ReelModel extends AbstractPost{
    private String videoUrl;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "reelModel", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<PostHashTagModel> postHashTagModel;
}
