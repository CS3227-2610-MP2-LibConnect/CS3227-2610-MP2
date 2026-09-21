package libconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import libconnect.models.AccountStatus;
import libconnect.models.Fine;
import libconnect.models.FineStatus;
import libconnect.models.Librarian;
import libconnect.models.Notification;
import libconnect.models.NotificationType;
import libconnect.models.Reservation;
import libconnect.models.ReservationStatus;

/** Tests invariants and state transitions for librarian-owned models. */
class LibrarianDomainTest {
    /** Verifies that invalid librarian input is rejected. */
    @Test
    void librarian_blankEmployeeId_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Librarian(" ", "Name", "email@test", AccountStatus.ACTIVE));
    }

    /** Verifies that reservation transitions preserve identity and change status. */
    @Test
    void reservation_withStatus_preservesIdentity() {
        Reservation reservation = new Reservation("r1", "m1", "b1",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 7), ReservationStatus.PENDING);

        Reservation cancelled = reservation.withStatus(ReservationStatus.CANCELLED);

        assertEquals("r1", cancelled.getId());
        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
    }

    /** Verifies that fine amounts cannot be negative and waived fines change state. */
    @Test
    void fine_negativeAmount_rejectedAndWaiveChangesStatus() {
        assertThrows(IllegalArgumentException.class, () -> new Fine("f1", "l1", "m1",
                BigDecimal.valueOf(-1), "reason", FineStatus.OUTSTANDING, LocalDate.now()));

        Fine fine = new Fine("f1", "l1", "m1", BigDecimal.ONE, "reason",
                FineStatus.OUTSTANDING, LocalDate.now());
        assertEquals(FineStatus.WAIVED, fine.waive().getStatus());
    }

    /** Verifies that notifications can be marked read without changing their identity. */
    @Test
    void notification_markRead_setsReadState() {
        Notification notification = new Notification("n1", "m1", NotificationType.OVERDUE_ALERT,
                "l1", "Overdue", LocalDateTime.now(), false);

        assertTrue(notification.markRead().isRead());
        assertEquals("n1", notification.markRead().getId());
    }
}
