package libconnect.storage.file.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import libconnect.models.AccountStatus;
import libconnect.models.Librarian;
import libconnect.storage.StorageManager;
import libconnect.storage.file.FileLibrarianRepository;

/** Tests librarian-specific queries in the file-backed repository. */
class FileLibrarianRepositoryTest extends AbstractFileRepositoryTest<Librarian,
        FileLibrarianRepository> {

    @Override
    protected FileLibrarianRepository createRepository(Path dataFile) {
        return new FileLibrarianRepository(new StorageManager(temporaryDirectory), dataFile);
    }

    @Override
    protected void save(FileLibrarianRepository repository, Librarian librarian) {
        repository.save(librarian);
    }

    @Override
    protected List<Librarian> findAll(FileLibrarianRepository repository) {
        return repository.findAll();
    }

    @Override
    protected String getIdentifier(Librarian librarian) {
        return librarian.getEmployeeId();
    }

    @Override
    protected Librarian createEntity(String identifier, String variant) {
        return new Librarian("USER-" + identifier, identifier, "Name " + variant,
                variant + "@example.com", "password-hash", AccountStatus.ACTIVE);
    }

    @Test
    void findByEmail_ignoresCaseAndSurroundingWhitespace() {
        FileLibrarianRepository repository = createRepository(
                temporaryDirectory.resolve("librarians.json"));
        Librarian expected = createEntity("EMP-1", "ada");
        repository.save(expected);

        assertEquals(expected.getId(), repository.findByEmail(" ADA@EXAMPLE.COM ").orElseThrow().getId());
    }

    @Test
    void findByEmail_unknownEmail_returnsEmptyOptional() {
        FileLibrarianRepository repository = createRepository(
                temporaryDirectory.resolve("librarians.json"));

        assertTrue(repository.findByEmail("unknown@example.com").isEmpty());
    }

    @Test
    void findByUserId_existingUserId_returnsMatchingLibrarian() {
        FileLibrarianRepository repository = createRepository(
                temporaryDirectory.resolve("librarians.json"));
        Librarian expected = createEntity("EMP-1", "ada");
        repository.save(expected);

        assertEquals(expected.getId(), repository.findByUserId("USER-EMP-1").orElseThrow().getId());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void findByEmail_blankEmail_throwsIllegalArgumentException(String email) {
        FileLibrarianRepository repository = createRepository(
                temporaryDirectory.resolve("librarians.json"));

        assertThrows(IllegalArgumentException.class, () -> repository.findByEmail(email));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void findByUserId_blankUserId_throwsIllegalArgumentException(String userId) {
        FileLibrarianRepository repository = createRepository(
                temporaryDirectory.resolve("librarians.json"));

        assertThrows(IllegalArgumentException.class, () -> repository.findByUserId(userId));
    }
}
