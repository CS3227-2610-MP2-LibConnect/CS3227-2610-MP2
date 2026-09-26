package libconnect.storage.repositories;

import java.util.Optional;

import libconnect.models.Librarian;

/** Defines librarian persistence and email lookup operations. */
public interface LibrarianRepository extends Repository<Librarian> {
    /** Finds a librarian by normalized email address. */
    Optional<Librarian> findByEmail(String email);

    /** Finds a librarian by user ID. */
    Optional<Librarian> findByUserId(String userId);
}
