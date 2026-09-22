package libconnect.services;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import libconnect.models.Book;
import libconnect.storage.file.FileBookRepository;
import libconnect.util.ValidationUtils;

/** Provides catalogue book operations through file-backed repositories. */
public class BookService {
    private final FileBookRepository bookRepository;
    private final BookCopyService bookCopyService;

    /** Creates a service backed by the default book and book-copy data files. */
    public BookService() {
        this(new FileBookRepository(), new BookCopyService());
    }

    /**
     * Creates a service backed by the supplied repositories and copy service.
     *
     * @param bookRepository the repository used to persist books.
     * @param bookCopyService the service used to delete copies associated with a book.
     * @throws NullPointerException if either argument is null.
     */
    public BookService(FileBookRepository bookRepository, BookCopyService bookCopyService) {
        this.bookRepository = Objects.requireNonNull(bookRepository, "bookRepository");
        this.bookCopyService = Objects.requireNonNull(bookCopyService, "bookCopyService");
    }

    /**
     * Returns all books in the supplied category.
     *
     * <p>Category matching is case-insensitive and ignores surrounding whitespace.</p>
     *
     * @param category the category to search for.
     * @return all books in the category.
     * @throws IllegalArgumentException if {@code category} is blank.
     */
    public List<Book> getBooksByCategory(String category) {
        String requiredCategory = ValidationUtils.requireNonBlank(category, "category");
        return bookRepository.findAll().stream()
                .filter(book -> book.getCategory().equalsIgnoreCase(requiredCategory))
                .toList();
    }

    /**
     * Returns books whose authors contain the supplied text.
     *
     * @param author the author text to search for.
     * @return all books with a matching author.
     * @throws IllegalArgumentException if {@code author} is blank.
     */
    public List<Book> getBooksByAuthor(String author) {
        return bookRepository.findByAuthor(author);
    }

    /**
     * Returns books published within an inclusive year range.
     *
     * @param startYear the first publication year in the range.
     * @param endYear the last publication year in the range.
     * @return all books published between the supplied years, inclusive.
     * @throws IllegalArgumentException if {@code startYear} is greater than {@code endYear}.
     */
    public List<Book> getBooksByPublicationYear(int startYear, int endYear) {
        if (startYear > endYear) {
            throw new IllegalArgumentException("startYear must not be greater than endYear");
        }

        return bookRepository.findAll().stream()
                .filter(book -> book.getPublicationYear() >= startYear
                        && book.getPublicationYear() <= endYear)
                .toList();
    }

    /**
     * Returns books whose titles contain the supplied text.
     *
     * @param title the title text to search for.
     * @return all books with a matching title.
     * @throws IllegalArgumentException if {@code title} is blank.
     */
    public List<Book> getBooksByTitle(String title) {
        return bookRepository.findByTitle(title);
    }

    /**
     * Returns a book by its ISBN.
     *
     * @param isbn the ISBN to search for.
     * @return the matching book, or an empty optional if it does not exist.
     * @throws IllegalArgumentException if {@code isbn} is blank.
     */
    public Optional<Book> getBookByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn);
    }

    /**
     * Returns all persisted books.
     *
     * @return all books in the catalogue.
     */
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    /**
     * Creates and persists a new book.
     *
     * @param isbn the ISBN of the new book.
     * @param title the title of the new book.
     * @param author the author of the new book.
     * @param publisher the publisher of the new book.
     * @param category the category of the new book.
     * @param publicationYear the publication year of the new book.
     * @return true if the book was created, or false if the ISBN already exists.
     */
    public boolean createBook(String isbn, String title, String author, String publisher,
                              String category, int publicationYear) {
        if (bookRepository.findByIsbn(isbn).isPresent()) {
            return false;
        }

        bookRepository.save(new Book(isbn, title, author, publisher, category, publicationYear));
        return true;
    }

    /**
     * Updates an existing book and persists its new catalogue metadata.
     *
     * @param isbn the ISBN of the book to update.
     * @param title the updated title.
     * @param author the updated author.
     * @param publisher the updated publisher.
     * @param category the updated category.
     * @param publicationYear the updated publication year.
     * @throws NotFoundException if no book has the supplied ISBN.
     */
    public void updateBook(String isbn, String title, String author, String publisher,
                           String category, int publicationYear) {
        findBook(isbn);
        bookRepository.save(new Book(isbn, title, author, publisher, category, publicationYear));
    }

    /**
     * Deletes a book and all copies associated with its ISBN.
     *
     * @param isbn the ISBN of the book to delete.
     * @throws NotFoundException if no book has the supplied ISBN.
     */
    public void deleteBook(String isbn) {
        findBook(isbn);
        bookCopyService.deleteCopiesByIsbn(isbn);
        if (!bookRepository.deleteByIsbn(isbn)) {
            throw new NotFoundException("Book not found: " + isbn);
        }
    }

    private Book findBook(String isbn) {
        return bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new NotFoundException("Book not found: " + isbn));
    }
}
