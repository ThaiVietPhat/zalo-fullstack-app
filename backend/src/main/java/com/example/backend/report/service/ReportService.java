package com.example.backend.report.service;

import com.example.backend.admin.entity.AuditLog;
import com.example.backend.admin.repository.AuditLogRepository;
import com.example.backend.file.service.FileStorageService;
import com.example.backend.messaging.service.NotificationService;
import com.example.backend.report.dto.ReportDto;
import com.example.backend.report.dto.ReportRequest;
import com.example.backend.report.dto.ResolveReportRequest;
import com.example.backend.report.entity.Report;
import com.example.backend.report.entity.ReportStatus;
import com.example.backend.report.repository.ReportRepository;
import com.example.backend.shared.exception.ResourceNotFoundException;
import com.example.backend.user.entity.User;
import com.example.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogRepository auditLogRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public ReportDto createReport(UUID reporterId, UUID reportedId, ReportRequest req) {
        if (reporterId.equals(reportedId)) {
            throw new IllegalArgumentException("Không thể tố cáo chính mình");
        }
        if (reportRepository.existsByReporter_IdAndReported_IdAndStatus(reporterId, reportedId, ReportStatus.PENDING)) {
            throw new IllegalArgumentException("Bạn đã có báo cáo đang chờ xử lý với người dùng này");
        }

        User reporter = getUser(reporterId);
        User reported = getUser(reportedId);

        Report report = new Report();
        report.setReporter(reporter);
        report.setReported(reported);
        report.setReason(req.getReason());
        report.setDescription(req.getDescription());
        report.setStatus(ReportStatus.PENDING);
        if (req.getEvidenceKeys() != null && !req.getEvidenceKeys().isEmpty()) {
            report.setEvidenceKeys(req.getEvidenceKeys());
        }

        reportRepository.save(report);
        log.info("Report created: {} reported {}", reporterId, reportedId);
        return toDto(report);
    }

    @Transactional(readOnly = true)
    public Page<ReportDto> getReports(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            ReportStatus reportStatus = ReportStatus.valueOf(status.toUpperCase());
            return reportRepository.findAllByStatusOrderByCreatedAtDesc(reportStatus, pageable)
                    .map(this::toDto);
        }
        return reportRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toDto);
    }

    @Transactional
    public ReportDto resolveReport(Long reportId, UUID adminId, ResolveReportRequest req) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));
        User admin = getUser(adminId);
        User reported = report.getReported();

        report.setStatus(ReportStatus.valueOf(req.getStatus().toUpperCase()));
        report.setResolvedAt(LocalDateTime.now());
        report.setResolvedBy(admin);
        report.setResolution(req.getResolution());
        reportRepository.save(report);

        // Thực thi hành động phạt nếu có
        String banAction = req.getBanAction() != null ? req.getBanAction() : "NONE";
        if (!"NONE".equalsIgnoreCase(banAction) && "RESOLVED".equalsIgnoreCase(req.getStatus())) {
            Integer durationDays = switch (banAction.toUpperCase()) {
                case "BAN_1_DAY"    -> 1;
                case "BAN_7_DAYS"   -> 7;
                case "BAN_30_DAYS"  -> 30;
                default             -> null; // BAN_PERMANENT
            };
            String banReason = "Vi phạm bị tố cáo: " + report.getReason()
                    + (req.getResolution() != null && !req.getResolution().isBlank()
                        ? " — " + req.getResolution() : "");

            reported.setBanned(true);
            reported.setBanReason(banReason);
            reported.setBannedAt(LocalDateTime.now());
            reported.setBanUntil(durationDays != null
                    ? LocalDateTime.now().plusDays(durationDays) : null);
            userRepository.save(reported);

            notificationService.sendAccountBanned(reported.getEmail(), banReason, reported.getBanUntil());

            // Ghi audit log
            AuditLog log_ = new AuditLog();
            log_.setAdmin(admin);
            log_.setAdminEmail(admin.getEmail());
            log_.setAction("BAN_USER");
            log_.setTargetType("USER");
            log_.setTargetId(reported.getId());
            log_.setTargetName((reported.getFirstName() + " " + reported.getLastName()).trim());
            log_.setDetails("Via report #" + reportId + " — " + banReason);
            auditLogRepository.save(log_);
        }

        log.info("Report {} resolved as {} (banAction={}) by {}", reportId, req.getStatus(), banAction, adminId);
        return toDto(report);
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private ReportDto toDto(Report r) {
        // Sinh presigned URL cho từng evidence key
        List<String> evidenceUrls = new ArrayList<>();
        if (r.getEvidenceKeys() != null) {
            for (String key : r.getEvidenceKeys()) {
                try {
                    evidenceUrls.add(fileStorageService.generatePresignedUrl(key));
                } catch (Exception e) {
                    log.warn("Cannot generate presigned URL for evidence key: {}", key);
                }
            }
        }

        return ReportDto.builder()
                .id(r.getId())
                .reporterId(r.getReporter().getId())
                .reporterName((r.getReporter().getFirstName() + " " + r.getReporter().getLastName()).trim())
                .reporterEmail(r.getReporter().getEmail())
                .reportedId(r.getReported().getId())
                .reportedName((r.getReported().getFirstName() + " " + r.getReported().getLastName()).trim())
                .reportedEmail(r.getReported().getEmail())
                .reason(r.getReason())
                .description(r.getDescription())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .resolvedAt(r.getResolvedAt())
                .resolution(r.getResolution())
                .reportedBanned(r.getReported().isBanned())
                .evidenceUrls(evidenceUrls)
                .build();
    }
}
