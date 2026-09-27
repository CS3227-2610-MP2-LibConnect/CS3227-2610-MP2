package libconnect.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** Represents a monetary fine associated with a member's loan. */
public final class Fine implements libconnect.storage.repositories.Identifiable {
    private final String fineId;
    private final String loanId;
    private final String memberId;
    private final String bookTitle;
    private final BigDecimal amount;
    private final String reason;
    private final FineStatus status;
    private final LocalDate issuedDate;

    /** Creates a fine with a non-negative amount and valid identifying data. */
    public Fine(String fineId, String loanId, String memberId, BigDecimal amount,
                String reason, FineStatus status, LocalDate issuedDate) {
        this(fineId, loanId, memberId, amount, reason, status, issuedDate, null);
    }

    /** Creates a fine with the title of the overdue book. */
    @JsonCreator
    public Fine(@JsonProperty("fineId") String fineId,
                @JsonProperty("loanId") String loanId,
                @JsonProperty("memberId") String memberId,
                @JsonProperty("amount") BigDecimal amount,
                @JsonProperty("reason") String reason,
                @JsonProperty("status") FineStatus status,
                @JsonProperty("issuedDate") LocalDate issuedDate,
                @JsonProperty("bookTitle") String bookTitle) {
        this.fineId = requireText(fineId, "fineId");
        this.loanId = requireText(loanId, "loanId");
        this.memberId = requireText(memberId, "memberId");
        this.bookTitle = normalizeBookTitle(bookTitle);
        this.amount = Objects.requireNonNull(amount, "amount");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        this.reason = requireText(reason, "reason");
        this.status = Objects.requireNonNull(status, "status");
        this.issuedDate = Objects.requireNonNull(issuedDate, "issuedDate");
    }

    /** Returns the stable fine identifier. */
    @Override
    @JsonProperty("fineId")
    public String getId() {
        return fineId;
    }

    /** Returns the associated loan identifier. */
    public String getLoanId() {
        return loanId;
    }

    /** Returns the member identifier charged by this fine. */
    public String getMemberId() {
        return memberId;
    }

    /** Returns the title of the book associated with this fine, if it was persisted. */
    public String getBookTitle() {
        return bookTitle;
    }

    /** Returns the fine amount. */
    public BigDecimal getAmount() {
        return amount;
    }

    /** Returns the reason for issuing the fine. */
    public String getReason() {
        return reason;
    }

    /** Returns the current fine status. */
    public FineStatus getStatus() {
        return status;
    }

    /** Returns the date on which the fine was issued. */
    public LocalDate getIssuedDate() {
        return issuedDate;
    }

    /** Returns a copy with a new amount. */
    public Fine withAmount(BigDecimal newAmount) {
        return new Fine(fineId, loanId, memberId, newAmount, reason, status, issuedDate, bookTitle);
    }

    /** Returns a copy marked as waived. */
    public Fine waive() {
        return new Fine(fineId, loanId, memberId, amount, reason, FineStatus.WAIVED, issuedDate, bookTitle);
    }

    /** Returns a copy marked as paid. */
    public Fine pay() {
        return new Fine(fineId, loanId, memberId, amount, reason, FineStatus.PAID, issuedDate, bookTitle);
    }

    private static String normalizeBookTitle(String title) {
        return title == null || title.isBlank() ? null : title;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
