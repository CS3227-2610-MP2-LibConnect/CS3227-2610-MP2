package libconnect.services.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import libconnect.integration.FineIssuer;
import libconnect.integration.ReturnedLoanSummary;
import libconnect.models.BookCopy;
import libconnect.models.CopyStatus;
import libconnect.models.Fine;
import libconnect.models.FineStatus;
import libconnect.models.Loan;
import libconnect.models.LoanStatus;
import libconnect.services.BorrowService;
import libconnect.services.NotFoundException;
import libconnect.services.ServiceException;

/** Tests multi-copy borrowing and compensating return transactions. */
class BorrowServiceTest {
    private static final LocalDate BORROW_DATE = LocalDate.of(2026, 1, 1);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-02-05T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void borrowCopies_oneOrManyAvailableCopies_persistsCoordinatedState() {
        ServiceUnitTestDoubles.Copies copies = new ServiceUnitTestDoubles.Copies();
        ServiceUnitTestDoubles.Loans loans = new ServiceUnitTestDoubles.Loans();
        copies.save(copy("COPY-1"));
        copies.save(copy("COPY-2"));
        BorrowService service = new BorrowService(copies, loans, new RecordingFineIssuer(), CLOCK);

        service.borrowCopies("MEMBER-1", List.of("COPY-1", "COPY-2"), BORROW_DATE);

        assertEquals(2, loans.findAll().size());
        assertTrue(copies.findAll().stream().allMatch(copy -> copy.getStatus() == CopyStatus.BORROWED));
        assertTrue(loans.findAll().stream().allMatch(loan -> loan.getMemberId().equals("MEMBER-1")));
    }

    @Test
    void borrowCopies_tenCopiesSucceedsAndElevenCopiesFails() {
        ServiceUnitTestDoubles.Copies copies = new ServiceUnitTestDoubles.Copies();
        ServiceUnitTestDoubles.Loans loans = new ServiceUnitTestDoubles.Loans();
        List<String> copyIds = new ArrayList<>();
        for (int index = 1; index <= 11; index++) {
            String copyId = "COPY-" + index;
            copyIds.add(copyId);
            copies.save(copy(copyId));
        }
        BorrowService service = new BorrowService(copies, loans, new RecordingFineIssuer(), CLOCK);

        service.borrowCopies("MEMBER-1", copyIds.subList(0, 10), BORROW_DATE);
        assertEquals(10, loans.findAll().size());
        assertThrows(IllegalArgumentException.class,
                () -> service.borrowCopies("MEMBER-1", copyIds, BORROW_DATE));
    }

    @Test
    void borrowCopies_invalidOrUnavailableInput_performsNoWrites() {
        ServiceUnitTestDoubles.Copies copies = new ServiceUnitTestDoubles.Copies();
        ServiceUnitTestDoubles.Loans loans = new ServiceUnitTestDoubles.Loans();
        copies.save(copy("COPY-1"));
        BookCopy unavailable = new BookCopy("COPY-2", "978-2", CopyStatus.LOST, "A-2");
        copies.save(unavailable);
        BorrowService service = new BorrowService(copies, loans, new RecordingFineIssuer(), CLOCK);

        assertThrows(IllegalArgumentException.class,
                () -> service.borrowCopies(" ", List.of("COPY-1"), BORROW_DATE));
        assertThrows(IllegalArgumentException.class,
                () -> service.borrowCopies("MEMBER-1", List.of(), BORROW_DATE));
        assertThrows(NullPointerException.class,
                () -> service.borrowCopies("MEMBER-1", null, BORROW_DATE));
        assertThrows(NullPointerException.class,
                () -> service.borrowCopies("MEMBER-1", List.of("COPY-1"), null));
        assertThrows(IllegalArgumentException.class,
                () -> service.borrowCopies("MEMBER-1", List.of("COPY-1", "COPY-1"), BORROW_DATE));
        assertThrows(NotFoundException.class,
                () -> service.borrowCopies("MEMBER-1", List.of("COPY-unknown"), BORROW_DATE));
        assertThrows(ServiceException.class,
                () -> service.borrowCopies("MEMBER-1", List.of("COPY-2"), BORROW_DATE));
        assertEquals(0, loans.findAll().size());
        assertEquals(CopyStatus.AVAILABLE, copies.findById("COPY-1").orElseThrow().getStatus());
    }

    @Test
    void borrowCopies_loanPersistenceFailure_rollsBackCopiesAndLoans() {
        ServiceUnitTestDoubles.Copies copies = new ServiceUnitTestDoubles.Copies();
        ServiceUnitTestDoubles.Loans loans = new ServiceUnitTestDoubles.Loans();
        copies.save(copy("COPY-1"));
        loans.saveFailuresRemaining = 1;
        BorrowService service = new BorrowService(copies, loans, new RecordingFineIssuer(), CLOCK);

        assertThrows(ServiceException.class,
                () -> service.borrowCopies("MEMBER-1", List.of("COPY-1"), BORROW_DATE));

        assertEquals(0, loans.findAll().size());
        assertEquals(CopyStatus.AVAILABLE, copies.findById("COPY-1").orElseThrow().getStatus());
    }

    @Test
    void borrowCopies_copyPersistenceFailure_rollsBackCopyAndLoans() {
        ServiceUnitTestDoubles.Copies copies = new ServiceUnitTestDoubles.Copies();
        ServiceUnitTestDoubles.Loans loans = new ServiceUnitTestDoubles.Loans();
        copies.save(copy("COPY-1"));
        copies.saveFailuresRemaining = 1;
        BorrowService service = new BorrowService(copies, loans, new RecordingFineIssuer(), CLOCK);

        assertThrows(ServiceException.class,
                () -> service.borrowCopies("MEMBER-1", List.of("COPY-1"), BORROW_DATE));

        assertEquals(0, loans.findAll().size());
        assertEquals(CopyStatus.AVAILABLE, copies.findById("COPY-1").orElseThrow().getStatus());
    }

    @Test
    void borrowCopies_rollbackFailure_reportsRollbackFailure() {
        ServiceUnitTestDoubles.Copies copies = new ServiceUnitTestDoubles.Copies();
        ServiceUnitTestDoubles.Loans loans = new ServiceUnitTestDoubles.Loans();
        copies.save(copy("COPY-1"));
        copies.saveFailuresRemaining = 2;
        BorrowService service = new BorrowService(copies, loans, new RecordingFineIssuer(), CLOCK);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.borrowCopies("MEMBER-1", List.of("COPY-1"), BORROW_DATE));

        assertTrue(exception.getMessage().contains("restore its changes"));
    }

    @Test
    void returnLoan_overdueLoan_makesCopyAvailableAndIssuesFine() {
        ServiceUnitTestDoubles.Copies copies = new ServiceUnitTestDoubles.Copies();
        ServiceUnitTestDoubles.Loans loans = new ServiceUnitTestDoubles.Loans();
        RecordingFineIssuer fineIssuer = new RecordingFineIssuer();
        copies.save(new BookCopy("COPY-1", "978-1", CopyStatus.BORROWED, "A-1"));
        loans.save(new Loan("LOAN-1", "MEMBER-1", "COPY-1", BORROW_DATE));
        BorrowService service = new BorrowService(copies, loans, fineIssuer, CLOCK);

        service.returnLoan("LOAN-1");

        assertEquals(LoanStatus.RETURNED, loans.findById("LOAN-1").orElseThrow().getStatus());
        assertEquals(CopyStatus.AVAILABLE, copies.findById("COPY-1").orElseThrow().getStatus());
        assertEquals("LOAN-1", fineIssuer.returnedLoan.getLoanId());
    }

    @Test
    void returnLoan_nonOverdueLoan_doesNotIssueFine() {
        ServiceUnitTestDoubles.Copies copies = new ServiceUnitTestDoubles.Copies();
        ServiceUnitTestDoubles.Loans loans = new ServiceUnitTestDoubles.Loans();
        RecordingFineIssuer fineIssuer = new RecordingFineIssuer();
        copies.save(new BookCopy("COPY-1", "978-1", CopyStatus.BORROWED, "A-1"));
        loans.save(new Loan("LOAN-1", "MEMBER-1", "COPY-1", LocalDate.of(2026, 1, 20)));
        BorrowService service = new BorrowService(copies, loans, fineIssuer, CLOCK);

        service.returnLoan("LOAN-1");

        assertEquals(null, fineIssuer.returnedLoan);
    }

    @Test
    void returnLoan_missingOrReturnedLoan_rejected() {
        ServiceUnitTestDoubles.Copies copies = new ServiceUnitTestDoubles.Copies();
        ServiceUnitTestDoubles.Loans loans = new ServiceUnitTestDoubles.Loans();
        BorrowService service = new BorrowService(copies, loans, new RecordingFineIssuer(), CLOCK);

        assertThrows(NotFoundException.class, () -> service.returnLoan("LOAN-unknown"));
        loans.save(new Loan("LOAN-1", "MEMBER-1", "COPY-1", BORROW_DATE,
                LocalDate.of(2026, 1, 31), LocalDate.of(2026, 1, 20), LoanStatus.RETURNED, false));
        assertThrows(ServiceException.class, () -> service.returnLoan("LOAN-1"));
    }

    @Test
    void returnLoan_missingBookCopy_rejected() {
        ServiceUnitTestDoubles.Copies copies = new ServiceUnitTestDoubles.Copies();
        ServiceUnitTestDoubles.Loans loans = new ServiceUnitTestDoubles.Loans();
        loans.save(new Loan("LOAN-1", "MEMBER-1", "COPY-unknown", BORROW_DATE));
        BorrowService service = new BorrowService(copies, loans, new RecordingFineIssuer(), CLOCK);

        assertThrows(NotFoundException.class, () -> service.returnLoan("LOAN-1"));
    }

    @Test
    void returnLoan_persistenceFailure_restoresOriginalRecords() {
        ServiceUnitTestDoubles.Copies copies = new ServiceUnitTestDoubles.Copies();
        ServiceUnitTestDoubles.Loans loans = new ServiceUnitTestDoubles.Loans();
        copies.save(new BookCopy("COPY-1", "978-1", CopyStatus.BORROWED, "A-1"));
        loans.save(new Loan("LOAN-1", "MEMBER-1", "COPY-1", BORROW_DATE));
        loans.saveFailuresRemaining = 1;
        BorrowService service = new BorrowService(copies, loans, new RecordingFineIssuer(), CLOCK);

        assertThrows(ServiceException.class, () -> service.returnLoan("LOAN-1"));

        assertEquals(LoanStatus.ACTIVE, loans.findById("LOAN-1").orElseThrow().getStatus());
        assertEquals(CopyStatus.BORROWED, copies.findById("COPY-1").orElseThrow().getStatus());
    }

    @Test
    void returnLoan_fineIssuanceFailure_restoresOriginalRecords() {
        ServiceUnitTestDoubles.Copies copies = new ServiceUnitTestDoubles.Copies();
        ServiceUnitTestDoubles.Loans loans = new ServiceUnitTestDoubles.Loans();
        copies.save(new BookCopy("COPY-1", "978-1", CopyStatus.BORROWED, "A-1"));
        loans.save(new Loan("LOAN-1", "MEMBER-1", "COPY-1", BORROW_DATE));
        FineIssuer failingFineIssuer = returnedLoan -> {
            throw new ServiceException("fine storage failure");
        };
        BorrowService service = new BorrowService(copies, loans, failingFineIssuer, CLOCK);

        assertThrows(ServiceException.class, () -> service.returnLoan("LOAN-1"));

        assertEquals(LoanStatus.ACTIVE, loans.findById("LOAN-1").orElseThrow().getStatus());
        assertEquals(CopyStatus.BORROWED, copies.findById("COPY-1").orElseThrow().getStatus());
    }

    private static BookCopy copy(String copyId) {
        return new BookCopy(copyId, "978-1", CopyStatus.AVAILABLE, "A-1");
    }

    private static final class RecordingFineIssuer implements FineIssuer {
        private ReturnedLoanSummary returnedLoan;

        @Override
        public Fine issueForReturnedLoan(ReturnedLoanSummary loan) {
            returnedLoan = loan;
            return new Fine("FINE-1", loan.getLoanId(), loan.getMemberId(), BigDecimal.ONE,
                    "Overdue loan", FineStatus.OUTSTANDING, loan.getReturnDate());
        }
    }
}
