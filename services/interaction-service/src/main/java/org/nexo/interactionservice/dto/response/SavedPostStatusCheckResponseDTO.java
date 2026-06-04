package org.nexo.interactionservice.dto.response;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedPostStatusCheckResponseDTO implements Serializable {

    @Builder.Default
    private Map<Long, Boolean> savedStatus = new LinkedHashMap<>();

    @JsonAnyGetter
    public Map<Long, Boolean> asJson() {
        return savedStatus;
    }
}
