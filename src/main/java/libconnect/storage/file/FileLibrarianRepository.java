package libconnect.storage.file;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

import libconnect.models.Librarian;
import libconnect.storage.StorageManager;
import libconnect.storage.repositories.LibrarianRepository;
import libconnect.util.ValidationUtils;

/** Stores librarians in a JSON array and reads each operation from disk. */
public final class FileLibrarianRepository extends AbstractFileRepository<Librarian>
        implements LibrarianRepository {

    private static final Path DEFAULT_DATA_FILE = Path.of("data", "librarians.json");

    /** Creates a file-backed librarian repository. */
    public FileLibrarianRepository(StorageManager storageManager, Path file) {
        super(storageManager, file, Librarian.class);
    }

    public FileLibrarianRepository() {
        this(new StorageManager(DEFAULT_DATA_FILE.getParent()), DEFAULT_DATA_FILE);
    }

    /** Finds a librarian by case-insensitive email address. */
    @Override
    public Optional<Librarian> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        return readAll().stream()
                .filter(librarian -> librarian.getEmail().toLowerCase(Locale.ROOT).equals(normalizedEmail))
                .findFirst();
    }

    @Override
    public Optional<Librarian> findByUserId(String userId) {
        String requiredUserId = ValidationUtils.requireNonBlank(userId, "userId");

        return readAll().stream()
                .filter(librarian -> librarian.getUserId().equals(requiredUserId))
                .findFirst();
    }
}
