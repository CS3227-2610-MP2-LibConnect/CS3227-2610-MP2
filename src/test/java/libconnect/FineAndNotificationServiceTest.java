package libconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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

    private static final class ActiveMemberDirectory implements MemberDirectory {
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
