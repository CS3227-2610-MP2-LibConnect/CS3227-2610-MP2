package libconnect.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import libconnect.util.ValidationUtils;

/**
 * Represents a member's loan of one physical book copy.
 */
public class Loan implements libconnect.storage.repositories.Identifiable {
    private static final long LOAN_PERIOD_DAYS = 30;
    private static final long RENEWAL_PERIOD_DAYS = 30;

    private final String loanId;
    private final String memberId;
    private final String copyId;
    private final LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private LoanStatus status;
    private boolean isRenewed;

    /**
     * Creates an active loan that is due 30 days after the borrow date.
     *
     * @param loanId the stable identifier for the loan.
     * @param memberId the identifier of the borrowing member.
     * @param copyId the identifier of the borrowed book copy.
     * @param borrowDate the date on which the book was borrowed.
     * @throws IllegalArgumentException if an identifier is blank.
     * @throws NullPointerException if a date is null.
     */
    public Loan(String loanId, String memberId, String copyId,
                LocalDate borrowDate) {
        this(loanId, memberId, copyId, Objects.requireNonNull(borrowDate, "borrowDate cannot be null"),
                borrowDate.plusDays(LOAN_PERIOD_DAYS),
                null, LoanStatus.ACTIVE, false);
    }

    /**
     * Creates a loan with an explicitly supplied state.
     *
     * <p>This constructor is useful when reconstructing a loan from persistence.</p>
     *
     * @param loanId the stable identifier for the loan.
     * @param memberId the identifier of the borrowing member.
     * @param copyId the identifier of the borrowed book copy.
     * @param borrowDate the date on which the book was borrowed.
     * @param dueDate the date on which the book was due.
     * @param returnDate the date on which the book was returned, or null if active.
     * @param status the current loan status.
     * @param isRenewed whether this loan has already been renewed.
     * @throws IllegalArgumentException if an identifier is blank or a date relationship is invalid.
     * @throws NullPointerException if a required date or status is null.
     */
    @JsonCreator
    public Loan(@JsonProperty("loanId") String loanId,
                @JsonProperty("memberId") String memberId,
                @JsonProperty("copyId") String copyId,
                @JsonProperty("borrowDate") LocalDate borrowDate,
                @JsonProperty("dueDate") LocalDate dueDate,
                @JsonProperty("returnDate") LocalDate returnDate,
                @JsonProperty("status") LoanStatus status,
                @JsonProperty("isRenewed") boolean isRenewed) {
        this.loanId = ValidationUtils.requireNonBlank(loanId, "loanId");
        this.memberId = ValidationUtils.requireNonBlank(memberId, "memberId");
        this.copyId = ValidationUtils.requireNonBlank(copyId, "copyId");
        this.borrowDate = Objects.requireNonNull(borrowDate, "borrowDate cannot be null");
        this.dueDate = Objects.requireNonNull(dueDate, "dueDate cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        validateDateOrder();
        validateReturnState(returnDate);
        this.returnDate = returnDate;
        this.isRenewed = isRenewed;
    }

    /**
     * Returns the stable identifier for this loan.
     *
     * @return the loan ID.
     */
    public String getLoanId() {
        return loanId;
    }

    /**
     * Returns the identifier of the borrowing member.
     *
     * @return the member ID.
     */
    public String getMemberId() {
        return memberId;
    }

    /**
     * Returns the identifier of the borrowed book copy.
     *
     * @return the copy ID.
     */
    public String getCopyId() {
        return copyId;
    }

    /**
     * Returns the date on which the book was borrowed.
     *
     * @return the borrow date.
     */
    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    /**
     * Returns the current due date.
     *
     * @return the due date.
     */
    public LocalDate getDueDate() {
        return dueDate;
    }

    /**
     * Returns the return date, or null if the loan is still active.
     *
     * @return the return date or null.
     */
    public LocalDate getReturnDate() {
        return returnDate;
    }

    /**
     * Returns the current loan status.
     *
     * @return the loan status.
     */
    public LoanStatus getStatus() {
        return status;
    }

    /**
     * Returns whether this loan has already been renewed.
     *
     * @return true if the loan has been renewed.
     */
    @JsonProperty("isRenewed")
    public boolean hasBeenRenewed() {
        return isRenewed;
    }

    /**
     * Returns whether this loan is currently active.
     *
     * @return true if the loan is active.
     */
    public boolean isActive() {
        return status == LoanStatus.ACTIVE;
    }

    /**
     * Renews this active loan by 30 days.
     *
     * @throws IllegalStateException if the loan is returned or has already been renewed.
     */
    public void renew() {
        if (!isActive()) {
            throw new IllegalStateException("Only an active loan can be renewed");
        }
        if (isRenewed) {
            throw new IllegalStateException("A loan can only be renewed once");
        }

        dueDate = dueDate.plusDays(RENEWAL_PERIOD_DAYS);
        isRenewed = true;
    }

    /**
     * Records the return date for this active loan.
     *
     * @param returnDate the date on which the book was returned.
     * @throws IllegalArgumentException if the return date precedes the borrow date.
     * @throws IllegalStateException if the loan has already been returned.
     * @throws NullPointerException if returnDate is null.
     */
    public void returnBook(LocalDate returnDate) {
        Objects.requireNonNull(returnDate, "returnDate cannot be null");
        if (!isActive()) {
            throw new IllegalStateException("Only an active loan can be returned");
        }
        if (returnDate.isBefore(borrowDate)) {
            throw new IllegalArgumentException("returnDate cannot precede borrowDate");
        }

        this.returnDate = returnDate;
        status = LoanStatus.RETURNED;
    }

    /**
     * Returns whether the loan is overdue as of the current date.
     *
     * <p>For a returned loan, the return date is used. For an active loan, the current date
     * is used.</p>
     *
     * @return true if the applicable date is after the due date.
     */
    public boolean isOverdue() {
        return calculateOverdueDays() > 0;
    }

    /**
     * Calculates the number of days by which the loan is overdue.
     *
     * @return the number of overdue days, or zero if the loan is not overdue.
     */
    public long calculateOverdueDays() {
        LocalDate comparisonDate = isActive() ? LocalDate.now() : returnDate;
        return Math.max(0, ChronoUnit.DAYS.between(dueDate, comparisonDate));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Loan otherLoan)) {
            return false;
        }
        return loanId.equals(otherLoan.loanId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return loanId.hashCode();
    }

    @Override
    public String toString() {
        return "Loan{" + "loanId='" + loanId + '\'' + ", memberId='" + memberId + '\''
                + ", copyId='" + copyId + '\'' + ", borrowDate=" + borrowDate
                + ", dueDate=" + dueDate + ", returnDate=" + returnDate
                + ", status=" + status + ", isRenewed=" + isRenewed + '}';
    }

    private void validateDateOrder() {
        if (dueDate.isBefore(borrowDate)) {
            throw new IllegalArgumentException("dueDate cannot precede borrowDate");
        }
    }

    private void validateReturnState(LocalDate returnDate) {
        if (status == LoanStatus.ACTIVE && returnDate != null) {
            throw new IllegalArgumentException("An active loan cannot have a return date");
        }
        if (status == LoanStatus.RETURNED && returnDate == null) {
            throw new IllegalArgumentException("A returned loan must have a return date");
        }
        if (returnDate != null && returnDate.isBefore(borrowDate)) {
            throw new IllegalArgumentException("returnDate cannot precede borrowDate");
        }
    }

    @Override 
    public String getId() {
        return loanId;
    }

}
