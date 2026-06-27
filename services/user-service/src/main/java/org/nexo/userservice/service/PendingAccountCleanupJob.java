package org.nexo.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.userservice.enums.EAccountStatus;
import org.nexo.userservice.grpc.AuthGrpcClient;
import org.nexo.userservice.model.UserModel;
import org.nexo.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class PendingAccountCleanupJob {

    private final UserRepository userRepository;
    private final AuthGrpcClient authGrpcClient;

    @Value("${account.pending.expiration-days:7}")
    private long pendingExpirationDays;

    // Mặc định chạy mỗi ngày lúc 03:00 sáng. Có thể override qua property.
    @Scheduled(cron = "${account.pending.cleanup-cron:0 0 3 * * *}")
    public void cleanupExpiredPendingAccounts() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(pendingExpirationDays);
        List<UserModel> expired = userRepository
                .findAllByAccountStatusAndCreatedAtBefore(EAccountStatus.PENDING, threshold);

        if (expired.isEmpty()) {
            log.info("[PENDING-CLEANUP] Không có tài khoản PENDING quá hạn (threshold={})", threshold);
            return;
        }

        log.info("[PENDING-CLEANUP] Tìm thấy {} tài khoản PENDING quá hạn cần dọn dẹp", expired.size());
        int deleted = 0;
        for (UserModel user : expired) {
            try {
                // Xóa trên Keycloak trước để giải phóng email/username cho việc đăng ký lại
                boolean keycloakDeleted = true;
                if (user.getKeycloakUserId() != null && !user.getKeycloakUserId().isBlank()) {
                    keycloakDeleted = authGrpcClient.deleteUser(user.getKeycloakUserId());
                }

                if (!keycloakDeleted) {
                    log.warn("[PENDING-CLEANUP] Bỏ qua xóa DB cho userId={} vì xóa Keycloak thất bại (sẽ thử lại lần sau)",
                            user.getId());
                    continue;
                }

                userRepository.delete(user);
                deleted++;
                log.info("[PENDING-CLEANUP] Đã xóa tài khoản PENDING quá hạn userId={}, email={}",
                        user.getId(), user.getEmail());
            } catch (Exception e) {
                log.error("[PENDING-CLEANUP] Lỗi khi dọn dẹp userId={}: {}", user.getId(), e.getMessage());
            }
        }
        log.info("[PENDING-CLEANUP] Đã dọn dẹp {}/{} tài khoản PENDING quá hạn", deleted, expired.size());
    }
}
