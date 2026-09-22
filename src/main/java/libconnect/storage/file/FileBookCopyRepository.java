package libconnect.storage.file;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import libconnect.models.BookCopy;
import libconnect.models.CopyStatus;
import libconnect.storage.StorageManager;
import libconnect.storage.repositories.BookCopyRepository;
import libconnect.util.ValidationUtils;

/**
 * Provides JSON file-backed persistence for physical book copies.
 */
public class FileBookCopyRepository extends AbstractFileRepository<BookCopy> implements BookCopyRepository {
    private static final Path DEFAULT_DATA_FILE = Path.of("data", "book-copies.json");

    /**
     * Creates a repository backed by {@code data/book-copies.json}.
     *
     * @throws IllegalStateException if the data file cannot be created.
     */
    public FileBookCopyRepository() {
        this(new StorageManager(DEFAULT_DATA_FILE.getParent()), DEFAULT_DATA_FILE);
    }

    /**
     * Creates a repository backed by the supplied file.
     *
     * @param dataFile the JSON file used for persistence.
     * @throws NullPointerException if {@code dataFile} is null.
     * @throws IllegalStateException if the data file cannot be created.
     */
    public FileBookCopyRepository(Path dataFile) {
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
    public FileBookCopyRepository(StorageManager storageManager, Path dataFile) {
        super(storageManager, dataFile, BookCopy.class);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code isbn} is blank.
     * @throws IllegalStateException if the data file cannot be read.
     */
    @Override
    public List<BookCopy> findByIsbn(String isbn) {
        String requiredIsbn = ValidationUtils.requireNonBlank(isbn, "isbn");

        return readAll().stream()
                .filter(copy -> copy.getIsbn().equals(requiredIsbn))
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code isbn} is blank.
     * @throws IllegalStateException if the data file cannot be read or written.
     */
    @Override
    public void deleteByIsbn(String isbn) {
        String requiredIsbn = ValidationUtils.requireNonBlank(isbn, "isbn");
        List<BookCopy> copies = readAll().stream()
                .filter(copy -> copy.getIsbn().equals(requiredIsbn)).toList();
        for (BookCopy copy : copies) {
            deleteById(copy.getId());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code status} is null.
     * @throws IllegalStateException if the data file cannot be read.
     */
    @Override
    public List<BookCopy> findByStatus(CopyStatus status) {
        Objects.requireNonNull(status, "status cannot be null");

        return readAll().stream().filter(copy -> copy.getStatus() == status).toList();
    }
}
