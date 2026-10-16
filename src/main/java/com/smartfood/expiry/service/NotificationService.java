package com.smartfood.expiry.service;

import com.smartfood.expiry.dto.BatchItemViewDto;
import com.smartfood.expiry.entity.ExpiryNotification;
import com.smartfood.expiry.entity.ProductBatch;
import com.smartfood.expiry.entity.User;
import com.smartfood.expiry.enums.ExpiryStatus;
import com.smartfood.expiry.repository.ExpiryNotificationRepository;
import com.smartfood.expiry.repository.ProductBatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final ExpiryNotificationRepository notificationRepository;
    private final ProductBatchRepository batchRepository;
    private final ExpiryDetectionService expiryDetectionService;
    private final JavaMailSender mailSender;

    @Value("${app.notifications.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${spring.mail.username:alerts@smartfood.example.com}")
    private String mailFrom;

    @Value("${app.notifications.email.to:}")
    private String mailTo;

    public NotificationService(ExpiryNotificationRepository notificationRepository,
                               ProductBatchRepository batchRepository,
                               ExpiryDetectionService expiryDetectionService,
                               org.springframework.beans.factory.ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.notificationRepository = notificationRepository;
        this.batchRepository = batchRepository;
        this.expiryDetectionService = expiryDetectionService;
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    /**
     * Scheduled daily task to detect expiring/expired inventory and generate alert notifications.
     */
    @Scheduled(cron = "${app.scheduling.expiry-check-cron:0 0 6 * * *}")
    public void runDailyExpiryAudit() {
        log.info("Starting scheduled daily expiry detection audit...");
        int generatedCount = performExpiryCheck(LocalDate.now());
        log.info("Expiry audit completed. Generated {} new notifications.", generatedCount);
    }

    public int performExpiryCheck(LocalDate referenceDate) {
        List<BatchItemViewDto> allViews = expiryDetectionService.getAllActiveBatchViews(referenceDate);
        int newAlerts = 0;

        StringBuilder emailSummary = new StringBuilder();

        for (BatchItemViewDto view : allViews) {
            ProductBatch batch = view.getBatch();
            ExpiryStatus status = view.getStatus();

            if (status == ExpiryStatus.EXPIRED || status == ExpiryStatus.NEAR_EXPIRY) {
                // Check if notification already exists for this batch, date, and status to prevent duplicate spam
                boolean alreadyNotified = notificationRepository.existsByBatchAndAlertDateAndAlertType(
                        batch, referenceDate, status);

                if (!alreadyNotified) {
                    ExpiryNotification notification = new ExpiryNotification(
                            batch,
                            status,
                            referenceDate,
                            view.getDaysRemaining()
                    );
                    notificationRepository.save(notification);
                    newAlerts++;

                    emailSummary.append("- [")
                            .append(status.getDisplayName())
                            .append("] ")
                            .append(batch.getProduct().getName())
                            .append(" (Batch: ")
                            .append(batch.getBatchNumber())
                            .append(") - Qty: ")
                            .append(batch.getQuantity())
                            .append(", Expiry: ")
                            .append(batch.getExpiryDate())
                            .append("\n");
                }
            }
        }

        if (newAlerts > 0 && emailEnabled && mailSender != null && mailTo != null && !mailTo.isBlank()) {
            sendEmailAlert("Smart Food Expiry Alert: " + newAlerts + " items require attention", emailSummary.toString());
        }

        return newAlerts;
    }

    public void markAsReviewed(Long notificationId, User user, String notes) {
        ExpiryNotification notif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + notificationId));

        notif.setReviewed(true);
        notif.setReviewedByUser(user);
        notif.setReviewedAt(LocalDateTime.now());
        if (notes != null && !notes.trim().isEmpty()) {
            notif.setNotes(notes.trim());
        }
        notificationRepository.save(notif);
    }

    @Transactional(readOnly = true)
    public List<ExpiryNotification> getUnreviewedNotifications() {
        return notificationRepository.findByReviewedFalseOrderByAlertDateDesc();
    }

    @Transactional(readOnly = true)
    public List<ExpiryNotification> getAllNotifications() {
        return notificationRepository.findAllByOrderByAlertDateDesc();
    }

    private void sendEmailAlert(String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(mailTo);
            message.setSubject(subject);
            message.setText("Daily Food Expiry Management Summary:\n\n" + content +
                    "\nPlease review the store dashboard and FEFO rotation shelves immediately.");
            mailSender.send(message);
            log.info("Expiry alert email sent successfully.");
        } catch (Exception e) {
            log.warn("Failed to send email alert (check SMTP configuration): {}", e.getMessage());
        }
    }
}
