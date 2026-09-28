package libconnect.integration;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** Exposes the dates and identifiers needed to create a fine after a loan is returned. */
public final class ReturnedLoanSummary {
    private final String loanId;
    private final String memberId;
    private final LocalDate dueDate;
    private final LocalDate returnDate;

    /** Creates a summary for a returned loan. */
    public ReturnedLoanSummary(String loanId, String memberId, LocalDate dueDate,
                               LocalDate returnDate) {
        this.loanId = requireText(loanId, "loanId");
        this.memberId = requireText(memberId, "memberId");
        this.dueDate = Objects.requireNonNull(dueDate, "dueDate");
        this.returnDate = Objects.requireNonNull(returnDate, "returnDate");
        if (returnDate.isBefore(dueDate)) {
            throw new IllegalArgumentException("returnDate cannot precede dueDate");
        }
    }

    /** Returns the stable loan identifier. */
    public String getLoanId() {
        return loanId;
    }

    /** Returns the borrowing member identifier. */
    public String getMemberId() {
        return memberId;
    }

    /** Returns the loan due date. */
    public LocalDate getDueDate() {
        return dueDate;
    }

    /** Returns the actual date on which the loan was returned. */
    public LocalDate getReturnDate() {
        return returnDate;
    }

    /** Returns whether the loan was returned after its due date. */
    public boolean isOverdue() {
        return returnDate.isAfter(dueDate);
    }

    /** Returns the number of days by which the loan was overdue. */
    public long getOverdueDays() {
        return Math.max(0, ChronoUnit.DAYS.between(dueDate, returnDate));
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
