package libconnect.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import libconnect.integration.FineIssuer;
import libconnect.integration.LoanQuery;
import libconnect.integration.LoanSummary;
import libconnect.integration.MemberDirectory;
import libconnect.integration.ReturnedLoanSummary;
import libconnect.models.Fine;
import libconnect.models.FineStatus;
import libconnect.storage.repositories.FineRepository;

/** Applies fine policy and coordinates fine persistence. */
public final class FineService implements FineIssuer {
    private final FineRepository repository;
    private final LoanQuery loanQuery;
    private final MemberDirectory memberDirectory;
    private final Clock clock;
    private final BigDecimal dailyRate;

    /** Creates a fine service with an injectable clock and daily overdue rate. */
    public FineService(FineRepository repository, LoanQuery loanQuery,
                       MemberDirectory memberDirectory, Clock clock, BigDecimal dailyRate) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.loanQuery = Objects.requireNonNull(loanQuery, "loanQuery");
        this.memberDirectory = Objects.requireNonNull(memberDirectory, "memberDirectory");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.dailyRate = Objects.requireNonNull(dailyRate, "dailyRate").setScale(2, RoundingMode.HALF_UP);
        if (this.dailyRate.signum() <= 0) {
            throw new IllegalArgumentException("dailyRate must be positive");
        }
    }

    /** Creates or returns the outstanding fine for an overdue loan. */
    public Fine createFine(String loanId) {
        LoanSummary loan = loanQuery.findById(requireText(loanId, "loanId"))
                .orElseThrow(() -> new IllegalArgumentException("Loan does not exist"));
        LocalDate today = LocalDate.now(clock);
        if (!loan.isOverdueOn(today)) {
            throw new IllegalStateException("Loan is not overdue");
        }
        if (!memberDirectory.exists(loan.getMemberId()) || !memberDirectory.isActive(loan.getMemberId())) {
            throw new IllegalStateException("Member does not exist or is inactive");
        }
        return repository.findByLoanId(loanId).stream()
                .filter(fine -> fine.getStatus() == FineStatus.OUTSTANDING)
                .findFirst()
                .orElseGet(() -> {
                    BigDecimal amount = calculateOverdueFine(loanId);
                    Fine fine = new Fine(UUID.randomUUID().toString(), loanId, loan.getMemberId(), amount,
                            "Overdue loan", FineStatus.OUTSTANDING, today, loan.getBookName());
                    repository.save(fine);
                    return fine;
                });
    }

    /** Creates or returns the outstanding fine for an overdue returned loan. */
    @Override
    public Fine issueForReturnedLoan(ReturnedLoanSummary returnedLoan) {
        Objects.requireNonNull(returnedLoan, "returnedLoan");
        if (!returnedLoan.isOverdue()) {
            throw new IllegalStateException("Loan is not overdue");
        }
        if (!memberDirectory.exists(returnedLoan.getMemberId())) {
            throw new IllegalStateException("Member does not exist");
        }

        return repository.findByLoanId(returnedLoan.getLoanId()).stream()
                .filter(fine -> fine.getStatus() == FineStatus.OUTSTANDING)
                .findFirst()
                .orElseGet(() -> {
                    BigDecimal amount = dailyRate.multiply(
                            BigDecimal.valueOf(returnedLoan.getOverdueDays()))
                            .setScale(2, RoundingMode.HALF_UP);
                    String bookTitle = getBookTitle(returnedLoan.getLoanId());
                    Fine fine = new Fine(UUID.randomUUID().toString(), returnedLoan.getLoanId(),
                            returnedLoan.getMemberId(), amount, "Overdue loan",
                            FineStatus.OUTSTANDING, returnedLoan.getReturnDate(), bookTitle);
                    repository.save(fine);
                    return fine;
                });
    }

    /** Calculates the current overdue fine without persisting it. */
    public BigDecimal calculateOverdueFine(String loanId) {
        LoanSummary loan = loanQuery.findById(requireText(loanId, "loanId"))
                .orElseThrow(() -> new IllegalArgumentException("Loan does not exist"));
        int overdueDays = loan.getOverdueDaysOn(LocalDate.now(clock));
        return dailyRate.multiply(BigDecimal.valueOf(overdueDays)).setScale(2, RoundingMode.HALF_UP);
    }

    /** Returns all fines for a member. */
    public List<Fine> getMemberFines(String memberId) {
        return repository.findByMemberId(requireText(memberId, "memberId"));
    }

    /** Returns the title of the book associated with a fine's loan. */
    public String getBookTitle(String loanId) {
        return loanQuery.findById(requireText(loanId, "loanId"))
                .map(LoanSummary::getBookName)
                .orElse("Book information unavailable");
    }

    /** Returns the persisted book title for a fine, or resolves it from the associated loan. */
    public String getBookTitle(Fine fine) {
        Objects.requireNonNull(fine, "fine");
        return fine.getBookTitle() == null ? getBookTitle(fine.getLoanId()) : fine.getBookTitle();
    }

    /** Marks a member's outstanding fine as paid and persists the updated fine. */
    public Fine payFine(String fineId, String memberId) {
        Fine fine = getRequired(fineId);
        String requiredMemberId = requireText(memberId, "memberId");
        if (!fine.getMemberId().equals(requiredMemberId)) {
            throw new IllegalArgumentException("Fine does not belong to member");
        }
        if (fine.getStatus() != FineStatus.OUTSTANDING) {
            throw new IllegalStateException("Only outstanding fines can be paid");
        }

        Fine paidFine = fine.pay();
        repository.save(paidFine);
        return paidFine;
    }

    /** Returns all persisted fines for librarian reporting. */
    public List<Fine> getAllFines() {
        return repository.findAll();
    }

    /** Edits the amount of an outstanding fine. */
    public Fine editFine(String fineId, BigDecimal amount) {
        Fine fine = getRequired(fineId);
        if (fine.getStatus() != FineStatus.OUTSTANDING) {
            throw new IllegalStateException("Only outstanding fines can be edited");
        }
        BigDecimal normalizedAmount = Objects.requireNonNull(amount, "amount")
                .setScale(2, RoundingMode.HALF_UP);
        if (normalizedAmount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        Fine updated = fine.withAmount(normalizedAmount);
        repository.save(updated);
        return updated;
    }

    /** Removes a fine by identifier. */
    public boolean removeFine(String fineId) {
        return repository.deleteById(requireText(fineId, "fineId"));
    }

    private Fine getRequired(String fineId) {
        return repository.findById(requireText(fineId, "fineId"))
                .orElseThrow(() -> new IllegalArgumentException("Fine does not exist"));
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
