package libconnect.storage.file;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import libconnect.models.BookCopy;
import libconnect.models.CopyStatus;
import libconnect.storage.FileManager;
import libconnect.storage.exceptions.DeleteFailureException;
import libconnect.storage.repositories.BookCopyRepository;
import libconnect.util.ValidationUtils;

/**
 * Provides JSON file-backed persistence for physical book copies.
 */
public class FileBookCopyRepository extends FileRepositorySupport implements BookCopyRepository {
    private static final Logger LOGGER = Logger.getLogger(FileBookCopyRepository.class.getName());
    private static final Path DEFAULT_DATA_FILE = Path.of("data", "book-copies.json");
    private static final String COPY_ID_FIELD = "copyId";
    private static final String ISBN_FIELD = "isbn";
    private static final String STATUS_FIELD = "status";
    private static final String SHELF_LOCATION_FIELD = "shelfLocation";

    /**
     * Creates a repository backed by {@code data/book-copies.json}.
     *
     * @throws IllegalStateException if the data file cannot be created.
     */
    public FileBookCopyRepository() {
        this(new FileManager(), DEFAULT_DATA_FILE);
    }

    /**
     * Creates a repository backed by the supplied file.
     *
     * @param dataFile the JSON file used for persistence.
     * @throws NullPointerException if {@code dataFile} is null.
     * @throws IllegalStateException if the data file cannot be created.
     */
    public FileBookCopyRepository(Path dataFile) {
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
    public FileBookCopyRepository(FileManager fileManager, Path dataFile) {
        super(fileManager, dataFile, "book copies");
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code copyId} is blank.
     * @throws IllegalStateException if the data file cannot be read.
     */
    @Override
    public Optional<BookCopy> findById(String copyId) {
        String requiredCopyId = ValidationUtils.requireNonBlank(copyId, COPY_ID_FIELD);

        return findFirst(this::parseCopies, copy -> copy.getCopyId().equals(requiredCopyId));
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if the data file cannot be read.
     */
    @Override
    public List<BookCopy> findAll() {
        return readAllRecords(this::parseCopies);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code isbn} is blank.
     * @throws IllegalStateException if the data file cannot be read.
     */
    @Override
    public List<BookCopy> findByIsbn(String isbn) {
        String requiredIsbn = ValidationUtils.requireNonBlank(isbn, ISBN_FIELD);

        return findMatching(this::parseCopies, copy -> copy.getIsbn().equals(requiredIsbn));
    }

    /**
     * {@inheritDoc}
     * 
     * Not requried to throw DeleteFailureException if no copies are found with the
     * given ISBN, as this is to delete all copies with the given ISBN prior to 
     * deleting the book with the given ISBN. 
     * If no copies are found, it is still valid to delete the book.
     *
     * @throws IllegalArgumentException if {@code isbn} is blank.
     * @throws IllegalStateException if the data file cannot be read or written.
     */
    @Override
    public void deleteByIsbn(String isbn) {
        String requiredIsbn = ValidationUtils.requireNonBlank(isbn, ISBN_FIELD);
        List<BookCopy> copies = readRecords(this::parseCopies);

        boolean wasRemoved = removeMatching(copies, copy -> copy.getIsbn().equals(requiredIsbn));
        if (wasRemoved) {
            writeRecords(copies, this::toJson);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code status} is null.
     * @throws IllegalStateException if the data file cannot be read.
     */
    @Override
    public List<BookCopy> findByStatus(CopyStatus status) {
        Objects.requireNonNull(status, "status cannot be null");

        return findMatching(this::parseCopies, copy -> copy.getStatus() == status);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code bookCopy} is null.
     * @throws IllegalStateException if the data file cannot be read or written.
     */
    @Override
    public void save(BookCopy bookCopy) {
        Objects.requireNonNull(bookCopy, "bookCopy cannot be null");

        List<BookCopy> copies = readRecords(this::parseCopies);
        upsert(copies, bookCopy,
                existingCopy -> existingCopy.getCopyId().equals(bookCopy.getCopyId()));

        writeRecords(copies, this::toJson);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code copyId} is blank.
     * @throws DeleteFailureException if no copy with the supplied identifier can be deleted.
     * @throws IllegalStateException if the data file cannot be read or written.
     */
    @Override
    public void deleteById(String copyId) throws DeleteFailureException {
        String requiredCopyId = ValidationUtils.requireNonBlank(copyId, COPY_ID_FIELD);
        List<BookCopy> copies = readRecords(this::parseCopies);

        deleteMatching(copies, copy -> copy.getCopyId().equals(requiredCopyId),
                this::toJson, "No book copy found with copyId: " + requiredCopyId);
    }

    /**
     * Parses a JSON array containing book-copy objects.
     *
     * @param json the JSON content to parse.
     * @return the parsed copies.
     * @throws IllegalArgumentException if the JSON structure or a record is invalid.
     */
    private List<BookCopy> parseCopies(String json) {
        JsonParser parser = new JsonParser(json);
        List<BookCopy> copies = parser.parseCopyArray();
        Set<String> copyIds = new LinkedHashSet<>();
        List<BookCopy> validCopies = new ArrayList<>();

        for (BookCopy copy : copies) {
            if (!copyIds.add(copy.getCopyId())) {
                logMalformedRecord(LOGGER, "book-copy", "Duplicate copyId in book-copy data: "
                        + copy.getCopyId(), null);
                continue;
            }
            validCopies.add(copy);
        }

        return validCopies;
    }

    /**
     * Serializes copies as a readable JSON array.
     *
     * @param copies the copies to serialize.
     * @return the JSON representation of the copies.
     */
    private String toJson(List<BookCopy> copies) {
        return JsonWriter.toJsonArray(copies, this::toJsonObject);
    }

    /**
     * Serializes one copy as a JSON object.
     *
     * @param copy the copy to serialize.
     * @return the JSON object representation of the copy.
     */
    private String toJsonObject(BookCopy copy) {
        return "  {\n"
                + "    \"" + COPY_ID_FIELD + "\": " + JsonWriter.quote(copy.getCopyId()) + ",\n"
                + "    \"" + ISBN_FIELD + "\": " + JsonWriter.quote(copy.getIsbn()) + ",\n"
                + "    \"" + STATUS_FIELD + "\": " + JsonWriter.quote(copy.getStatus().name()) + ",\n"
                + "    \"" + SHELF_LOCATION_FIELD + "\": " + JsonWriter.quote(copy.getShelfLocation()) + "\n"
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
            super(json, "book-copy");
        }

        /**
         * Parses the root JSON array into book copies.
         *
         * @return the parsed book copies.
         * @throws IllegalArgumentException if the JSON is invalid.
         */
        private List<BookCopy> parseCopyArray() {
            return parseArray(this::parseCopy,
                    exception -> logMalformedRecord(LOGGER, "book-copy", exception.getMessage(),
                            exception));
        }

        /**
         * Parses one book-copy JSON object.
         *
         * @return the parsed book copy.
         * @throws IllegalArgumentException if the object is invalid.
         */
        private BookCopy parseCopy() {
            skipWhitespace();
            expect('{');
            skipWhitespace();

            String copyId = null;
            String isbn = null;
            String status = null;
            String shelfLocation = null;

            if (!consume('}')) {
                while (true) {
                    String fieldName = parseString();
                    skipWhitespace();
                    expect(':');
                    String fieldValue = parseString();

                    switch (fieldName) {
                    case COPY_ID_FIELD -> copyId = fieldValue;
                    case ISBN_FIELD -> isbn = fieldValue;
                    case STATUS_FIELD -> status = fieldValue;
                    case SHELF_LOCATION_FIELD -> shelfLocation = fieldValue;
                    default -> {
                        // Unknown fields are ignored for forward compatibility.
                    }
                    }

                    skipWhitespace();
                    if (consume('}')) {
                        break;
                    }
                    expect(',');
                    skipWhitespace();
                }
            }

            if (copyId == null || isbn == null || status == null || shelfLocation == null) {
                throw new IllegalArgumentException("Book-copy record is missing a required field");
            }

            try {
                return new BookCopy(copyId, isbn, CopyStatus.valueOf(status), shelfLocation);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Invalid book-copy record", exception);
            }
        }

    }
}
