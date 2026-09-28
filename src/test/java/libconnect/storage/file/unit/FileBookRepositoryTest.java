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
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import libconnect.models.Book;
import libconnect.storage.file.FileBookRepository;

/**
 * Tests unit-level JSON persistence for catalogue books.
 */
class FileBookRepositoryTest extends AbstractFileRepositoryTest<Book, FileBookRepository> {

    @Override
    protected FileBookRepository createRepository(Path dataFile) {
        return new FileBookRepository(dataFile);
    }

    @Override
    protected void save(FileBookRepository repository, Book book) {
        repository.save(book);
    }

    @Override
    protected List<Book> findAll(FileBookRepository repository) {
        return repository.findAll();
    }

    @Override
    protected String getIdentifier(Book book) {
        return book.getIsbn();
    }

    @Override
    protected Book createEntity(String identifier, String variant) {
        return new Book(identifier, "Title " + variant, "Author " + variant,
                "Publisher " + variant, "Category " + variant, 2025);
    }

    @Test
    void constructor_nullDataFile_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new FileBookRepository(null));
    }

    @Test
    void save_preservesAllMetadataAfterReload() {
        FileBookRepository repository = createRepository(
                temporaryDirectory.resolve("books.json"));
        Book book = new Book("978-1", "A title", "An author", "A publisher",
                "A category", 2024);

        repository.save(book);

        Book savedBook = repository.findByIsbn("978-1").orElseThrow();
        assertAll(
                () -> assertEquals("978-1", savedBook.getIsbn()),
                () -> assertEquals("A title", savedBook.getTitle()),
                () -> assertEquals("An author", savedBook.getAuthor()),
                () -> assertEquals("A publisher", savedBook.getPublisher()),
                () -> assertEquals("A category", savedBook.getCategory()),
                () -> assertEquals(2024, savedBook.getPublicationYear()));
    }

    @Test
    void save_escapesSpecialCharactersBeforeWritingJson() {
        FileBookRepository repository = createRepository(
                temporaryDirectory.resolve("books.json"));
        Book book = new Book("978-1", "A \"title\"\\", "Author\nName", "Publisher",
                "Category", 2025);

        repository.save(book);

        assertEquals(book, repository.findByIsbn("978-1").orElseThrow());
        assertEquals(book.getTitle(), repository.findByIsbn("978-1").orElseThrow().getTitle());
        assertEquals(book.getAuthor(), repository.findByIsbn("978-1").orElseThrow().getAuthor());
    }

    @Test
    void findByIsbn_existingIsbn_returnsMatchingBook() {
        FileBookRepository repository = createRepository(
                temporaryDirectory.resolve("books.json"));
        Book expectedBook = createBook("978-1", "Title");
        repository.save(expectedBook);

        Optional<Book> result = repository.findByIsbn(" 978-1 ");

        assertEquals(Optional.of(expectedBook), result);
    }

    @Test
    void findByIsbn_unknownIsbn_returnsEmptyOptional() {
        FileBookRepository repository = createRepository(
                temporaryDirectory.resolve("books.json"));

        assertTrue(repository.findByIsbn("978-1").isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void findByIsbn_blankIsbn_throwsIllegalArgumentException(String isbn) {
        FileBookRepository repository = createRepository(
                temporaryDirectory.resolve("books.json"));

        assertThrows(IllegalArgumentException.class, () -> repository.findByIsbn(isbn));
    }

    @Test
    void findByTitle_matchingTextIgnoringCase_returnsAllMatchingBooks() {
        FileBookRepository repository = createRepository(
                temporaryDirectory.resolve("books.json"));
        Book firstBook = new Book("978-1", "The Great Gatsby", "Author 1", "Publisher",
                "Category", 1925);
        Book secondBook = new Book("978-2", "A Great Adventure", "Author 2", "Publisher",
                "Category", 2020);
        repository.save(firstBook);
        repository.save(secondBook);

        List<Book> matchingBooks = repository.findByTitle(" GREAT ");

        assertEquals(List.of(firstBook, secondBook), matchingBooks);
    }

    @Test
    void findByTitle_noMatch_returnsEmptyList() {
        FileBookRepository repository = createRepository(
                temporaryDirectory.resolve("books.json"));
        repository.save(createBook("978-1", "The Great Gatsby"));

        assertTrue(repository.findByTitle("History").isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void findByTitle_blankTitle_throwsIllegalArgumentException(String title) {
        FileBookRepository repository = createRepository(
                temporaryDirectory.resolve("books.json"));

        assertThrows(IllegalArgumentException.class, () -> repository.findByTitle(title));
    }

    @Test
    void findByAuthor_matchingTextIgnoringCase_returnsAllMatchingBooks() {
        FileBookRepository repository = createRepository(
                temporaryDirectory.resolve("books.json"));
        Book firstBook = new Book("978-1", "Title 1", "Mary Shelley", "Publisher",
                "Category", 1818);
        Book secondBook = new Book("978-2", "Title 2", "Shelley Jackson", "Publisher",
                "Category", 2009);
        repository.save(firstBook);
        repository.save(secondBook);

        List<Book> matchingBooks = repository.findByAuthor(" SHELLEY ");

        assertEquals(List.of(firstBook, secondBook), matchingBooks);
    }

    @Test
    void findByAuthor_noMatch_returnsEmptyList() {
        FileBookRepository repository = createRepository(
                temporaryDirectory.resolve("books.json"));
        repository.save(createBook("978-1", "Title"));

        assertTrue(repository.findByAuthor("Unknown").isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void findByAuthor_blankAuthor_throwsIllegalArgumentException(String author) {
        FileBookRepository repository = createRepository(
                temporaryDirectory.resolve("books.json"));

        assertThrows(IllegalArgumentException.class, () -> repository.findByAuthor(author));
    }

    @Test
    void save_nullBook_throwsNullPointerException() {
        FileBookRepository repository = createRepository(
                temporaryDirectory.resolve("books.json"));

        assertThrows(NullPointerException.class, () -> repository.save(null));
    }

    @Test
    void deleteByIsbn_existingIsbn_removesMatchingBook() {
        FileBookRepository repository = createRepository(
                temporaryDirectory.resolve("books.json"));
        Book bookToDelete = createBook("978-1", "To delete");
        Book retainedBook = createBook("978-2", "To retain");
        repository.save(bookToDelete);
        repository.save(retainedBook);

        repository.deleteByIsbn(" 978-1 ");

        assertEquals(List.of(retainedBook), repository.findAll());
    }

    @Test
    void deleteByIsbn_unknownIsbn_returnsFalseAndPreservesData() {
        FileBookRepository repository = createRepository(
                temporaryDirectory.resolve("books.json"));
        Book existingBook = createBook("978-1", "Existing");
        repository.save(existingBook);

        assertFalse(repository.deleteByIsbn("978-2"));
        assertEquals(List.of(existingBook), repository.findAll());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void deleteByIsbn_blankIsbn_throwsIllegalArgumentException(String isbn) {
        FileBookRepository repository = createRepository(
                temporaryDirectory.resolve("books.json"));

        assertThrows(IllegalArgumentException.class, () -> repository.deleteByIsbn(isbn));
    }

    @Test
    void findAll_recordMissingRequiredField_logsAndSkipsMalformedRecord() throws IOException {
        Path dataFile = temporaryDirectory.resolve("books.json");
        FileBookRepository repository = createRepository(dataFile);
        String books = "[{\"isbn\":\"978-1\",\"title\":\"Title\"},"
                + "{\"isbn\":\"978-2\",\"title\":\"Valid\","
                + "\"author\":\"Author\",\"publisher\":\"Publisher\","
                + "\"category\":\"Category\",\"publicationYear\":2025}]";
        Files.writeString(dataFile, books);

        try (WarningCapture warnings = captureWarnings(
                Logger.getLogger("libconnect.storage.StorageManager"))) {
            assertEquals(List.of(createBook("978-2", "Valid")), repository.findAll());
            assertTrue(warnings.containsMessage("Skipped malformed Book record"));
        }
    }

    @Test
    void findAll_nonPositivePublicationYear_logsAndSkipsMalformedRecord() throws IOException {
        Path dataFile = temporaryDirectory.resolve("books.json");
        FileBookRepository repository = createRepository(dataFile);
        String books = "[{\"isbn\":\"978-1\",\"title\":\"Invalid\","
                + "\"author\":\"Author\",\"publisher\":\"Publisher\","
                + "\"category\":\"Category\",\"publicationYear\":\"invalid\"},"
                + "{\"isbn\":\"978-2\",\"title\":\"Valid\","
                + "\"author\":\"Author\",\"publisher\":\"Publisher\","
                + "\"category\":\"Category\",\"publicationYear\":2025}]";
        Files.writeString(dataFile, books);

        try (WarningCapture warnings = captureWarnings(
                Logger.getLogger("libconnect.storage.StorageManager"))) {
            assertEquals(List.of(createBook("978-2", "Valid")), repository.findAll());
            assertTrue(warnings.containsMessage("Skipped malformed Book record"));
        }
    }

    @Test
    void findAll_duplicateIsbn_logsAndSkipsLaterRecord() throws IOException {
        Path dataFile = temporaryDirectory.resolve("books.json");
        FileBookRepository repository = createRepository(dataFile);
        String duplicateBooks = "[{\"isbn\":\"978-1\",\"title\":\"Title 1\","
                + "\"author\":\"Author 1\",\"publisher\":\"Publisher\","
                + "\"category\":\"Category\",\"publicationYear\":2020},"
                + "{\"isbn\":\"978-1\",\"title\":\"Title 2\","
                + "\"author\":\"Author 2\",\"publisher\":\"Publisher\","
                + "\"category\":\"Category\",\"publicationYear\":2021}]";
        Files.writeString(dataFile, duplicateBooks);

        try (WarningCapture warnings = captureWarnings(
                Logger.getLogger("libconnect.storage.StorageManager"))) {
            assertEquals(List.of(new Book("978-1", "Title 1", "Author 1", "Publisher",
                    "Category", 2020)), repository.findAll());
            assertTrue(warnings.containsMessage("Skipped malformed Book record"));
        }
    }

    @Test
    void findAll_unknownField_ignoresField() throws IOException {
        Path dataFile = temporaryDirectory.resolve("books.json");
        FileBookRepository repository = createRepository(dataFile);
        String bookWithUnknownField = "[{\"isbn\":\"978-1\",\"title\":\"Title\","
                + "\"author\":\"Author\",\"publisher\":\"Publisher\","
                + "\"category\":\"Category\",\"publicationYear\":2025,"
                + "\"futureField\":\"ignored\"}]";
        Files.writeString(dataFile, bookWithUnknownField);

        assertEquals(List.of(createBook("978-1", "Title")), repository.findAll());
    }

    private static Book createBook(String isbn, String title) {
        return new Book(isbn, title, "Author", "Publisher", "Category", 2025);
    }
}
