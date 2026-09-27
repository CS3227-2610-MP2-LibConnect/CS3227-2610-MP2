package libconnect.integration;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** Exposes stable loan information needed by librarian services. */
public final class LoanSummary {
    private final String loanId;
    private final String memberId;
    private final String bookCopyId;
    private final String bookName;
    private final LocalDate borrowDate;
    private final LocalDate dueDate;

    /** Creates a loan summary for cross-role queries. */
    public LoanSummary(String loanId, String memberId, String bookCopyId, LocalDate dueDate) {
        this(loanId, memberId, bookCopyId, bookCopyId, dueDate.minusDays(30), dueDate);
    }

    /** Creates a loan summary with the book details required for member notifications. */
    public LoanSummary(String loanId, String memberId, String bookCopyId, String bookName,
                       LocalDate borrowDate, LocalDate dueDate) {
        this.loanId = requireText(loanId, "loanId");
        this.memberId = requireText(memberId, "memberId");
        this.bookCopyId = requireText(bookCopyId, "bookCopyId");
        this.bookName = requireText(bookName, "bookName");
        this.borrowDate = Objects.requireNonNull(borrowDate, "borrowDate");
        this.dueDate = Objects.requireNonNull(dueDate, "dueDate");
        if (dueDate.isBefore(borrowDate)) {
            throw new IllegalArgumentException("dueDate cannot precede borrowDate");
        }
    }

    /** Returns the stable loan identifier. */
    public String getLoanId() {
        return loanId;
    }

    /** Returns the member identifier associated with the loan. */
    public String getMemberId() {
        return memberId;
    }

    /** Returns the borrowed book-copy identifier. */
    public String getBookCopyId() {
        return bookCopyId;
    }

    /** Returns the name of the borrowed book. */
    public String getBookName() {
        return bookName;
    }

    /** Returns the date on which the book was borrowed. */
    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    /** Returns the loan due date. */
    public LocalDate getDueDate() {
        return dueDate;
    }

    /** Returns whether this loan is overdue on the supplied date. */
    public boolean isOverdueOn(LocalDate date) {
        return date.isAfter(dueDate);
    }

    /** Returns the number of overdue days on the supplied date, or zero if not overdue. */
    public int getOverdueDaysOn(LocalDate date) {
        if (!isOverdueOn(date)) {
            return 0;
        }
        return Math.toIntExact(ChronoUnit.DAYS.between(dueDate, date));
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
