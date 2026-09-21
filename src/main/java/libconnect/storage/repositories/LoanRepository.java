package libconnect.storage.repositories;

import java.util.List;
import java.util.Optional;

import libconnect.models.Loan;
import libconnect.models.LoanStatus;
import libconnect.storage.exceptions.DeleteFailureException;

/**
 * Defines persistence operations for loans.
 */
public interface LoanRepository {
    /**
     * Finds a loan by its stable identifier.
     *
     * @param loanId the loan identifier to find.
     * @return the matching loan, or an empty optional if no loan exists.
     */
    Optional<Loan> findById(String loanId);

    /**
     * Returns every persisted loan.
     *
     * @return all persisted loans.
     */
    List<Loan> findAll();

    /**
     * Finds all loans belonging to a member.
     *
     * @param memberId the member identifier to search for.
     * @return all loans associated with the member.
     */
    List<Loan> findByMemberId(String memberId);

    /**
     * Finds all loans associated with a physical book copy.
     *
     * @param copyId the book-copy identifier to search for.
     * @return all loans associated with the book copy.
     */
    List<Loan> findByCopyId(String copyId);

    /**
     * Finds all loans with the supplied status.
     *
     * @param status the loan status to search for.
     * @return all loans with the supplied status.
     */
    List<Loan> findByStatus(LoanStatus status);

    /**
     * Inserts a loan or replaces the existing loan with the same stable identifier.
     *
     * @param loan the loan to persist.
     */
    void save(Loan loan);

    /**
     * Deletes a loan by its stable identifier.
     *
     * @param loanId the loan identifier to delete.
     * @throws DeleteFailureException if no loan with the supplied identifier can be deleted.
     */
    void deleteById(String loanId) throws DeleteFailureException;
}
