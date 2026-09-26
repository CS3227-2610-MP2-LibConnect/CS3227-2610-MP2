package libconnect.services;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import libconnect.integration.LoanSummary;
import libconnect.models.Notification;
import libconnect.models.NotificationType;
import libconnect.models.Reservation;
import libconnect.storage.repositories.NotificationRepository;

/** Creates idempotent overdue and reservation notifications. */
public final class NotificationService {
    private final NotificationRepository repository;
    private final Clock clock;

    /** Creates a notification service with an injectable clock. */
    public NotificationService(NotificationRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Sends or returns the existing overdue alert for a loan. */
    public Notification sendOverdueAlert(LoanSummary loan) {
        Objects.requireNonNull(loan, "loan");
        return sendIfAbsent(loan.getMemberId(), NotificationType.OVERDUE_ALERT, loan.getLoanId(),
                "Loan " + loan.getLoanId() + " is overdue.");
    }

    /** Sends or returns the existing reservation reminder. */
    public Notification sendReservationReminder(Reservation reservation) {
        Objects.requireNonNull(reservation, "reservation");
        return sendIfAbsent(reservation.getMemberId(), NotificationType.RESERVATION_REMINDER,
                reservation.getId(), "Reservation " + reservation.getId() + " is ready for collection.");
    }

    /** Returns notifications for a user in creation order. */
    public List<Notification> getNotifications(String userId) {
        requireText(userId, "userId");
        return repository.findByRecipientUserId(userId).stream()
                .sorted(java.util.Comparator.comparing(Notification::getCreatedAt))
                .toList();
    }

    /** Marks a notification as read. */
    public Notification markAsRead(String notificationId) {
        requireText(notificationId, "notificationId");
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification does not exist"));
        if (!notification.isRead()) {
            notification = notification.markRead();
            repository.save(notification);
        }
        return notification;
    }

    private Notification sendIfAbsent(String recipientUserId, NotificationType type,
                                      String referenceId, String message) {
        if (repository.existsByRecipientAndReference(recipientUserId, type, referenceId)) {
            return repository.findByRecipientUserId(recipientUserId).stream()
                    .filter(notification -> notification.getType() == type
                            && notification.getReferenceId().equals(referenceId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Notification index is inconsistent"));
        }
        Notification notification = new Notification(UUID.randomUUID().toString(), recipientUserId, type,
                referenceId, message, LocalDateTime.now(clock), false);
        repository.save(notification);
        return notification;
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
