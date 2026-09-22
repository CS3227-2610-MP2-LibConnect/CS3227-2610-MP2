package libconnect.services.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import libconnect.models.Loan;
import libconnect.models.LoanStatus;
import libconnect.services.LoanService;
import libconnect.services.NotFoundException;
import libconnect.services.ServiceException;
import libconnect.storage.file.FileLoanRepository;

/** Tests loan behavior implemented by {@link LoanService}. */
class LoanServiceTest {
    @TempDir
    Path temporaryDirectory;

    private FileLoanRepository loanRepository;
    private LoanService loanService;

    @BeforeEach
    void setUp() {
        loanRepository = new FileLoanRepository(temporaryDirectory.resolve("loans.json"));
        loanService = new LoanService(loanRepository);
    }

    @Test
    void createLoan_duplicateId_returnsFalseAndPreservesOriginal() {
        Loan originalLoan = loan("LOAN-1", "MEMBER-1", "COPY-1", LocalDate.of(2026, 1, 1));
        loanRepository.save(originalLoan);

        assertThrows(ServiceException.class, () -> loanService.createLoan("LOAN-1", "MEMBER-2", "COPY-2", LocalDate.of(2026, 2, 1)));
        assertEquals(originalLoan, loanService.getLoanById("LOAN-1").orElseThrow());
    }

    @Test
    void createLoan_withoutId_generatesPersistedLoanId() {
        assertTrue(loanService.createLoan("MEMBER-1", "COPY-1", LocalDate.of(2026, 1, 1)));

        Loan createdLoan = loanRepository.findAll().get(0);
        assertTrue(createdLoan.getLoanId().startsWith("LOAN-"));
        assertEquals("MEMBER-1", createdLoan.getMemberId());
    }

    @Test
    void getLoansByUserId_matchesPersistedStringMemberId() {
        Loan loan = loan("LOAN-1", "42", "COPY-1", LocalDate.of(2026, 1, 1));
        loanRepository.save(loan);

        assertEquals(java.util.List.of(loan), loanService.getLoansByUserId(42));
    }

    @Test
    void getLoansByUserId_withoutLoans_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> loanService.getLoansByUserId(42));
    }

    @Test
    void getExpiredLoans_returnsOnlyOverdueLoans() {
        Loan overdueLoan = loan("LOAN-1", "MEMBER-1", "COPY-1", LocalDate.now().minusDays(60));
        Loan currentLoan = loan("LOAN-2", "MEMBER-1", "COPY-2", LocalDate.now());
        loanRepository.save(overdueLoan);
        loanRepository.save(currentLoan);

        assertEquals(java.util.List.of(overdueLoan), loanService.getExpiredLoans());
    }

    @Test
    void renewLoan_activeLoan_extendsDueDateAndPersistsRenewal() {
        Loan loan = loan("LOAN-1", "MEMBER-1", "COPY-1", LocalDate.of(2026, 1, 1));
        loanRepository.save(loan);

        loanService.renewLoan("LOAN-1");

        Loan renewedLoan = loanRepository.findById("LOAN-1").orElseThrow();
        assertEquals(LocalDate.of(2026, 3, 2), renewedLoan.getDueDate());
        assertTrue(renewedLoan.hasBeenRenewed());
    }

    @Test
    void renewLoan_twice_throwsServiceException() {
        loanRepository.save(loan("LOAN-1", "MEMBER-1", "COPY-1", LocalDate.of(2026, 1, 1)));
        loanService.renewLoan("LOAN-1");

        assertThrows(ServiceException.class, () -> loanService.renewLoan("LOAN-1"));
    }

    @Test
    void returnLoan_activeLoan_marksLoanReturned() {
        loanRepository.save(loan("LOAN-1", "MEMBER-1", "COPY-1", LocalDate.now()));

        loanService.returnLoan("LOAN-1");

        Loan returnedLoan = loanRepository.findById("LOAN-1").orElseThrow();
        assertEquals(LoanStatus.RETURNED, returnedLoan.getStatus());
        assertEquals(LocalDate.now(), returnedLoan.getReturnDate());
    }

    @Test
    void returnLoan_alreadyReturnedLoan_throwsServiceException() {
        LocalDate borrowDate = LocalDate.of(2026, 1, 1);
        Loan returnedLoan = new Loan("LOAN-1", "MEMBER-1", "COPY-1", borrowDate,
                borrowDate.plusDays(30), borrowDate.plusDays(10), LoanStatus.RETURNED, false);
        loanRepository.save(returnedLoan);

        assertThrows(ServiceException.class, () -> loanService.returnLoan("LOAN-1"));
    }

    @Test
    void deleteLoan_unknownId_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> loanService.deleteLoan("LOAN-unknown"));
    }

    private static Loan loan(String loanId, String memberId, String copyId, LocalDate borrowDate) {
        return new Loan(loanId, memberId, copyId, borrowDate);
    }
}
