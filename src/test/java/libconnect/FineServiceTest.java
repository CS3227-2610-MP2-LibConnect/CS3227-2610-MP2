package libconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import libconnect.integration.ReturnedLoanSummary;
import libconnect.models.Fine;
import libconnect.models.FineStatus;
import libconnect.services.FineService;

/** Tests fine calculation, management, and validation rules. */
class FineServiceTest {
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
    void calculateOverdueFine_dueToday_returnsZero() {
        LoanSummary loan = new LoanSummary("l1", "m1", "c1", LocalDate.of(2026, 9, 21));
        FineService service = new FineService(new ServiceTestDoubles.Fines(), loanQueryFor(loan),
                new ActiveMemberDirectory(), CLOCK, BigDecimal.valueOf(1.50));

        assertEquals(BigDecimal.ZERO.setScale(2), service.calculateOverdueFine("l1"));
    }

    @Test
    void createFine_overdueLoan_persistsCorrectFineAndIsIdempotent() {
        LoanSummary loan = new LoanSummary("l1", "m1", "c1", "Algorithms",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 18));
        ServiceTestDoubles.Fines repository = new ServiceTestDoubles.Fines();
        FineService service = new FineService(repository, loanQueryFor(loan), new ActiveMemberDirectory(),
                CLOCK, BigDecimal.valueOf(1.50));

        Fine first = service.createFine("l1");
        Fine second = service.createFine("l1");

        assertEquals(first.getId(), second.getId());
        assertEquals(BigDecimal.valueOf(4.50).setScale(2), first.getAmount());
        assertEquals(FineStatus.OUTSTANDING, first.getStatus());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void createFine_nonOverdueOrInactiveMember_rejected() {
        LoanSummary currentLoan = new LoanSummary("l1", "m1", "c1", LocalDate.of(2026, 9, 21));
        FineService currentService = new FineService(new ServiceTestDoubles.Fines(), loanQueryFor(currentLoan),
                new ActiveMemberDirectory(), CLOCK, BigDecimal.ONE);
        assertThrows(IllegalStateException.class, () -> currentService.createFine("l1"));

        LoanSummary inactiveLoan = new LoanSummary("l2", "inactive", "c1", LocalDate.of(2026, 9, 18));
        FineService inactiveService = new FineService(new ServiceTestDoubles.Fines(), loanQueryFor(inactiveLoan),
                new ActiveMemberDirectory(), CLOCK, BigDecimal.ONE);
        assertThrows(IllegalStateException.class, () -> inactiveService.createFine("l2"));
    }

    @Test
    void createFine_unknownLoanAndBlankId_rejected() {
        FineService service = new FineService(new ServiceTestDoubles.Fines(), loanQueryFor(null),
                new ActiveMemberDirectory(), CLOCK, BigDecimal.ONE);

        assertThrows(IllegalArgumentException.class, () -> service.createFine("unknown"));
        assertThrows(IllegalArgumentException.class, () -> service.calculateOverdueFine(" "));
    }

    @Test
    void issueForReturnedLoan_overdueSummary_createsFine() {
        ServiceTestDoubles.Fines repository = new ServiceTestDoubles.Fines();
        FineService service = new FineService(repository, loanQueryFor(null), new ActiveMemberDirectory(),
                CLOCK, BigDecimal.valueOf(2.25));
        ReturnedLoanSummary returnedLoan = new ReturnedLoanSummary("l1", "m1",
                LocalDate.of(2026, 9, 18), LocalDate.of(2026, 9, 20));

        Fine fine = service.issueForReturnedLoan(returnedLoan);

        assertEquals(BigDecimal.valueOf(4.50).setScale(2), fine.getAmount());
        assertEquals(FineStatus.OUTSTANDING, fine.getStatus());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void issueForReturnedLoan_sameLoan_isIdempotent() {
        ServiceTestDoubles.Fines repository = new ServiceTestDoubles.Fines();
        FineService service = new FineService(repository, loanQueryFor(null), new ActiveMemberDirectory(),
                CLOCK, BigDecimal.ONE);
        ReturnedLoanSummary returnedLoan = new ReturnedLoanSummary("l1", "m1",
                LocalDate.of(2026, 9, 18), LocalDate.of(2026, 9, 20));

        Fine first = service.issueForReturnedLoan(returnedLoan);
        Fine second = service.issueForReturnedLoan(returnedLoan);

        assertEquals(first.getId(), second.getId());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void issueForReturnedLoan_nonOverdueOrUnknownMember_rejected() {
        FineService service = new FineService(new ServiceTestDoubles.Fines(), loanQueryFor(null),
                new ActiveMemberDirectory(), CLOCK, BigDecimal.ONE);
        ReturnedLoanSummary nonOverdueLoan = new ReturnedLoanSummary("l1", "m1",
                LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 21));
        ReturnedLoanSummary unknownMemberLoan = new ReturnedLoanSummary("l2", "unknown",
                LocalDate.of(2026, 9, 18), LocalDate.of(2026, 9, 21));

        assertThrows(IllegalStateException.class, () -> service.issueForReturnedLoan(nonOverdueLoan));
        assertThrows(IllegalStateException.class, () -> service.issueForReturnedLoan(unknownMemberLoan));
    }

    @Test
    void fineConstructor_invalidDailyRate_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new FineService(new ServiceTestDoubles.Fines(), loanQueryFor(null),
                        new ActiveMemberDirectory(), CLOCK, BigDecimal.ZERO));
        assertThrows(NullPointerException.class,
                () -> new FineService(null, loanQueryFor(null), new ActiveMemberDirectory(), CLOCK, BigDecimal.ONE));
    }

    @Test
    void fineQueriesAndPayment_coverOwnershipAndStatusRules() {
        ServiceTestDoubles.Fines repository = new ServiceTestDoubles.Fines();
        Fine fine = fine("f1", "l1", "m1", BigDecimal.valueOf(3.00), FineStatus.OUTSTANDING);
        repository.save(fine);
        FineService service = new FineService(repository, loanQueryFor(null), new ActiveMemberDirectory(),
                CLOCK, BigDecimal.ONE);

        assertEquals(List.of(fine), service.getMemberFines("m1"));
        assertEquals(List.of(fine), service.getAllFines());
        assertThrows(IllegalArgumentException.class, () -> service.payFine("f1", "m2"));

        Fine paid = service.payFine("f1", "m1");
        assertEquals(FineStatus.PAID, paid.getStatus());
        assertThrows(IllegalStateException.class, () -> service.payFine("f1", "m1"));
    }

    @Test
    void getBookTitle_prefersPersistedTitleAndHasFallback() {
        FineService service = new FineService(new ServiceTestDoubles.Fines(), loanQueryFor(null),
                new ActiveMemberDirectory(), CLOCK, BigDecimal.ONE);
        Fine fine = fine("f1", "l1", "m1", BigDecimal.ONE, FineStatus.OUTSTANDING);

        assertEquals("Algorithms", new FineService(new ServiceTestDoubles.Fines(),
                loanQueryFor(new LoanSummary("l1", "m1", "c1", "Algorithms",
                        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 18))),
                new ActiveMemberDirectory(), CLOCK, BigDecimal.ONE).getBookTitle("l1"));
        assertEquals("Algorithms", service.getBookTitle(fine.withAmount(BigDecimal.ONE)));
        assertEquals("Book information unavailable", service.getBookTitle("missing"));
    }

    @Test
    void editFine_validatesAmountAndOutstandingStatus() {
        ServiceTestDoubles.Fines repository = new ServiceTestDoubles.Fines();
        repository.save(fine("f1", "l1", "m1", BigDecimal.ONE, FineStatus.OUTSTANDING));
        repository.save(fine("f2", "l2", "m1", BigDecimal.ONE, FineStatus.PAID));
        FineService service = new FineService(repository, loanQueryFor(null), new ActiveMemberDirectory(),
                CLOCK, BigDecimal.ONE);

        assertEquals(BigDecimal.valueOf(2.35).setScale(2), service.editFine("f1", BigDecimal.valueOf(2.3456))
                .getAmount());
        assertThrows(IllegalArgumentException.class, () -> service.editFine("f1", BigDecimal.valueOf(-1)));
        assertThrows(IllegalStateException.class, () -> service.editFine("f2", BigDecimal.ONE));
    }

    @Test
    void removeFine_returnsWhetherFineExisted() {
        ServiceTestDoubles.Fines repository = new ServiceTestDoubles.Fines();
        repository.save(fine("f1", "l1", "m1", BigDecimal.ONE, FineStatus.OUTSTANDING));
        FineService service = new FineService(repository, loanQueryFor(null), new ActiveMemberDirectory(),
                CLOCK, BigDecimal.ONE);

        assertEquals(true, service.removeFine("f1"));
        assertEquals(false, service.removeFine("f1"));
        assertThrows(IllegalArgumentException.class, () -> service.removeFine(" "));
    }

    private static Fine fine(String fineId, String loanId, String memberId, BigDecimal amount,
                             FineStatus status) {
        return new Fine(fineId, loanId, memberId, amount, "Overdue loan", status,
                LocalDate.of(2026, 9, 21), "Algorithms");
    }

    private static LoanQuery loanQueryFor(LoanSummary loan) {
        return new LoanQuery() {
            @Override
            public Optional<LoanSummary> findById(String loanId) {
                return loan == null || !loan.getLoanId().equals(loanId) ? Optional.empty() : Optional.of(loan);
            }

            @Override
            public List<LoanSummary> findActiveLoans() {
                return loan == null ? List.of() : List.of(loan);
            }

            @Override
            public List<LoanSummary> findOverdueLoans(LocalDate date) {
                return loan == null ? List.of() : List.of(loan);
            }
        };
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
