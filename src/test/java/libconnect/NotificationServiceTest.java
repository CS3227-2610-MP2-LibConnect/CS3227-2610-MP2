package libconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import libconnect.integration.LoanSummary;
import libconnect.models.Notification;
import libconnect.models.NotificationType;
import libconnect.models.Reservation;
import libconnect.models.ReservationStatus;
import libconnect.services.NotificationService;

/** Tests notification delivery, querying, and read-state rules. */
class NotificationServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-21T00:00:00Z"), ZoneOffset.UTC);

    /** Verifies repeated alert delivery returns the original notification. */
    @Test
    void sendOverdueAlert_sameLoan_isIdempotent() {
        LoanSummary loan = new LoanSummary("l1", "m1", "c1", LocalDate.of(2026, 9, 18));
        ServiceTestDoubles.Notifications repository = new ServiceTestDoubles.Notifications();
        NotificationService service = new NotificationService(repository, CLOCK);

        Notification first = service.sendOverdueAlert(loan);
        Notification second = service.sendOverdueAlert(loan);

        assertEquals(first.getId(), second.getId());
        assertEquals(1, repository.findAll().size());
    }

    /** Verifies that marking a notification as read persists the new state. */
    @Test
    void markAsRead_unreadNotification_persistsReadState() {
        ServiceTestDoubles.Notifications repository = new ServiceTestDoubles.Notifications();
        NotificationService service = new NotificationService(repository, CLOCK);
        Notification notification = service.sendOverdueAlert(
                new LoanSummary("l1", "m1", "c1", LocalDate.of(2026, 9, 18)));

        assertEquals(true, service.markAsRead(notification.getId()).isRead());
        assertEquals(true, repository.findById(notification.getId()).orElseThrow().isRead());
    }

    @Test
    void sendReservationReminder_createsReminderAndIsIdempotent() {
        ServiceTestDoubles.Notifications repository = new ServiceTestDoubles.Notifications();
        NotificationService service = new NotificationService(repository, CLOCK);
        Reservation reservation = new Reservation("r1", "m1", "b1", LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 8), ReservationStatus.FULFILLED);

        Notification first = service.sendReservationReminder(reservation);
        Notification second = service.sendReservationReminder(reservation);

        assertEquals(first.getId(), second.getId());
        assertEquals(NotificationType.RESERVATION_REMINDER, first.getType());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void getNotifications_isolatesRecipientsAndSortsByCreationTime() {
        ServiceTestDoubles.Notifications repository = new ServiceTestDoubles.Notifications();
        NotificationService service = new NotificationService(repository, CLOCK);
        Notification older = new Notification("n1", "m1", NotificationType.OVERDUE_ALERT, "l1", "Older",
                java.time.LocalDateTime.of(2026, 9, 20, 10, 0), false);
        Notification newer = new Notification("n2", "m1", NotificationType.RESERVATION_REMINDER, "r1", "Newer",
                java.time.LocalDateTime.of(2026, 9, 21, 10, 0), false);
        Notification other = new Notification("n3", "m2", NotificationType.OVERDUE_ALERT, "l2", "Other",
                java.time.LocalDateTime.of(2026, 9, 19, 10, 0), false);
        repository.save(newer);
        repository.save(other);
        repository.save(older);

        assertEquals(List.of(older, newer), service.getNotifications("m1"));
    }

    @Test
    void notificationInputs_missingOrBlank_rejected() {
        NotificationService service = new NotificationService(new ServiceTestDoubles.Notifications(), CLOCK);

        assertThrows(NullPointerException.class, () -> service.sendOverdueAlert(null));
        assertThrows(NullPointerException.class, () -> service.sendReservationReminder(null));
        assertThrows(IllegalArgumentException.class, () -> service.getNotifications(" "));
        assertThrows(IllegalArgumentException.class, () -> service.markAsRead("unknown"));
        assertThrows(IllegalArgumentException.class, () -> service.markAsRead(" "));
    }

    @Test
    void notifications_sameReferenceDifferentRecipients_areIndependent() {
        ServiceTestDoubles.Notifications repository = new ServiceTestDoubles.Notifications();
        NotificationService service = new NotificationService(repository, CLOCK);
        LoanSummary firstLoan = new LoanSummary("l1", "m1", "c1", LocalDate.of(2026, 9, 18));
        LoanSummary secondLoan = new LoanSummary("l1", "m2", "c1", LocalDate.of(2026, 9, 18));

        service.sendOverdueAlert(firstLoan);
        service.sendOverdueAlert(secondLoan);

        assertEquals(2, repository.findAll().size());
        assertEquals(1, service.getNotifications("m1").size());
        assertEquals(1, service.getNotifications("m2").size());
    }

    @Test
    void markAsRead_alreadyReadNotification_returnsReadNotification() {
        ServiceTestDoubles.Notifications repository = new ServiceTestDoubles.Notifications();
        Notification notification = new Notification("n1", "m1", NotificationType.OVERDUE_ALERT, "l1",
                "Already read", java.time.LocalDateTime.of(2026, 9, 21, 10, 0), true);
        repository.save(notification);
        NotificationService service = new NotificationService(repository, CLOCK);

        assertEquals(notification, service.markAsRead("n1"));
        assertEquals(notification, repository.findById("n1").orElseThrow());
    }
}
