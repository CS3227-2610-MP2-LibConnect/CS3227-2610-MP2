package libconnect.storage.file;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import libconnect.models.Book;
import libconnect.storage.StorageManager;
import libconnect.storage.repositories.BookRepository;
import libconnect.util.ValidationUtils;

/**
 * Provides JSON file-backed persistence for catalogue books.
 */
public class FileBookRepository extends AbstractFileRepository<Book> implements BookRepository {
    private static final Path DEFAULT_DATA_FILE = Path.of("data", "books.json");

    /**
     * Creates a repository backed by {@code data/books.json}.
     *
     * @throws IllegalStateException if the data file cannot be created.
     */
    public FileBookRepository() {
        this(new StorageManager(DEFAULT_DATA_FILE.getParent()), DEFAULT_DATA_FILE);
    }

    /**
     * Creates a repository backed by the supplied file.
     *
     * @param dataFile the JSON file used for persistence.
     * @throws NullPointerException if {@code dataFile} is null.
     * @throws IllegalStateException if the data file cannot be created.
     */
    public FileBookRepository(Path dataFile) {
        this(new StorageManager(dataFile.getParent()), dataFile);
    }

    /**
     * Creates a repository with explicit storage dependencies.
     *
     * @param storageManager the manager used for storage operations.
     * @param dataFile the JSON file used for persistence.
     * @throws NullPointerException if either argument is null.
     * @throws IllegalStateException if the data file cannot be created.
     */
    public FileBookRepository(StorageManager storageManager, Path dataFile) {
        super(storageManager, dataFile, Book.class);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code title} is blank.
     * @throws IllegalStateException if the data file cannot be read.
     */
    @Override
    public List<Book> findByTitle(String title) {
        String requiredTitle = ValidationUtils.requireNonBlank(title, "title").toLowerCase();
        return readAll().stream()
                .filter(book -> book.getTitle().toLowerCase().contains(requiredTitle))
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code author} is blank.
     * @throws IllegalStateException if the data file cannot be read.
     */
    @Override
    public List<Book> findByAuthor(String author) {
        String requiredAuthor = ValidationUtils.requireNonBlank(author, "author").toLowerCase();

        return readAll().stream()
                .filter(book -> book.getAuthor().toLowerCase().contains(requiredAuthor))
                .toList();
    }

    /**
     * Finds a book by its stable ISBN.
     *
     * @param isbn the ISBN to find.
     * @return the matching book, or an empty optional if no book exists.
     * @throws IllegalArgumentException if {@code isbn} is null or blank.
     */
    @Override
    public Optional<Book> findByIsbn(String isbn) {
        return findById(isbn);
    }

    /**
     * Deletes a book by its stable ISBN.
     *
     * @param isbn the ISBN of the book to delete.
     * @return true if the book was deleted, or false otherwise.
     * @throws IllegalArgumentException if {@code isbn} is null or blank.
     */
    @Override
    public boolean deleteByIsbn(String isbn) {
        return deleteById(isbn);
    }
}
