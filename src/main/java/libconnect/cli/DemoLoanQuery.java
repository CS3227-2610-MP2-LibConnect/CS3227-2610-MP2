package libconnect.cli;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import libconnect.integration.LoanQuery;
import libconnect.integration.LoanSummary;

/** Provides temporary in-memory loan data for overdue and fine manual tests. */
public final class DemoLoanQuery implements LoanQuery {
    private final Map<String, LoanSummary> loans = new LinkedHashMap<>();

    /** Creates one overdue demo loan using the supplied clock. */
    /** Creates temporary loan data using the supplied clock. */
    public DemoLoanQuery(Clock clock) {
        loans.put("loan-1", new LoanSummary("loan-1", "m1", "copy-1",
                LocalDate.now(clock).minusDays(3)));
    }

    /** Finds a demo loan by stable identifier. */
    @Override
    public Optional<LoanSummary> findById(String loanId) {
        return Optional.ofNullable(loans.get(loanId));
    }

    /** Returns all demo loans. */
    @Override
    public List<LoanSummary> findActiveLoans() {
        return List.copyOf(loans.values());
    }

    /** Returns demo loans overdue on the supplied date. */
    @Override
    public List<LoanSummary> findOverdueLoans(LocalDate date) {
        return loans.values().stream().filter(loan -> loan.isOverdueOn(date)).toList();
    }
}
