package libconnect.storage.file.unit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import libconnect.models.BookCopy;
import libconnect.models.CopyStatus;
import libconnect.storage.file.FileBookCopyRepository;

/**
 * Tests unit-level JSON persistence for physical book copies.
 */
class FileBookCopyRepositoryTest extends AbstractFileRepositoryTest<BookCopy,
        FileBookCopyRepository> {

    @Override
    protected FileBookCopyRepository createRepository(Path dataFile) {
        return new FileBookCopyRepository(dataFile);
    }

    @Override
    protected void save(FileBookCopyRepository repository, BookCopy bookCopy) {
        repository.save(bookCopy);
    }

    @Override
    protected List<BookCopy> findAll(FileBookCopyRepository repository) {
        return repository.findAll();
    }

    @Override
    protected String getIdentifier(BookCopy bookCopy) {
        return bookCopy.getCopyId();
    }

    @Override
    protected BookCopy createEntity(String identifier, String variant) {
        return new BookCopy(identifier, "ISBN-" + variant, "SHELF-" + variant);
    }

    @Test
    void constructor_nullDataFile_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new FileBookCopyRepository(null));
    }

    @Test
    void save_preservesStatusAndShelfLocationAfterReload() {
        Path dataFile = temporaryDirectory.resolve("book-copies.json");
        FileBookCopyRepository repository = createRepository(dataFile);
        BookCopy bookCopy = new BookCopy("COPY-1", "ISBN-1", CopyStatus.BORROWED, "A\"1\\02");

        repository.save(bookCopy);

        BookCopy savedCopy = repository.findById("COPY-1").orElseThrow();
        assertAll(
                () -> assertEquals("ISBN-1", savedCopy.getIsbn()),
                () -> assertEquals(CopyStatus.BORROWED, savedCopy.getStatus()),
                () -> assertEquals("A\"1\\02", savedCopy.getShelfLocation()));
    }

    @Test
    void findById_existingId_returnsMatchingCopy() {
        FileBookCopyRepository repository = createRepository(
                temporaryDirectory.resolve("book-copies.json"));
        BookCopy expectedCopy = new BookCopy("COPY-1", "ISBN-1", "A1-01");
        repository.save(expectedCopy);

        Optional<BookCopy> result = repository.findById(" COPY-1 ");

        assertEquals(Optional.of(expectedCopy), result);
    }

    @Test
    void findById_unknownId_returnsEmptyOptional() {
        FileBookCopyRepository repository = createRepository(
                temporaryDirectory.resolve("book-copies.json"));

        assertTrue(repository.findById("COPY-1").isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void findById_blankId_throwsIllegalArgumentException(String copyId) {
        FileBookCopyRepository repository = createRepository(
                temporaryDirectory.resolve("book-copies.json"));

        assertThrows(IllegalArgumentException.class, () -> repository.findById(copyId));
    }

    @Test
    void findByIsbn_matchingIsbn_returnsAllMatchingCopies() {
        FileBookCopyRepository repository = createRepository(
                temporaryDirectory.resolve("book-copies.json"));
        repository.save(new BookCopy("COPY-1", "ISBN-1", "A1-01"));
        repository.save(new BookCopy("COPY-2", "ISBN-1", "A1-02"));
        repository.save(new BookCopy("COPY-3", "ISBN-2", "B1-01"));

        List<BookCopy> matchingCopies = repository.findByIsbn(" ISBN-1 ");

        assertEquals(List.of(
                new BookCopy("COPY-1", "ISBN-1", "A1-01"),
                new BookCopy("COPY-2", "ISBN-1", "A1-02")), matchingCopies);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void findByIsbn_blankIsbn_throwsIllegalArgumentException(String isbn) {
        FileBookCopyRepository repository = createRepository(
                temporaryDirectory.resolve("book-copies.json"));

        assertThrows(IllegalArgumentException.class, () -> repository.findByIsbn(isbn));
    }

    @ParameterizedTest
    @EnumSource(CopyStatus.class)
    void findByStatus_status_returnsCopiesWithThatStatus(CopyStatus status) {
        FileBookCopyRepository repository = createRepository(
                temporaryDirectory.resolve("book-copies.json"));
        repository.save(new BookCopy("COPY-1", "ISBN-1", status, "A1-01"));
        repository.save(new BookCopy("COPY-2", "ISBN-2", otherStatus(status), "A1-02"));

        List<BookCopy> matchingCopies = repository.findByStatus(status);

        assertEquals(List.of(new BookCopy("COPY-1", "ISBN-1", status, "A1-01")), matchingCopies);
    }

    @Test
    void findByStatus_nullStatus_throwsNullPointerException() {
        FileBookCopyRepository repository = createRepository(
                temporaryDirectory.resolve("book-copies.json"));

        assertThrows(NullPointerException.class, () -> repository.findByStatus(null));
    }

    @Test
    void save_nullCopy_throwsNullPointerException() {
        FileBookCopyRepository repository = createRepository(
                temporaryDirectory.resolve("book-copies.json"));

        assertThrows(NullPointerException.class, () -> repository.save(null));
    }

    @Test
    void deleteById_existingId_removesOnlyMatchingCopy() {
        FileBookCopyRepository repository = createRepository(
                temporaryDirectory.resolve("book-copies.json"));
        repository.save(new BookCopy("COPY-1", "ISBN-1", "A1-01"));
        repository.save(new BookCopy("COPY-2", "ISBN-2", "A1-02"));

        repository.deleteById(" COPY-1 ");

        assertEquals(List.of(new BookCopy("COPY-2", "ISBN-2", "A1-02")), repository.findAll());
    }

    @Test
    void deleteById_unknownId_returnsFalseAndPreservesData() {
        FileBookCopyRepository repository = createRepository(
                temporaryDirectory.resolve("book-copies.json"));
        BookCopy existingCopy = new BookCopy("COPY-1", "ISBN-1", "A1-01");
        repository.save(existingCopy);

        assertFalse(repository.deleteById("COPY-2"));
        assertEquals(List.of(existingCopy), repository.findAll());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void deleteById_blankId_throwsIllegalArgumentException(String copyId) {
        FileBookCopyRepository repository = createRepository(
                temporaryDirectory.resolve("book-copies.json"));

        assertThrows(IllegalArgumentException.class, () -> repository.deleteById(copyId));
    }

    @Test
    void deleteByIsbn_matchingIsbn_removesAllMatchingCopies() {
        FileBookCopyRepository repository = createRepository(
                temporaryDirectory.resolve("book-copies.json"));
        repository.save(new BookCopy("COPY-1", "ISBN-1", "A1-01"));
        repository.save(new BookCopy("COPY-2", "ISBN-1", "A1-02"));
        repository.save(new BookCopy("COPY-3", "ISBN-2", "B1-01"));

        repository.deleteByIsbn(" ISBN-1 ");

        assertEquals(List.of(new BookCopy("COPY-3", "ISBN-2", "B1-01")), repository.findAll());
    }

    @Test
    void deleteByIsbn_unknownIsbn_completesWithoutChangingData() {
        FileBookCopyRepository repository = createRepository(
                temporaryDirectory.resolve("book-copies.json"));
        BookCopy existingCopy = new BookCopy("COPY-1", "ISBN-1", "A1-01");
        repository.save(existingCopy);

        repository.deleteByIsbn("ISBN-2");

        assertEquals(List.of(existingCopy), repository.findAll());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void deleteByIsbn_blankIsbn_throwsIllegalArgumentException(String isbn) {
        FileBookCopyRepository repository = createRepository(
                temporaryDirectory.resolve("book-copies.json"));

        assertThrows(IllegalArgumentException.class, () -> repository.deleteByIsbn(isbn));
    }

    @Test
    void findAll_recordMissingRequiredField_logsAndSkipsMalformedRecord() throws IOException {
        Path dataFile = temporaryDirectory.resolve("book-copies.json");
        FileBookCopyRepository repository = createRepository(dataFile);
        String copies = "[{\"copyId\":\"COPY-1\",\"isbn\":\"ISBN-1\"},"
                + "{\"copyId\":\"COPY-2\",\"isbn\":\"ISBN-2\","
                + "\"status\":\"AVAILABLE\",\"shelfLocation\":\"A1-02\"}]";
        Files.writeString(dataFile, copies);

        try (WarningCapture warnings = captureWarnings(
                Logger.getLogger("libconnect.storage.StorageManager"))) {
            assertEquals(List.of(new BookCopy("COPY-2", "ISBN-2", "A1-02")),
                    repository.findAll());
            assertTrue(warnings.containsMessage("Skipped malformed BookCopy record"));
        }
    }

    @Test
    void findAll_invalidStatus_logsAndSkipsMalformedRecord() throws IOException {
        Path dataFile = temporaryDirectory.resolve("book-copies.json");
        FileBookCopyRepository repository = createRepository(dataFile);
        String copies = "[{\"copyId\":\"COPY-1\",\"isbn\":\"ISBN-1\","
                + "\"status\":\"INVALID\",\"shelfLocation\":\"A1-01\"},"
                + "{\"copyId\":\"COPY-2\",\"isbn\":\"ISBN-2\","
                + "\"status\":\"AVAILABLE\",\"shelfLocation\":\"A1-02\"}]";
        Files.writeString(dataFile, copies);

        try (WarningCapture warnings = captureWarnings(
                Logger.getLogger("libconnect.storage.StorageManager"))) {
            assertEquals(List.of(new BookCopy("COPY-2", "ISBN-2", "A1-02")),
                    repository.findAll());
            assertTrue(warnings.containsMessage("Skipped malformed BookCopy record"));
        }
    }

    @Test
    void findAll_duplicateCopyId_logsAndSkipsLaterRecord() throws IOException {
        Path dataFile = temporaryDirectory.resolve("book-copies.json");
        FileBookCopyRepository repository = createRepository(dataFile);
        String duplicateCopies = "[{\"copyId\":\"COPY-1\",\"isbn\":\"ISBN-1\","
                + "\"status\":\"AVAILABLE\",\"shelfLocation\":\"A1-01\"},"
                + "{\"copyId\":\"COPY-1\",\"isbn\":\"ISBN-2\","
                + "\"status\":\"AVAILABLE\",\"shelfLocation\":\"A1-02\"}]";
        Files.writeString(dataFile, duplicateCopies);

        try (WarningCapture warnings = captureWarnings(
                Logger.getLogger("libconnect.storage.StorageManager"))) {
            assertEquals(List.of(new BookCopy("COPY-1", "ISBN-1", "A1-01")),
                    repository.findAll());
            assertTrue(warnings.containsMessage("Skipped malformed BookCopy record"));
        }
    }

    @Test
    void findAll_unknownField_ignoresField() throws IOException {
        Path dataFile = temporaryDirectory.resolve("book-copies.json");
        FileBookCopyRepository repository = createRepository(dataFile);
        String copyWithUnknownField = "[{\"copyId\":\"COPY-1\",\"isbn\":\"ISBN-1\","
                + "\"status\":\"AVAILABLE\",\"shelfLocation\":\"A1-01\","
                + "\"futureField\":\"ignored\"}]";
        Files.writeString(dataFile, copyWithUnknownField);

        assertEquals(List.of(new BookCopy("COPY-1", "ISBN-1", "A1-01")), repository.findAll());
    }

    private static CopyStatus otherStatus(CopyStatus status) {
        return status == CopyStatus.AVAILABLE ? CopyStatus.BORROWED : CopyStatus.AVAILABLE;
    }
}
