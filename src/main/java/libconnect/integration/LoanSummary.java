package libconnect.integration;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** Exposes stable loan information needed by librarian services. */
public final class LoanSummary {
    private final String loanId;
    private final String memberId;
    private final String bookCopyId;
    private final LocalDate dueDate;

    /** Creates a loan summary for cross-role queries. */
    public LoanSummary(String loanId, String memberId, String bookCopyId, LocalDate dueDate) {
        this.loanId = requireText(loanId, "loanId");
        this.memberId = requireText(memberId, "memberId");
        this.bookCopyId = requireText(bookCopyId, "bookCopyId");
        this.dueDate = Objects.requireNonNull(dueDate, "dueDate");
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
