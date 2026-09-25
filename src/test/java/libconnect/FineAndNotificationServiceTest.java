package libconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import libconnect.integration.LoanQuery;
import libconnect.integration.LoanSummary;
import libconnect.integration.MemberDirectory;
import libconnect.models.Notification;
import libconnect.models.NotificationType;
import libconnect.models.Reservation;
import libconnect.models.ReservationStatus;
import libconnect.models.Fine;
import libconnect.models.FineStatus;
import libconnect.services.FineService;
import libconnect.services.NotificationService;

/** Tests fine calculation and idempotent notification delivery. */
class FineAndNotificationServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-21T00:00:00Z"), ZoneOffset.UTC);

    /** Verifies overdue days are multiplied by the configured daily rate. */
    @Test
    void calculateOverdueFine_overdueLoan_returnsExpectedAmount() {
        LoanSummary loan = new LoanSummary("l1", "m1", "c1", LocalDate.of(2026, 9, 18));
        LoanQuery loans = new LoanQuery() {
            @Override
            public Optional<LoanSummary> findById(String loanId) {
                return Optional.of(loan);
            }

            @Override
            public List<LoanSummary> findActiveLoans() {
                return List.of(loan);
            }

            @Override
            public List<LoanSummary> findOverdueLoans(LocalDate date) {
                return List.of(loan);
            }
        };
        MemberDirectory members = new ActiveMemberDirectory();
        FineService service = new FineService(new ServiceTestDoubles.Fines(), loans, members,
                CLOCK, BigDecimal.valueOf(1.50));

        assertEquals(BigDecimal.valueOf(4.50).setScale(2), service.calculateOverdueFine("l1"));
    }

    @Test
    void calculateOverdueFine_dueDate_returnsZero() {
        LoanSummary loan = new LoanSummary("l1", "m1", "c1", LocalDate.of(2026, 9, 21));
        FineService service = createFineService(loan, true);

        assertEquals(BigDecimal.ZERO.setScale(2), service.calculateOverdueFine("l1"));
    }

    @Test
    void calculateOverdueFine_roundsDailyRateBeforeMultiplication() {
        LoanSummary loan = new LoanSummary("l1", "m1", "c1", LocalDate.of(2026, 9, 19));
        FineService service = createFineService(loan, true, BigDecimal.valueOf(1.005));

        assertEquals(BigDecimal.valueOf(2.02).setScale(2), service.calculateOverdueFine("l1"));
    }

    @Test
    void fineService_invalidRateOrLoan_rejected() {
        LoanSummary loan = new LoanSummary("l1", "m1", "c1", LocalDate.of(2026, 9, 18));

        assertThrows(IllegalArgumentException.class, () -> createFineService(loan, true, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> createFineService(loan, true).calculateOverdueFine("missing"));
        assertThrows(IllegalArgumentException.class,
                () -> createFineService(loan, true).calculateOverdueFine(" "));
    }

    @Test
    void createFine_overdueLoan_persistsAndIsIdempotent() {
        LoanSummary loan = new LoanSummary("l1", "m1", "c1", LocalDate.of(2026, 9, 18));
        ServiceTestDoubles.Fines repository = new ServiceTestDoubles.Fines();
        FineService service = createFineService(repository, loan, true, BigDecimal.ONE);

        Fine first = service.createFine("l1");
        Fine second = service.createFine("l1");

        assertSame(first.getId(), second.getId());
        assertEquals(FineStatus.OUTSTANDING, first.getStatus());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void createFine_nonOverdueOrInactiveMember_rejected() {
        LoanSummary dueLoan = new LoanSummary("l1", "m1", "c1", LocalDate.of(2026, 9, 21));
        LoanSummary overdueLoan = new LoanSummary("l2", "m1", "c1", LocalDate.of(2026, 9, 18));

        assertThrows(IllegalStateException.class, () -> createFineService(dueLoan, true).createFine("l1"));
        assertThrows(IllegalStateException.class,
                () -> createFineService(overdueLoan, false).createFine("l2"));
    }

    @Test
    void editFine_roundsAmount_andRejectsInvalidStates() {
        ServiceTestDoubles.Fines repository = new ServiceTestDoubles.Fines();
        repository.save(new Fine("f1", "l1", "m1", BigDecimal.ONE, "reason",
                FineStatus.OUTSTANDING, LocalDate.of(2026, 9, 21)));
        repository.save(new Fine("f2", "l2", "m1", BigDecimal.ONE, "reason",
                FineStatus.PAID, LocalDate.of(2026, 9, 21)));
        FineService service = createFineService(repository,
                new LoanSummary("l1", "m1", "c1", LocalDate.of(2026, 9, 18)), true, BigDecimal.ONE);

        assertEquals(BigDecimal.valueOf(2.35).setScale(2),
                service.editFine("f1", BigDecimal.valueOf(2.345)).getAmount());
        assertThrows(IllegalArgumentException.class, () -> service.editFine("f1", BigDecimal.valueOf(-1)));
        assertThrows(NullPointerException.class, () -> service.editFine("f1", null));
        assertThrows(IllegalStateException.class, () -> service.editFine("f2", BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> service.editFine("missing", BigDecimal.ONE));
    }

    @Test
    void removeFine_existingAndMissingFine_reportsResult() {
        ServiceTestDoubles.Fines repository = new ServiceTestDoubles.Fines();
        repository.save(new Fine("f1", "l1", "m1", BigDecimal.ONE, "reason",
                FineStatus.OUTSTANDING, LocalDate.of(2026, 9, 21)));
        FineService service = createFineService(repository,
                new LoanSummary("l1", "m1", "c1", LocalDate.of(2026, 9, 18)), true, BigDecimal.ONE);

        assertEquals(true, service.removeFine("f1"));
        assertEquals(false, service.removeFine("f1"));
    }

    /** Verifies repeated alert delivery returns the original notification. */
    @Test
    void sendOverdueAlert_sameLoan_isIdempotent() {
        LoanSummary loan = new LoanSummary("l1", "m1", "c1", LocalDate.of(2026, 9, 18));
        ServiceTestDoubles.Notifications repository = new ServiceTestDoubles.Notifications();
        NotificationService service = new NotificationService(repository, CLOCK);

        Notification first = service.sendOverdueAlert(loan);
        Notification second = service.sendOverdueAlert(loan);

        assertSame(first.getId(), second.getId());
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
    }

    @Test
    void sendReservationReminder_setsRecipientAndIsIdempotent() {
        ServiceTestDoubles.Notifications repository = new ServiceTestDoubles.Notifications();
        NotificationService service = new NotificationService(repository, CLOCK);
        Reservation reservation = new Reservation("r1", "m1", "b1",
                LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 27), ReservationStatus.PENDING);

        Notification first = service.sendReservationReminder(reservation);
        Notification second = service.sendReservationReminder(reservation);

        assertEquals(NotificationType.RESERVATION_REMINDER, first.getType());
        assertEquals("m1", first.getRecipientUserId());
        assertSame(first.getId(), second.getId());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void notifications_differentTypesAndReferences_areIndependent() {
        ServiceTestDoubles.Notifications repository = new ServiceTestDoubles.Notifications();
        NotificationService service = new NotificationService(repository, CLOCK);
        LoanSummary loan = new LoanSummary("same-id", "m1", "c1", LocalDate.of(2026, 9, 18));
        Reservation reservation = new Reservation("same-id", "m1", "b1",
                LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 27), ReservationStatus.PENDING);

        service.sendOverdueAlert(loan);
        service.sendReservationReminder(reservation);

        assertEquals(2, service.getNotifications("m1").size());
    }

    @Test
    void markAsRead_missingOrBlankNotification_rejected() {
        NotificationService service = new NotificationService(new ServiceTestDoubles.Notifications(), CLOCK);

        assertThrows(IllegalArgumentException.class, () -> service.markAsRead("missing"));
        assertThrows(IllegalArgumentException.class, () -> service.markAsRead(" "));
    }

    private static FineService createFineService(LoanSummary loan, boolean memberActive) {
        return createFineService(new ServiceTestDoubles.Fines(), loan, memberActive, BigDecimal.ONE);
    }

    private static FineService createFineService(LoanSummary loan, boolean memberActive,
                                                 BigDecimal dailyRate) {
        return createFineService(new ServiceTestDoubles.Fines(), loan, memberActive, dailyRate);
    }

    private static FineService createFineService(ServiceTestDoubles.Fines repository,
                                                 LoanSummary loan, boolean memberActive,
                                                 BigDecimal dailyRate) {
        LoanQuery loans = new LoanQuery() {
            @Override
            public Optional<LoanSummary> findById(String loanId) {
                return loan.getLoanId().equals(loanId) ? Optional.of(loan) : Optional.empty();
            }

            @Override
            public List<LoanSummary> findActiveLoans() {
                return List.of(loan);
            }

            @Override
            public List<LoanSummary> findOverdueLoans(LocalDate date) {
                return loan.isOverdueOn(date) ? List.of(loan) : List.of();
            }
        };
        MemberDirectory members = new ActiveMemberDirectory() {
            @Override
            public boolean isActive(String memberId) {
                return memberActive && super.isActive(memberId);
            }
        };
        return new FineService(repository, loans, members, CLOCK, dailyRate);
    }

    private static class ActiveMemberDirectory implements MemberDirectory {
        /** Returns whether the test member exists. */
        @Override
        public boolean exists(String memberId) {
            return memberId.equals("m1");
        }

        /** Returns whether the test member is active. */
        @Override
        public boolean isActive(String memberId) {
            return memberId.equals("m1");
        }
    }
}
