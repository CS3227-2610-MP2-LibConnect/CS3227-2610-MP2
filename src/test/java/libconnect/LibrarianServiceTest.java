package libconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import libconnect.models.AccountStatus;
import libconnect.models.Librarian;
import libconnect.services.LibrarianService;

/** Tests librarian registration, editing, and authorization rules. */
class LibrarianServiceTest {
    /** Verifies that duplicate email addresses are rejected. */
    @Test
    void register_duplicateEmail_rejected() {
        ServiceTestDoubles.Librarians repository = new ServiceTestDoubles.Librarians();
        LibrarianService service = new LibrarianService(repository);
        service.register(new Librarian("e1", "Ada", "ada@example.com", AccountStatus.ACTIVE));

        assertThrows(IllegalStateException.class, () -> service.register(
                new Librarian("e2", "Grace", "ada@example.com", AccountStatus.ACTIVE)));
    }

    /** Verifies that inactive librarians cannot perform protected operations. */
    @Test
    void requireActive_inactiveLibrarian_rejected() {
        ServiceTestDoubles.Librarians repository = new ServiceTestDoubles.Librarians();
        repository.save(new Librarian("e1", "Ada", "ada@example.com", AccountStatus.INACTIVE));
        LibrarianService service = new LibrarianService(repository);

        assertThrows(IllegalStateException.class, () -> service.requireActive("e1"));
    }

    /** Verifies that librarian search is case-insensitive and deterministic. */
    @Test
    void search_matchesName_caseInsensitive() {
        ServiceTestDoubles.Librarians repository = new ServiceTestDoubles.Librarians();
        repository.save(new Librarian("e1", "Ada Lovelace", "ada@example.com", AccountStatus.ACTIVE));
        LibrarianService service = new LibrarianService(repository);

        assertEquals(1, service.search("lovelace").size());
    }
}
