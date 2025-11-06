package org.nexo.postservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class ChartDataDto implements Serializable {
    private List<String> time;
    private List<Long> data;
}
