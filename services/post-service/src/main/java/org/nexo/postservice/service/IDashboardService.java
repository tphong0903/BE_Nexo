package org.nexo.postservice.service;

import org.nexo.postservice.dto.response.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface IDashboardService {
    DashboardResponseDto getDashboardData();

    ChartDataDto getPostsByTime(LocalDate startDate, LocalDate endDate);

    ChartDataDto getUsersByTime(LocalDate startDate, LocalDate endDate);

    ChartDataDto getInteractionsByTime(LocalDate startDate, LocalDate endDate);

    ChartDataDto getReportsByTime(LocalDate startDate, LocalDate endDate);

    PageModelResponse getAllPost(String search, int page, int limit, String type);

    PostManagementInfo getPostManagementInfo();


}
