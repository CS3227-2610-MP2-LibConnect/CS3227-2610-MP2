package libconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import libconnect.models.AccountStatus;
import libconnect.models.Librarian;
import libconnect.models.Member;
import libconnect.services.LibrarianService;
import libconnect.storage.StorageManager;
import libconnect.storage.file.FileMemberRepository;

/** Tests librarian registration, editing, and authorization rules. */
class LibrarianServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void search_unknownUserId_returnsNoMatches() {
        ServiceTestDoubles.Librarians repository = new ServiceTestDoubles.Librarians();
        LibrarianService service = new LibrarianService(repository);

        assertEquals(0, service.search("USER-1").size());
    }

    /** Verifies that duplicate email addresses are rejected. */
    @Test
    void register_duplicateEmail_rejected() {
        ServiceTestDoubles.Librarians repository = new ServiceTestDoubles.Librarians();
        LibrarianService service = new LibrarianService(repository);
        service.register(new Librarian("u1", "e1", "Ada", "ada@example.com", "password-hash",
                AccountStatus.ACTIVE));

        assertThrows(IllegalStateException.class, () -> service.register(
                new Librarian("u2", "e2", "Grace", "ada@example.com", "password-hash",
                        AccountStatus.ACTIVE)));
    }

    @Test
    void register_duplicateEmailIgnoringCase_rejected() {
        ServiceTestDoubles.Librarians repository = new ServiceTestDoubles.Librarians();
        LibrarianService service = new LibrarianService(repository);
        service.register(new Librarian("u1", "e1", "Ada", "ada@example.com", "password-hash",
                AccountStatus.ACTIVE));

        assertThrows(IllegalStateException.class, () -> service.register(
                new Librarian("u2", "e2", "Grace", " ADA@EXAMPLE.COM ", "password-hash",
                        AccountStatus.ACTIVE)));
    }

    @Test
    void register_duplicateEmployeeId_rejected() {
        ServiceTestDoubles.Librarians repository = new ServiceTestDoubles.Librarians();
        LibrarianService service = new LibrarianService(repository);
        service.register(new Librarian("u1", "e1", "Ada", "ada@example.com", "password-hash",
                AccountStatus.ACTIVE));

        assertThrows(IllegalStateException.class, () -> service.register(
                new Librarian("u2", "e1", "Grace", "grace@example.com", "password-hash",
                        AccountStatus.ACTIVE)));
    }

    @Test
    void register_emailUsedByMember_rejected() {
        ServiceTestDoubles.Librarians librarians = new ServiceTestDoubles.Librarians();
        FileMemberRepository members = new FileMemberRepository(new StorageManager(temporaryDirectory),
                temporaryDirectory.resolve("members.json"));
        members.save(new Member("u1", "Ada", "ada@example.com", "password-hash", "m1"));
        LibrarianService service = new LibrarianService(librarians, members);

        assertThrows(IllegalStateException.class, () -> service.register(
                new Librarian("u2", "e2", "Grace", "ada@example.com", "password-hash",
                        AccountStatus.ACTIVE)));
    }

    @Test
    void register_emailUsedByMemberIgnoringCase_rejected() {
        ServiceTestDoubles.Librarians librarians = new ServiceTestDoubles.Librarians();
        FileMemberRepository members = new FileMemberRepository(new StorageManager(temporaryDirectory),
                temporaryDirectory.resolve("members.json"));
        members.save(new Member("u1", "Ada", "ada@example.com", "password-hash", "m1"));
        LibrarianService service = new LibrarianService(librarians, members);

        assertThrows(IllegalStateException.class, () -> service.register(
                new Librarian("u2", "e2", "Grace", " ADA@EXAMPLE.COM ", "password-hash",
                        AccountStatus.ACTIVE)));
    }

    @Test
    void register_stringOverload_createsActiveHashedLibrarian() {
        ServiceTestDoubles.Librarians repository = new ServiceTestDoubles.Librarians();
        LibrarianService service = new LibrarianService(repository);

        service.register("e1", "Ada", "ada@example.com", "secret-password");

        Librarian librarian = repository.findById("e1").orElseThrow();
        assertEquals(AccountStatus.ACTIVE, librarian.getStatus());
        assertTrue(librarian.getPasswordHash().startsWith("PBKDF2WithHmacSHA256$210000$"));
    }

    /** Verifies that inactive librarians cannot perform protected operations. */
    @Test
    void requireActive_inactiveLibrarian_rejected() {
        ServiceTestDoubles.Librarians repository = new ServiceTestDoubles.Librarians();
        repository.save(new Librarian("u1", "e1", "Ada", "ada@example.com", "password-hash",
                AccountStatus.INACTIVE));
        LibrarianService service = new LibrarianService(repository);

        assertThrows(IllegalStateException.class, () -> service.requireActive("e1"));
    }

    @Test
    void requireActive_activeLibrarian_returnsLibrarian() {
        ServiceTestDoubles.Librarians repository = new ServiceTestDoubles.Librarians();
        Librarian librarian = new Librarian("u1", "e1", "Ada", "ada@example.com", "password-hash",
                AccountStatus.ACTIVE);
        repository.save(librarian);
        LibrarianService service = new LibrarianService(repository);

        assertEquals(librarian, service.requireActive("e1"));
    }

    @Test
    void edit_preservesStatusAndPasswordAndUpdatesProfile() {
        ServiceTestDoubles.Librarians repository = new ServiceTestDoubles.Librarians();
        repository.save(new Librarian("u1", "e1", "Ada", "ada@example.com", "password-hash",
                AccountStatus.INACTIVE));
        LibrarianService service = new LibrarianService(repository);

        Librarian edited = service.edit("e1", "Ada Lovelace", "ada.lovelace@example.com");

        assertEquals("Ada Lovelace", edited.getName());
        assertEquals("ada.lovelace@example.com", edited.getEmail());
        assertEquals("password-hash", edited.getPasswordHash());
        assertEquals(AccountStatus.INACTIVE, edited.getStatus());
    }

    @Test
    void edit_duplicateEmail_rejected() {
        ServiceTestDoubles.Librarians repository = new ServiceTestDoubles.Librarians();
        repository.save(new Librarian("u1", "e1", "Ada", "ada@example.com", "password-hash",
                AccountStatus.ACTIVE));
        repository.save(new Librarian("u2", "e2", "Grace", "grace@example.com", "password-hash",
                AccountStatus.ACTIVE));
        LibrarianService service = new LibrarianService(repository);

        assertThrows(IllegalStateException.class,
                () -> service.edit("e1", "Ada", "grace@example.com"));
    }

    @Test
    void deactivate_activeLibrarian_persistsInactiveStatus() {
        ServiceTestDoubles.Librarians repository = new ServiceTestDoubles.Librarians();
        repository.save(new Librarian("u1", "e1", "Ada", "ada@example.com", "password-hash",
                AccountStatus.ACTIVE));
        LibrarianService service = new LibrarianService(repository);

        Librarian deactivated = service.deactivate("e1");

        assertEquals(AccountStatus.INACTIVE, deactivated.getStatus());
        assertThrows(IllegalStateException.class, () -> service.requireActive("e1"));
    }

    /** Verifies that librarian search is case-insensitive and deterministic. */
    @Test
    void search_matchesName_caseInsensitive() {
        ServiceTestDoubles.Librarians repository = new ServiceTestDoubles.Librarians();
        repository.save(new Librarian("u1", "e1", "Ada Lovelace", "ada@example.com", "password-hash",
                AccountStatus.ACTIVE));
        LibrarianService service = new LibrarianService(repository);

        assertEquals(1, service.search("lovelace").size());
    }

    @Test
    void search_matchesEmployeeIdEmailAndBlankQuery_returnsSortedResults() {
        ServiceTestDoubles.Librarians repository = new ServiceTestDoubles.Librarians();
        repository.save(new Librarian("u2", "e2", "Grace", "grace@example.com", "password-hash",
                AccountStatus.ACTIVE));
        repository.save(new Librarian("u1", "e1", "Ada", "ada@example.com", "password-hash",
                AccountStatus.ACTIVE));
        LibrarianService service = new LibrarianService(repository);

        assertEquals(1, service.search(" E2 ").size());
        assertEquals(1, service.search("GRACE@EXAMPLE.COM").size());
        assertEquals(2, service.search(null).size());
        assertEquals("e1", service.search("").get(0).getEmployeeId());
    }

    @Test
    void librarianOperations_unknownOrBlankId_rejected() {
        LibrarianService service = new LibrarianService(new ServiceTestDoubles.Librarians());

        assertThrows(IllegalArgumentException.class, () -> service.requireActive("unknown"));
        assertThrows(IllegalArgumentException.class, () -> service.edit(" ", "Ada", "ada@example.com"));
        assertThrows(IllegalArgumentException.class, () -> service.deactivate(" "));
    }
}
