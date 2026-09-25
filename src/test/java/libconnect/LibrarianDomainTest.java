package libconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import libconnect.integration.BookDetails;
import libconnect.integration.BookSummary;
import libconnect.integration.LoanSummary;
import libconnect.integration.MemberSummary;
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

    @Test
    void librarian_inactiveStatus_isNotActiveAndCanBeCopied() {
        Librarian librarian = new Librarian("e1", "Ada", "ada@example.com", AccountStatus.INACTIVE);

        Librarian active = librarian.withStatus(AccountStatus.ACTIVE);

        assertFalse(librarian.isActive());
        assertTrue(active.isActive());
        assertEquals("e1", active.getId());
        assertEquals("Ada", active.getName());
    }

    @Test
    void librarian_nullStatus_rejected() {
        assertThrows(NullPointerException.class,
                () -> new Librarian("e1", "Ada", "ada@example.com", null));
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

    @Test
    void reservation_expiryBoundary_isPendingThroughExpiryDateOnly() {
        LocalDate reservationDate = LocalDate.of(2026, 1, 1);
        LocalDate expiryDate = LocalDate.of(2026, 1, 7);
        Reservation reservation = new Reservation("r1", "m1", "b1", reservationDate,
                expiryDate, ReservationStatus.PENDING);

        assertTrue(reservation.isPendingOn(expiryDate));
        assertFalse(reservation.isPendingOn(expiryDate.plusDays(1)));
        assertFalse(reservation.withStatus(ReservationStatus.CANCELLED).isPendingOn(expiryDate));
    }

    @Test
    void reservation_expiryBeforeReservation_rejected() {
        assertThrows(IllegalArgumentException.class, () -> new Reservation("r1", "m1", "b1",
                LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 1), ReservationStatus.PENDING));
    }

    @Test
    void reservation_nullStatusOrDate_rejected() {
        assertThrows(NullPointerException.class, () -> new Reservation("r1", "m1", "b1",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 7), null));
        Reservation reservation = new Reservation("r1", "m1", "b1",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 7), ReservationStatus.PENDING);
        assertThrows(NullPointerException.class, () -> reservation.isPendingOn(null));
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

    @Test
    void fine_zeroAmount_isValidAndAmountUpdatePreservesIdentity() {
        Fine fine = new Fine("f1", "l1", "m1", BigDecimal.ZERO, "reason",
                FineStatus.OUTSTANDING, LocalDate.of(2026, 1, 1));

        Fine updated = fine.withAmount(BigDecimal.valueOf(2.50));

        assertEquals(BigDecimal.ZERO, fine.getAmount());
        assertEquals(BigDecimal.valueOf(2.50), updated.getAmount());
        assertEquals("f1", updated.getId());
        assertEquals("l1", updated.getLoanId());
        assertEquals("m1", updated.getMemberId());
    }

    @Test
    void fine_nullAmount_rejected() {
        assertThrows(NullPointerException.class, () -> new Fine("f1", "l1", "m1", null,
                "reason", FineStatus.OUTSTANDING, LocalDate.of(2026, 1, 1)));
    }

    /** Verifies that notifications can be marked read without changing their identity. */
    @Test
    void notification_markRead_setsReadState() {
        Notification notification = new Notification("n1", "m1", NotificationType.OVERDUE_ALERT,
                "l1", "Overdue", LocalDateTime.now(), false);

        assertTrue(notification.markRead().isRead());
        assertEquals("n1", notification.markRead().getId());
    }

    @Test
    void notification_markRead_isIdempotent() {
        Notification notification = new Notification("n1", "m1", NotificationType.OVERDUE_ALERT,
                "l1", "Overdue", LocalDateTime.of(2026, 1, 1, 9, 0), true);

        Notification readAgain = notification.markRead();

        assertTrue(readAgain.isRead());
        assertEquals(notification.getCreatedAt(), readAgain.getCreatedAt());
        assertEquals(notification.getMessage(), readAgain.getMessage());
    }

    @Test
    void integrationContracts_enforceBoundaries() {
        BookDetails details = new BookDetails("isbn", "Title", "Author", "Publisher",
                "Category", 1);
        BookSummary summary = new BookSummary("b1", details, 0);
        MemberSummary member = new MemberSummary("m1", "Ada", "ada@example.com", false);
        LoanSummary loan = new LoanSummary("l1", "m1", "c1", LocalDate.of(2026, 1, 7));

        assertEquals(0, summary.availableCopies());
        assertFalse(member.isActive());
        assertFalse(loan.isOverdueOn(LocalDate.of(2026, 1, 7)));
        assertEquals(1, loan.getOverdueDaysOn(LocalDate.of(2026, 1, 8)));
        assertThrows(IllegalArgumentException.class, () -> new BookDetails("isbn", "Title",
                "Author", "Publisher", "Category", 0));
        assertThrows(IllegalArgumentException.class, () -> new BookSummary("b1", details, -1));
    }
}
