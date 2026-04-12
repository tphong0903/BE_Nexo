package org.nexo.interactionservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedPostStatusCheckRequestDTO implements Serializable {

    @NotEmpty(message = "postIds must not be empty")
    @Size(max = 100, message = "postIds size must not exceed 100")
    private List<@NotNull(message = "postIds must not contain null values") Long> postIds;
}
