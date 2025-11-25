package org.nexo.userservice.service.Impl;

import java.util.List;
import java.util.stream.Collectors;

import org.nexo.userservice.dto.CreateReportRequest;
import org.nexo.userservice.dto.PageModelResponse;
import org.nexo.userservice.dto.ReportUserResponse;
import org.nexo.userservice.dto.UpdateReportStatusRequest;
import org.nexo.userservice.enums.EReportStatus;
import org.nexo.userservice.enums.ERole;
import org.nexo.userservice.exception.ResourceNotFoundException;
import org.nexo.userservice.model.ReportUserId;
import org.nexo.userservice.model.ReportUserModel;
import org.nexo.userservice.model.UserModel;
import org.nexo.userservice.repository.ReportUserRepository;
import org.nexo.userservice.repository.UserRepository;
import org.nexo.userservice.service.ReportUserService;
import org.nexo.userservice.util.JwtUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportUserServiceImpl implements ReportUserService {

        private final ReportUserRepository reportUserRepository;
        private final UserRepository userRepository;
        private final JwtUtil jwtUtil;

        @Override
        @Transactional
        public ReportUserResponse createReport(String token, String username, CreateReportRequest request) {
                String keycloakUserId = jwtUtil.getUserIdFromToken(token);
                Long reporterId = userRepository.findActiveByKeycloakUserId(keycloakUserId)
                                .map(UserModel::getId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                UserModel reportedUser = userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Target user not found with username: " + username));

                Long reportedId = reportedUser.getId();

                if (reporterId.equals(reportedId)) {
                        throw new ResourceNotFoundException("Cannot report yourself");
                }

                ReportUserId reportId = ReportUserId.builder()
                                .reporterId(reporterId)
                                .reportedId(reportedId)
                                .build();

                if (reportUserRepository.existsById(reportId)) {
                        throw new ResourceNotFoundException("User has already been reported");
                }

                UserModel reporterUser = userRepository.findById(reporterId)
                                .orElseThrow(() -> new ResourceNotFoundException("Reporter user not found"));

                ReportUserModel reportModel = ReportUserModel.builder()
                                .id(reportId)
                                .reporter(reporterUser)
                                .reported(reportedUser)
                                .reason(request.getReason())
                                .status(EReportStatus.PENDING)
                                .build();

                ReportUserModel savedReport = reportUserRepository.save(reportModel);
                return mapToResponse(savedReport);
        }

        @Override
        public PageModelResponse<ReportUserResponse> getReportsByReporter(String token, Pageable pageable,
                        String search) {
                String keycloakUserId = jwtUtil.getUserIdFromToken(token);
                Long reporterId = userRepository.findActiveByKeycloakUserId(keycloakUserId)
                                .map(UserModel::getId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                Page<ReportUserModel> reportsPage;
                if (search != null && !search.trim().isEmpty()) {
                        reportsPage = reportUserRepository.findByIdReporterIdWithSearch(reporterId, search.trim(),
                                        pageable);
                } else {
                        reportsPage = reportUserRepository.findByIdReporterId(reporterId, pageable);
                }

                List<ReportUserResponse> reportResponses = reportsPage.getContent().stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());

                return PageModelResponse.<ReportUserResponse>builder()
                                .content(reportResponses)
                                .pageNo(reportsPage.getNumber())
                                .pageSize(reportsPage.getSize())
                                .totalElements(reportsPage.getTotalElements())
                                .totalPages(reportsPage.getTotalPages())
                                .last(reportsPage.isLast())
                                .build();
        }

        @Override
        public PageModelResponse<ReportUserResponse> getReportsByReported(String token, String username,
                        Pageable pageable,
                        String search) {
                String keycloakUserId = jwtUtil.getUserIdFromToken(token);
                Long currentUserId = userRepository.findActiveByKeycloakUserId(keycloakUserId)
                                .map(UserModel::getId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                UserModel reportedUser = userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Target user not found with username: " + username));

                Long reportedId = reportedUser.getId();

                UserModel currentUser = userRepository.findById(currentUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

                if (!currentUserId.equals(reportedId) && currentUser.getRole() != ERole.ADMIN) {
                        throw new ResourceNotFoundException("You don't have permission to view these reports");
                }

                Page<ReportUserModel> reportsPage;
                if (search != null && !search.trim().isEmpty()) {
                        reportsPage = reportUserRepository.findByIdReportedIdWithSearch(reportedId, search.trim(),
                                        pageable);
                } else {
                        reportsPage = reportUserRepository.findByIdReportedId(reportedId, pageable);
                }

                List<ReportUserResponse> reportResponses = reportsPage.getContent().stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());

                return PageModelResponse.<ReportUserResponse>builder()
                                .content(reportResponses)
                                .pageNo(reportsPage.getNumber())
                                .pageSize(reportsPage.getSize())
                                .totalElements(reportsPage.getTotalElements())
                                .totalPages(reportsPage.getTotalPages())
                                .last(reportsPage.isLast())
                                .build();
        }

        @Override
        public PageModelResponse<ReportUserResponse> getAllReports(EReportStatus status, Pageable pageable) {
                Page<ReportUserModel> reportsPage;
                if (status != null) {
                        reportsPage = reportUserRepository.findByStatus(status, pageable);
                } else {
                        reportsPage = reportUserRepository.findAll(pageable);
                }

                List<ReportUserResponse> reportResponses = reportsPage.getContent().stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());

                return PageModelResponse.<ReportUserResponse>builder()
                                .content(reportResponses)
                                .pageNo(reportsPage.getNumber())
                                .pageSize(reportsPage.getSize())
                                .totalElements(reportsPage.getTotalElements())
                                .totalPages(reportsPage.getTotalPages())
                                .last(reportsPage.isLast())
                                .build();
        }

        @Override
        @Transactional
        public ReportUserResponse updateReportStatus(Long reporterId, Long reportedId,
                        UpdateReportStatusRequest request) {
                ReportUserId reportId = ReportUserId.builder()
                                .reporterId(reporterId)
                                .reportedId(reportedId)
                                .build();

                ReportUserModel reportModel = reportUserRepository.findById(reportId)
                                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

                reportModel.setStatus(request.getStatus());
                ReportUserModel updatedReport = reportUserRepository.save(reportModel);
                return mapToResponse(updatedReport);
        }

        @Override
        @Transactional
        public void deleteReport(String token, String username) {
                String keycloakUserId = jwtUtil.getUserIdFromToken(token);
                Long reporterId = userRepository.findActiveByKeycloakUserId(keycloakUserId)
                                .map(UserModel::getId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                UserModel reportedUser = userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Target user not found with username: " + username));

                Long reportedId = reportedUser.getId();

                ReportUserId reportId = ReportUserId.builder()
                                .reporterId(reporterId)
                                .reportedId(reportedId)
                                .build();

                if (!reportUserRepository.existsById(reportId)) {
                        throw new ResourceNotFoundException("Report not found");
                }
                reportUserRepository.deleteById(reportId);
        }

        private ReportUserResponse mapToResponse(ReportUserModel report) {
                return ReportUserResponse.builder()
                                .reporterId(report.getId().getReporterId())
                                .reporterUsername(report.getReporter() != null ? report.getReporter().getUsername()
                                                : null)
                                .reportedId(report.getId().getReportedId())
                                .reportedUsername(report.getReported() != null ? report.getReported().getUsername()
                                                : null)
                                .reason(report.getReason())
                                .status(report.getStatus())
                                .createdAt(report.getCreatedAt())
                                .build();
        }
}
