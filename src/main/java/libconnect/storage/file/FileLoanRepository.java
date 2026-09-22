package libconnect.storage.file;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import libconnect.models.Loan;
import libconnect.models.LoanStatus;
import libconnect.storage.StorageManager;
import libconnect.storage.repositories.LoanRepository;
import libconnect.util.ValidationUtils;

/**
 * Provides JSON file-backed persistence for loans.
 */
public class FileLoanRepository extends AbstractFileRepository<Loan> implements LoanRepository {
    private static final Path DEFAULT_DATA_FILE = Path.of("data", "loans.json");

    /**
     * Creates a repository backed by {@code data/loans.json}.
     *
     * @throws IllegalStateException if the data file cannot be created.
     */
    public FileLoanRepository() {
        this(new StorageManager(DEFAULT_DATA_FILE.getParent()), DEFAULT_DATA_FILE);
    }

    /**
     * Creates a repository backed by the supplied file.
     *
     * @param dataFile the JSON file used for persistence.
     * @throws NullPointerException if {@code dataFile} is null.
     * @throws IllegalStateException if the data file cannot be created.
     */
    public FileLoanRepository(Path dataFile) {
        this(new StorageManager(dataFile.getParent()), dataFile);
    }

    /**
     * Creates a repository with explicit storage dependencies.
     *
     * @param storageManager the manager used for storage operations.
     * @param dataFile the JSON file used for persistence.
     * @throws NullPointerException if either argument is null.
     * @throws IllegalStateException if the data file cannot be created.
     */
    public FileLoanRepository(StorageManager storageManager, Path dataFile) {
        super(storageManager, dataFile, Loan.class);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code memberId} is blank.
     * @throws IllegalStateException if the data file cannot be read.
     */
    @Override
    public List<Loan> findByMemberId(String memberId) {
        String requiredMemberId = ValidationUtils.requireNonBlank(memberId, "member_id");

        return findAll().stream()
                .filter(loan -> loan.getMemberId().equals(requiredMemberId))
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code copyId} is blank.
     * @throws IllegalStateException if the data file cannot be read.
     */
    @Override
    public List<Loan> findByCopyId(String copyId) {
        String requiredCopyId = ValidationUtils.requireNonBlank(copyId, "copy_id");

        return findAll().stream()
                .filter(loan -> loan.getCopyId().equals(requiredCopyId))
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code status} is null.
     * @throws IllegalStateException if the data file cannot be read.
     */
    @Override
    public List<Loan> findByStatus(LoanStatus status) {
        Objects.requireNonNull(status, "status cannot be null");

        return findAll().stream()
                .filter(loan -> loan.getStatus() == status)
                .toList();
    }
}
