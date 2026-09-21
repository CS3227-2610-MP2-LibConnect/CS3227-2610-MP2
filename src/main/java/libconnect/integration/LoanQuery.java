package libconnect.integration;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Provides read-only loan data required by librarian operations. */
public interface LoanQuery {
    /** Finds a loan by its stable identifier. */
    Optional<LoanSummary> findById(String loanId);

    /** Returns all active loans visible to a librarian. */
    List<LoanSummary> findActiveLoans();

    /** Returns active loans overdue on the supplied date. */
    List<LoanSummary> findOverdueLoans(LocalDate date);
}
