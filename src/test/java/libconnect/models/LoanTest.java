package libconnect.models;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LoanTest {
    private static final LocalDate BORROW_DATE = LocalDate.of(2026, 1, 1);

    @Test
    void constructor_validValues_setsDefaultDueDate() {
        Loan loan = createLoan();

        assertAll(
                () -> assertEquals("LOAN-1", loan.getLoanId()),
                () -> assertEquals("MEMBER-1", loan.getMemberId()),
                () -> assertEquals("COPY-1", loan.getCopyId()),
                () -> assertEquals(BORROW_DATE, loan.getBorrowDate()),
                () -> assertEquals(BORROW_DATE.plusDays(30), loan.getDueDate()),
                () -> assertNull(loan.getReturnDate()),
                () -> assertEquals(LoanStatus.ACTIVE, loan.getStatus()),
                () -> assertFalse(loan.hasBeenRenewed()),
                () -> assertTrue(loan.isActive()));
    }

    @Test
    void constructor_borrowDateAtDateBoundary_calculatesDueDateCorrectly() {
        Loan endOfMonthLoan = new Loan("LOAN-1", "MEMBER-1", "COPY-1",
                LocalDate.of(2026, 1, 31));
        Loan endOfYearLoan = new Loan("LOAN-2", "MEMBER-1", "COPY-2",
                LocalDate.of(2026, 12, 31));
        Loan leapDayLoan = new Loan("LOAN-3", "MEMBER-1", "COPY-3",
                LocalDate.of(2028, 2, 29));

        assertAll(
                () -> assertEquals(LocalDate.of(2026, 3, 2), endOfMonthLoan.getDueDate()),
                () -> assertEquals(LocalDate.of(2027, 1, 30), endOfYearLoan.getDueDate()),
                () -> assertEquals(LocalDate.of(2028, 3, 30), leapDayLoan.getDueDate()));
    }

    @Test
    void constructor_nullBorrowDate_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new Loan("LOAN-1", "MEMBER-1", "COPY-1", null));
    }

    @ParameterizedTest
    @MethodSource("invalidIdentifiers")
    void constructor_invalidIdentifier_throwsIllegalArgumentException(
            String loanId, String memberId, String copyId) {
        assertThrows(IllegalArgumentException.class,
                () -> new Loan(loanId, memberId, copyId, BORROW_DATE));
    }

    private static Stream<Arguments> invalidIdentifiers() {
        return Stream.of(
                Arguments.of(null, "MEMBER-1", "COPY-1"),
                Arguments.of("", "MEMBER-1", "COPY-1"),
                Arguments.of("   ", "MEMBER-1", "COPY-1"),
                Arguments.of("LOAN-1", null, "COPY-1"),
                Arguments.of("LOAN-1", "", "COPY-1"),
                Arguments.of("LOAN-1", "   ", "COPY-1"),
                Arguments.of("LOAN-1", "MEMBER-1", null),
                Arguments.of("LOAN-1", "MEMBER-1", ""),
                Arguments.of("LOAN-1", "MEMBER-1", "   "));
    }

    @Test
    void constructor_explicitStatePreservesSuppliedDueDate() {
        LocalDate dueDate = LocalDate.of(2026, 2, 15);
        LocalDate returnDate = LocalDate.of(2026, 2, 20);
        Loan loan = new Loan("LOAN-1", "MEMBER-1", "COPY-1", BORROW_DATE,
                dueDate, returnDate, LoanStatus.RETURNED, true);

        assertAll(
                () -> assertEquals(dueDate, loan.getDueDate()),
                () -> assertEquals(returnDate, loan.getReturnDate()),
                () -> assertEquals(LoanStatus.RETURNED, loan.getStatus()),
                () -> assertTrue(loan.hasBeenRenewed()));
    }

    @Test
    void constructor_dueDateBeforeBorrowDate_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Loan("LOAN-1", "MEMBER-1", "COPY-1", BORROW_DATE,
                        BORROW_DATE.minusDays(1), null, LoanStatus.ACTIVE, false));
    }

    @Test
    void renew_defaultLoan_extendsDueDateBy30Days() {
        Loan loan = createLoan();
        LocalDate originalDueDate = loan.getDueDate();

        loan.renew();

        assertAll(
                () -> assertEquals(originalDueDate.plusDays(30), loan.getDueDate()),
                () -> assertTrue(loan.hasBeenRenewed()));
    }

    @Test
    void renew_alreadyRenewedLoan_throwsIllegalStateException() {
        Loan loan = createLoan();
        loan.renew();

        assertThrows(IllegalStateException.class, loan::renew);
    }

    @Test
    void renew_returnedLoan_throwsIllegalStateException() {
        Loan loan = createLoan();
        loan.returnBook(BORROW_DATE.plusDays(5));

        assertThrows(IllegalStateException.class, loan::renew);
    }

    @Test
    void returnBook_validDate_marksLoanReturned() {
        Loan loan = createLoan();
        LocalDate returnDate = BORROW_DATE.plusDays(5);

        loan.returnBook(returnDate);

        assertAll(
                () -> assertEquals(returnDate, loan.getReturnDate()),
                () -> assertEquals(LoanStatus.RETURNED, loan.getStatus()),
                () -> assertFalse(loan.isActive()));
    }

    @Test
    void returnBook_nullOrBeforeBorrowDate_throwsException() {
        Loan loan = createLoan();

        assertAll(
                () -> assertThrows(NullPointerException.class, () -> loan.returnBook(null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> loan.returnBook(BORROW_DATE.minusDays(1))));
    }

    @Test
    void returnBook_alreadyReturned_throwsIllegalStateException() {
        Loan loan = createLoan();
        LocalDate originalReturnDate = BORROW_DATE.plusDays(5);
        loan.returnBook(originalReturnDate);

        assertThrows(IllegalStateException.class,
                () -> loan.returnBook(BORROW_DATE.plusDays(6)));
        assertEquals(originalReturnDate, loan.getReturnDate());
    }

    @Test
    void activeDefaultLoan_notOverdueOnDueDate() {
        LocalDate dueDate = LocalDate.now();
        Loan loan = new Loan("LOAN-1", "MEMBER-1", "COPY-1",
                dueDate.minusDays(30));

        assertFalse(loan.isOverdue());
        assertEquals(0, loan.calculateOverdueDays());
    }

    @Test
    void activeLoan_overdueAfterDueDate() {
        LocalDate dueDate = LocalDate.now().minusDays(4);
        Loan loan = new Loan("LOAN-1", "MEMBER-1", "COPY-1", dueDate,
                dueDate, null, LoanStatus.ACTIVE, false);

        assertTrue(loan.isOverdue());
        assertEquals(4, loan.calculateOverdueDays());
    }

    @Test
    void returnedLoan_overdueCalculationUsesReturnDate() {
        LocalDate dueDate = LocalDate.of(2026, 1, 10);
        LocalDate returnDate = LocalDate.of(2026, 1, 13);
        Loan loan = new Loan("LOAN-1", "MEMBER-1", "COPY-1", BORROW_DATE,
                dueDate, returnDate, LoanStatus.RETURNED, false);

        assertTrue(loan.isOverdue());
        assertEquals(3, loan.calculateOverdueDays());
    }

    @Test
    void equals_sameLoanId_returnsTrue() {
        Loan firstLoan = createLoan();
        Loan secondLoan = new Loan("LOAN-1", "OTHER-MEMBER", "OTHER-COPY",
                BORROW_DATE.plusDays(1));

        assertEquals(firstLoan, secondLoan);
    }

    @Test
    void equals_differentLoanId_returnsFalse() {
        Loan firstLoan = createLoan();
        Loan secondLoan = new Loan("LOAN-2", "MEMBER-1", "COPY-1", BORROW_DATE);

        assertNotEquals(firstLoan, secondLoan);
    }

    @Test
    void equals_sameInstanceNullOrDifferentType_behavesCorrectly() {
        Loan loan = createLoan();

        assertAll(
                () -> assertEquals(loan, loan),
                () -> assertFalse(loan.equals(null)),
                () -> assertFalse(loan.equals("not a loan")));
    }

    @Test
    void hashCode_equalLoansHaveSameHashCode() {
        Loan firstLoan = createLoan();
        Loan secondLoan = new Loan("LOAN-1", "OTHER-MEMBER", "OTHER-COPY",
                BORROW_DATE.plusDays(1));

        assertEquals(firstLoan.hashCode(), secondLoan.hashCode());
    }

    @Test
    void toString_containsLoanDetails() {
        Loan loan = createLoan();
        String representation = loan.toString();

        assertAll(
                () -> assertTrue(representation.contains("LOAN-1")),
                () -> assertTrue(representation.contains("MEMBER-1")),
                () -> assertTrue(representation.contains("COPY-1")),
                () -> assertTrue(representation.contains(BORROW_DATE.toString())),
                () -> assertTrue(representation.contains(BORROW_DATE.plusDays(30).toString())),
                () -> assertTrue(representation.contains("ACTIVE")),
                () -> assertTrue(representation.contains("false")));
    }

    private static Loan createLoan() {
        return new Loan("LOAN-1", "MEMBER-1", "COPY-1", BORROW_DATE);
    }
}
