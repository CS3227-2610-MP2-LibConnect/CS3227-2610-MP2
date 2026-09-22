package libconnect.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import libconnect.models.BookCopy;
import libconnect.models.Loan;
import libconnect.storage.file.FileBookCopyRepository;
import libconnect.storage.file.FileLoanRepository;
import libconnect.storage.repositories.BookCopyRepository;
import libconnect.storage.repositories.LoanRepository;
import libconnect.util.ValidationUtils;

/** Coordinates borrowing several book copies as one compensating transaction. */
public final class BorrowService {
    private static final int MAX_BOOKS_PER_TRANSACTION = 10;
    private static final String TRANSACTION_FAILURE_MESSAGE =
            "Unable to complete the borrowing transaction. No books were borrowed.";
    private static final String ROLLBACK_FAILURE_MESSAGE =
            "Unable to complete the borrowing transaction and restore its changes.";

    private final BookCopyRepository copyRepository;
    private final LoanRepository loanRepository;

    /** Creates a borrowing service backed by the default data files. */
    public BorrowService() {
        this(new FileBookCopyRepository(), new FileLoanRepository());
    }

    /**
     * Creates a borrowing service with explicit repository dependencies.
     *
     * @param copyRepository the repository used to persist book-copy statuses.
     * @param loanRepository the repository used to persist loans.
     * @throws NullPointerException if either repository is null.
     */
    public BorrowService(BookCopyRepository copyRepository, LoanRepository loanRepository) {
        this.copyRepository = Objects.requireNonNull(copyRepository, "copyRepository");
        this.loanRepository = Objects.requireNonNull(loanRepository, "loanRepository");
    }

    /**
     * Borrows all supplied copies for one member.
     *
     * <p>The operation validates every copy before writing. If a persistence operation fails
     * after writing has started, created loans are deleted and copy statuses are restored.</p>
     *
     * @param memberId the membership identifier of the borrowing member.
     * @param copyIds the identifiers of the copies to borrow.
     * @param borrowDate the date on which the transaction takes place.
     * @throws IllegalArgumentException if the member ID or copy list is invalid.
     * @throws NotFoundException if a supplied copy does not exist.
     * @throws ServiceException if a copy is unavailable or the transaction cannot be completed.
     * @throws NullPointerException if {@code borrowDate} is null.
     */
    public void borrowCopies(String memberId, List<String> copyIds, LocalDate borrowDate) {
        String requiredMemberId = ValidationUtils.requireNonBlank(memberId, "memberId");
        Objects.requireNonNull(copyIds, "copyIds cannot be null");
        Objects.requireNonNull(borrowDate, "borrowDate cannot be null");
        validateCopyIds(copyIds);

        List<BookCopy> originalCopies = loadAvailableCopies(copyIds);
        List<BookCopy> updatedCopies = new ArrayList<>();
        List<String> createdLoanIds = new ArrayList<>();

        try {
            for (BookCopy copy : originalCopies) {
                BookCopy originalCopy = copySnapshot(copy);
                copy.markBorrowed();
                updatedCopies.add(originalCopy);
                copyRepository.save(copy);
            }

            for (BookCopy copy : originalCopies) {
                String loanId = generateUniqueLoanId();
                createdLoanIds.add(loanId);
                loanRepository.save(new Loan(loanId, requiredMemberId, copy.getCopyId(), borrowDate));
            }
        } catch (RuntimeException exception) {
            try {
                rollback(createdLoanIds, updatedCopies);
            } catch (RuntimeException rollbackException) {
                throw new ServiceException(ROLLBACK_FAILURE_MESSAGE, rollbackException);
            }
            throw new ServiceException(TRANSACTION_FAILURE_MESSAGE, exception);
        }
    }

    private void validateCopyIds(List<String> copyIds) {
        if (copyIds.isEmpty()) {
            throw new IllegalArgumentException("At least one book copy is required");
        }
        if (copyIds.size() > MAX_BOOKS_PER_TRANSACTION) {
            throw new IllegalArgumentException("A transaction cannot contain more than 10 books");
        }

        Set<String> uniqueCopyIds = new HashSet<>();
        for (String copyId : copyIds) {
            String requiredCopyId = ValidationUtils.requireNonBlank(copyId, "copyId");
            if (!uniqueCopyIds.add(requiredCopyId)) {
                throw new IllegalArgumentException("A book copy cannot be added more than once: "
                        + requiredCopyId);
            }
        }
    }

    private List<BookCopy> loadAvailableCopies(List<String> copyIds) {
        List<BookCopy> copies = new ArrayList<>();
        for (String copyId : copyIds) {
            BookCopy copy = copyRepository.findById(copyId)
                    .orElseThrow(() -> new NotFoundException("Book copy not found: " + copyId));
            if (!copy.isAvailable()) {
                throw new ServiceException("Book copy is not available for borrowing: " + copyId);
            }
            copies.add(copy);
        }
        return copies;
    }

    private BookCopy copySnapshot(BookCopy copy) {
        return new BookCopy(copy.getCopyId(), copy.getIsbn(), copy.getStatus(),
                copy.getShelfLocation());
    }

    private void rollback(List<String> createdLoanIds, List<BookCopy> originalCopies) {
        RuntimeException firstFailure = null;
        for (String loanId : createdLoanIds) {
            try {
                if (!loanRepository.deleteById(loanId)) {
                    throw new ServiceException("Loan was not found during rollback: " + loanId);
                }
            } catch (RuntimeException exception) {
                if (firstFailure == null) {
                    firstFailure = exception;
                }
            }
        }

        for (BookCopy originalCopy : originalCopies) {
            try {
                copyRepository.save(originalCopy);
            } catch (RuntimeException exception) {
                if (firstFailure == null) {
                    firstFailure = exception;
                }
            }
        }

        if (firstFailure != null) {
            throw firstFailure;
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
