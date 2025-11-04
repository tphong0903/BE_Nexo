package org.nexo.postservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.postservice.dto.DashBoardDateDTO;
import org.nexo.postservice.dto.response.ChartDataDto;
import org.nexo.postservice.dto.response.ResponseData;
import org.nexo.postservice.service.IDashboardService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin")
@Validated
@Slf4j
@RequiredArgsConstructor
public class DashboardController {
    private final IDashboardService dashboardService;

    @GetMapping("/dashboard")
    public ResponseData<?> getDashBoardData() {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", dashboardService.getDashboardData());
    }

    @PostMapping("/dashboard/posts")
    public ResponseData<?> getPostsByTime(@RequestBody DashBoardDateDTO dashboardDateDTO) {
        LocalDate startDate = dashboardDateDTO.getStartDate();
        LocalDate endDate = dashboardDateDTO.getEndDate();
        ChartDataDto dashboardResponse = dashboardService.getPostsByTime(startDate, endDate);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", dashboardResponse);

    }

    @PostMapping("/dashboard/interactions")
    public ResponseData<?> getInteractionsByTime(@RequestBody DashBoardDateDTO dashboardDateDTO) {
        LocalDate startDate = dashboardDateDTO.getStartDate();
        LocalDate endDate = dashboardDateDTO.getEndDate();
        ChartDataDto dashboardResponse = dashboardService.getInteractionsByTime(startDate, endDate);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", dashboardResponse);

    }

    @PostMapping("/dashboard/users")
    public ResponseData<?> getUsersByTime(@RequestBody DashBoardDateDTO dashboardDateDTO) {
        LocalDate startDate = dashboardDateDTO.getStartDate();
        LocalDate endDate = dashboardDateDTO.getEndDate();
        ChartDataDto dashboardResponse = dashboardService.getUsersByTime(startDate, endDate);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", dashboardResponse);

    }

    @PostMapping("/dashboard/reports")
    public ResponseData<?> getReportsByTime(@RequestBody DashBoardDateDTO dashboardDateDTO) {
        LocalDate startDate = dashboardDateDTO.getStartDate();
        LocalDate endDate = dashboardDateDTO.getEndDate();
        ChartDataDto dashboardResponse = dashboardService.getReportsByTime(startDate, endDate);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", dashboardResponse);

    }
}
