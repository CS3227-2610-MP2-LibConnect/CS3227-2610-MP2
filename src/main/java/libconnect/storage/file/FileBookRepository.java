package libconnect.storage.file;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import libconnect.models.Book;
import libconnect.storage.FileManager;
import libconnect.storage.exceptions.DeleteFailureException;
import libconnect.storage.repositories.BookRepository;
import libconnect.util.ValidationUtils;

/**
 * Provides JSON file-backed persistence for catalogue books.
 */
public class FileBookRepository extends FileRepositorySupport implements BookRepository {
    private static final Logger LOGGER = Logger.getLogger(FileBookRepository.class.getName());
    private static final Path DEFAULT_DATA_FILE = Path.of("data", "books.json");
    private static final String ISBN_FIELD = "isbn";
    private static final String TITLE_FIELD = "title";
    private static final String AUTHOR_FIELD = "author";
    private static final String PUBLISHER_FIELD = "publisher";
    private static final String CATEGORY_FIELD = "category";
    private static final String PUBLICATION_YEAR_FIELD = "publicationYear";

    /**
     * Creates a repository backed by {@code data/books.json}.
     *
     * @throws IllegalStateException if the data file cannot be created.
     */
    public FileBookRepository() {
        this(new FileManager(), DEFAULT_DATA_FILE);
    }

    /**
     * Creates a repository backed by the supplied file.
     *
     * @param dataFile the JSON file used for persistence.
     * @throws NullPointerException if {@code dataFile} is null.
     * @throws IllegalStateException if the data file cannot be created.
     */
    public FileBookRepository(Path dataFile) {
        this(new FileManager(), dataFile);
    }

    /**
     * Creates a repository with explicit storage dependencies.
     *
     * @param fileManager the manager used for file operations.
     * @param dataFile the JSON file used for persistence.
     * @throws NullPointerException if either argument is null.
     * @throws IllegalStateException if the data file cannot be created.
     */
    public FileBookRepository(FileManager fileManager, Path dataFile) {
        super(fileManager, dataFile, "books");
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code isbn} is blank.
     * @throws IllegalStateException if the data file cannot be read.
     */
    @Override
    public Optional<Book> findByIsbn(String isbn) {
        String requiredIsbn = ValidationUtils.requireNonBlank(isbn, ISBN_FIELD);

        return findFirst(this::parseBooks, book -> book.getIsbn().equals(requiredIsbn));
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if the data file cannot be read.
     */
    @Override
    public List<Book> findAll() {
        return readAllRecords(this::parseBooks);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code title} is blank.
     * @throws IllegalStateException if the data file cannot be read.
     */
    @Override
    public List<Book> findByTitle(String title) {
        String requiredTitle = ValidationUtils.requireNonBlank(title, TITLE_FIELD).toLowerCase();

        return findMatching(this::parseBooks,
                book -> book.getTitle().toLowerCase().contains(requiredTitle));
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code author} is blank.
     * @throws IllegalStateException if the data file cannot be read.
     */
    @Override
    public List<Book> findByAuthor(String author) {
        String requiredAuthor = ValidationUtils.requireNonBlank(author, AUTHOR_FIELD).toLowerCase();

        return findMatching(this::parseBooks,
                book -> book.getAuthor().toLowerCase().contains(requiredAuthor));
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code book} is null.
     * @throws IllegalStateException if the data file cannot be read or written.
     */
    @Override
    public void save(Book book) {
        Objects.requireNonNull(book, "book cannot be null");

        List<Book> books = readRecords(this::parseBooks);
        upsert(books, book, existingBook -> existingBook.getIsbn().equals(book.getIsbn()));

        writeRecords(books, this::toJson);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code isbn} is blank.
     * @throws DeleteFailureException if no book with the supplied ISBN can be deleted.
     * @throws IllegalStateException if the data file cannot be read or written.
     */
    @Override
    public void deleteByIsbn(String isbn) throws DeleteFailureException {
        String requiredIsbn = ValidationUtils.requireNonBlank(isbn, ISBN_FIELD);
        List<Book> books = readRecords(this::parseBooks);

        deleteMatching(books, book -> book.getIsbn().equals(requiredIsbn),
                this::toJson, "No book found with ISBN: " + requiredIsbn);
    }

    /**
     * Parses a JSON array containing book objects.
     *
     * @param json the JSON content to parse.
     * @return the parsed books.
     * @throws IllegalArgumentException if the JSON structure or a record is invalid.
     */
    private List<Book> parseBooks(String json) {
        JsonParser parser = new JsonParser(json);
        List<Book> books = parser.parseBookArray();
        Set<String> isbns = new LinkedHashSet<>();
        List<Book> validBooks = new ArrayList<>();

        for (Book book : books) {
            if (!isbns.add(book.getIsbn())) {
                logMalformedRecord(LOGGER, "book", "Duplicate ISBN in book data: "
                        + book.getIsbn(), null);
                continue;
            }
            validBooks.add(book);
        }

        return validBooks;
    }

    /**
     * Serializes books as a readable JSON array.
     *
     * @param books the books to serialize.
     * @return the JSON representation of the books.
     */
    private String toJson(List<Book> books) {
        return JsonWriter.toJsonArray(books, this::toJsonObject);
    }

    /**
     * Serializes one book as a JSON object.
     *
     * @param book the book to serialize.
     * @return the JSON object representation of the book.
     */
    private String toJsonObject(Book book) {
        return "  {\n"
                + "    \"" + ISBN_FIELD + "\": " + JsonWriter.quote(book.getIsbn()) + ",\n"
                + "    \"" + TITLE_FIELD + "\": " + JsonWriter.quote(book.getTitle()) + ",\n"
                + "    \"" + AUTHOR_FIELD + "\": " + JsonWriter.quote(book.getAuthor()) + ",\n"
                + "    \"" + PUBLISHER_FIELD + "\": " + JsonWriter.quote(book.getPublisher()) + ",\n"
                + "    \"" + CATEGORY_FIELD + "\": " + JsonWriter.quote(book.getCategory()) + ",\n"
                + "    \"" + PUBLICATION_YEAR_FIELD + "\": " + book.getPublicationYear() + "\n"
                + "  }";
    }

    /**
     * Parses the limited JSON structure used by this repository.
     */
    private static final class JsonParser extends JsonReader {

        /**
         * Creates a parser for JSON content.
         *
         * @param json the JSON content to parse.
         */
        private JsonParser(String json) {
            super(json, "book");
        }

        /**
         * Parses the root JSON array into books.
         *
         * @return the parsed books.
         * @throws IllegalArgumentException if the JSON is invalid.
         */
        private List<Book> parseBookArray() {
            return parseArray(this::parseBook,
                    exception -> logMalformedRecord(LOGGER, "book", exception.getMessage(),
                            exception));
        }

        /**
         * Parses one book JSON object.
         *
         * @return the parsed book.
         * @throws IllegalArgumentException if the object is invalid.
         */
        private Book parseBook() {
            skipWhitespace();
            expect('{');
            skipWhitespace();

            String isbn = null;
            String title = null;
            String author = null;
            String publisher = null;
            String category = null;
            Integer publicationYear = null;

            if (!consume('}')) {
                while (true) {
                    String fieldName = parseString();
                    skipWhitespace();
                    expect(':');

                    switch (fieldName) {
                    case ISBN_FIELD -> isbn = parseString();
                    case TITLE_FIELD -> title = parseString();
                    case AUTHOR_FIELD -> author = parseString();
                    case PUBLISHER_FIELD -> publisher = parseString();
                    case CATEGORY_FIELD -> category = parseString();
                    case PUBLICATION_YEAR_FIELD -> publicationYear = parseInteger();
                    default -> skipValue();
                    }

                    skipWhitespace();
                    if (consume('}')) {
                        break;
                    }
                    expect(',');
                    skipWhitespace();
                }
            }

            if (isbn == null || title == null || author == null || publisher == null
                    || category == null || publicationYear == null) {
                throw new IllegalArgumentException("Book record is missing a required field");
            }

            try {
                return new Book(isbn, title, author, publisher, category, publicationYear);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Invalid book record", exception);
            }
        }

    }
}
