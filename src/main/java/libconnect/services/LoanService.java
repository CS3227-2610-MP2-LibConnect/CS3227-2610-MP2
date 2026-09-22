package libconnect.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import libconnect.models.Loan;
import libconnect.storage.file.FileLoanRepository;

/** Provides loan operations by coordinating loan entities with a file-backed repository. */
public class LoanService {
    private final FileLoanRepository loanRepository;

    /** Creates a service backed by the default loan data file. */
    public LoanService() {
        this(new FileLoanRepository());
    }

    /**
     * Creates a service backed by the supplied file loan repository.
     *
     * @param loanRepository the repository used to persist loans.
     * @throws NullPointerException if {@code loanRepository} is null.
     */
    public LoanService(FileLoanRepository loanRepository) {
        this.loanRepository = Objects.requireNonNull(loanRepository, "loanRepository");
    }

    /**
     * Returns all loans associated with a user ID.
     *
     * <p>The repository stores this value in the loan's member ID field. The integer user ID is
     * therefore converted to its persisted string representation before querying the repository.</p>
     *
     * @param userId the user ID to search for.
     * @return all loans associated with the user.
     * @throws NotFoundException if no loans are found for the user.
     */
    public List<Loan> getLoansByUserId(int userId) {
        List<Loan> loans = loanRepository.findByMemberId(String.valueOf(userId));
        if (loans.isEmpty()) {
            throw new NotFoundException("No loans found for user: " + userId);
        }

        return loans;
    }

    /**
     * Returns a loan by its identifier.
     *
     * @param loanId the loan ID to search for.
     * @return the matching loan, or an empty optional if it does not exist.
     */
    public Optional<Loan> getLoanById(String loanId) {
        return loanRepository.findById(loanId);
    }

    /**
     * Returns all loans that are overdue as of today or were overdue when they were returned.
     *
     * <p>The overdue check is delegated to {@link Loan#isOverdue()} for each persisted loan.</p>
     *
     * @return all expired loans.
     */
    public List<Loan> getExpiredLoans() {
        return loanRepository.findAll().stream()
                .filter(Loan::isOverdue)
                .toList();
    }

    /**
     * Creates and persists a new loan.
     *
     * @param loanId the ID of the new loan.
     * @param memberId the ID of the borrowing member.
     * @param copyId the ID of the borrowed book copy.
     * @param borrowDate the date on which the book was borrowed.
     * @return true if the loan was created, or false if the ID already exists.
     */
    public void createLoan(String loanId, String memberId, String copyId, LocalDate borrowDate) {
        if (loanRepository.findById(loanId).isPresent()) {
            throw new ServiceException("Loan with ID already exists: " + loanId);
        }

        loanRepository.save(new Loan(loanId, memberId, copyId, borrowDate));
    }

    /**
     * Creates and persists a new loan with a generated unique identifier.
     *
     * @param memberId the ID of the borrowing member.
     * @param copyId the ID of the borrowed book copy.
     * @param borrowDate the date on which the book was borrowed.
     * @return true after the loan has been created.
     * @throws IllegalArgumentException if an identifier is blank.
     * @throws NullPointerException if {@code borrowDate} is null.
     */
    public boolean createLoan(String memberId, String copyId, LocalDate borrowDate) {
        String loanId = generateUniqueLoanId();
        loanRepository.save(new Loan(loanId, memberId, copyId, borrowDate));
        return true;
    }

    /**
     * Returns a loan today and persists the updated loan state in the repository.
     *
     * @param loanId the ID of the loan to return.
     * @throws NotFoundException if the loan does not exist.
     * @throws ServiceException if the loan was already returned.
     */
    public void returnLoan(String loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NotFoundException("Loan not found: " + loanId));
        try {
            loan.returnBook(LocalDate.now());
        } catch (IllegalStateException exception) {
            throw new ServiceException("Loan has already been returned: " + loanId, exception);
        }

        loanRepository.save(loan);
    }

    /**
     * Renews a loan and persists the updated loan state in the repository.
     *
     * @param loanId the ID of the loan to renew.
     * @throws NotFoundException if the loan does not exist.
     * @throws ServiceException if the loan was returned or was already renewed.
     */
    public void renewLoan(String loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NotFoundException("Loan not found: " + loanId));
        try {
            loan.renew();
        } catch (IllegalStateException exception) {
            throw new ServiceException("Loan cannot be renewed: " + loanId, exception);
        }

        loanRepository.save(loan);
    }

    /**
     * Deletes a loan from persistence.
     *
     * @param loanId the ID of the loan to delete.
     * @throws NotFoundException if the loan does not exist.
     */
    public void deleteLoan(String loanId) {
        if (!loanRepository.deleteById(loanId)) {
            throw new NotFoundException("Loan not found: " + loanId);
        }
    }

    private String generateUniqueLoanId() {
        String loanId;
        do {
            loanId = "LOAN-" + UUID.randomUUID();
        } while (loanRepository.findById(loanId).isPresent());

        return loanId;
    }
}
