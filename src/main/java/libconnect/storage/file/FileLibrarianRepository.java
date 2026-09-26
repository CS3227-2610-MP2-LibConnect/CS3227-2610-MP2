package libconnect.storage.file;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

import libconnect.models.Librarian;
import libconnect.storage.StorageManager;
import libconnect.storage.repositories.LibrarianRepository;

/** Stores librarians in a JSON array and reads each operation from disk. */
public final class FileLibrarianRepository extends AbstractFileRepository<Librarian>
        implements LibrarianRepository {
    /** Creates a file-backed librarian repository. */
    public FileLibrarianRepository(StorageManager storageManager, Path file) {
        super(storageManager, file, Librarian.class);
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
}
