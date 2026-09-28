package libconnect.integration;

import libconnect.models.Fine;

/** Provides fine creation for returned loans without exposing fine-service internals. */
public interface FineIssuer {
    /**
     * Creates or returns the outstanding fine for an overdue returned loan.
     *
     * @param loan the returned loan for which a fine should be issued.
     * @return the newly created or existing outstanding fine.
     */
    Fine issueForReturnedLoan(ReturnedLoanSummary loan);
}
