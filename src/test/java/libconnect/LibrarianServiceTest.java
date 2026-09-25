package libconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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

    @Test
    void register_duplicateEmployeeId_rejected() {
        ServiceTestDoubles.Librarians repository = new ServiceTestDoubles.Librarians();
        LibrarianService service = new LibrarianService(repository);
        service.register(new Librarian("e1", "Ada", "ada@example.com", AccountStatus.ACTIVE));

        assertThrows(IllegalStateException.class, () -> service.register(
                new Librarian("e1", "Grace", "grace@example.com", AccountStatus.ACTIVE)));
    }

    @Test
    void register_nullLibrarian_rejected() {
        assertThrows(NullPointerException.class,
                () -> new LibrarianService(new ServiceTestDoubles.Librarians()).register(null));
    }

    @Test
    void edit_existingLibrarian_updatesDetailsAndPreservesStatus() {
        ServiceTestDoubles.Librarians repository = new ServiceTestDoubles.Librarians();
        repository.save(new Librarian("e1", "Ada", "ada@example.com", AccountStatus.INACTIVE));
        LibrarianService service = new LibrarianService(repository);

        Librarian edited = service.edit("e1", "Grace", "grace@example.com");

        assertEquals("Grace", edited.getName());
        assertEquals("grace@example.com", edited.getEmail());
        assertEquals(AccountStatus.INACTIVE, edited.getStatus());
    }

    @Test
    void edit_duplicateEmailOwnedByAnotherLibrarian_rejected() {
        ServiceTestDoubles.Librarians repository = new ServiceTestDoubles.Librarians();
        repository.save(new Librarian("e1", "Ada", "ada@example.com", AccountStatus.ACTIVE));
        repository.save(new Librarian("e2", "Grace", "grace@example.com", AccountStatus.ACTIVE));
        LibrarianService service = new LibrarianService(repository);

        assertThrows(IllegalStateException.class,
                () -> service.edit("e1", "Ada", "grace@example.com"));
    }

    @Test
    void deactivate_activeLibrarian_persistsInactiveStatus() {
        ServiceTestDoubles.Librarians repository = new ServiceTestDoubles.Librarians();
        repository.save(new Librarian("e1", "Ada", "ada@example.com", AccountStatus.ACTIVE));
        LibrarianService service = new LibrarianService(repository);

        Librarian deactivated = service.deactivate("e1");

        assertEquals(AccountStatus.INACTIVE, deactivated.getStatus());
        assertEquals(AccountStatus.INACTIVE, repository.findById("e1").orElseThrow().getStatus());
    }

    /** Verifies that inactive librarians cannot perform protected operations. */
    @Test
    void requireActive_inactiveLibrarian_rejected() {
        ServiceTestDoubles.Librarians repository = new ServiceTestDoubles.Librarians();
        repository.save(new Librarian("e1", "Ada", "ada@example.com", AccountStatus.INACTIVE));
        LibrarianService service = new LibrarianService(repository);

        assertThrows(IllegalStateException.class, () -> service.requireActive("e1"));
    }

    @Test
    void requireActive_missingLibrarian_rejected() {
        LibrarianService service = new LibrarianService(new ServiceTestDoubles.Librarians());

        assertThrows(IllegalArgumentException.class, () -> service.requireActive("missing"));
    }

    /** Verifies that librarian search is case-insensitive and deterministic. */
    @Test
    void search_matchesName_caseInsensitive() {
        ServiceTestDoubles.Librarians repository = new ServiceTestDoubles.Librarians();
        repository.save(new Librarian("e1", "Ada Lovelace", "ada@example.com", AccountStatus.ACTIVE));
        LibrarianService service = new LibrarianService(repository);

        assertEquals(1, service.search("lovelace").size());
    }

    @Test
    void search_matchesIdAndEmail_andBlankQueryReturnsSortedResults() {
        ServiceTestDoubles.Librarians repository = new ServiceTestDoubles.Librarians();
        repository.save(new Librarian("e2", "Grace", "grace@example.com", AccountStatus.ACTIVE));
        repository.save(new Librarian("e1", "Ada", "ada@example.com", AccountStatus.ACTIVE));
        LibrarianService service = new LibrarianService(repository);

        assertEquals("e1", service.search("E1").get(0).getId());
        assertEquals("e2", service.search("GRACE@EXAMPLE.COM").get(0).getId());
        assertEquals(List.of("e1", "e2"), service.search("  ").stream()
                .map(Librarian::getId).toList());
        assertTrue(service.search(null).size() == 2);
    }
}
