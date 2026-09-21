package libconnect.storage.repositories;

import java.util.List;
import java.util.Optional;

import libconnect.models.Book;
import libconnect.storage.exceptions.DeleteFailureException;

/**
 * Defines persistence operations for catalogue books.
 */
public interface BookRepository {
    /**
     * Finds a book by its stable ISBN.
     *
     * @param isbn the ISBN to find.
     * @return the matching book, or an empty optional if no book exists.
     */
    Optional<Book> findByIsbn(String isbn);

    /**
     * Returns every persisted book.
     *
     * @return all persisted books.
     */
    List<Book> findAll();

    /**
     * Finds books whose titles contain the supplied text, ignoring case.
     *
     * @param title the title text to search for.
     * @return all books with a matching title.
     */
    List<Book> findByTitle(String title);

    /**
     * Finds books whose authors contain the supplied text, ignoring case.
     *
     * @param author the author text to search for.
     * @return all books with a matching author.
     */
    List<Book> findByAuthor(String author);

    /**
     * Inserts a new book or replaces the existing book with the same ISBN.
     *
     * @param book the book to persist.
     */
    void save(Book book);

    /**
     * Deletes a book by its stable ISBN.
     *
     * @param isbn the ISBN of the book to delete.
     * @throws DeleteFailureException if no book with the supplied ISBN can be deleted.
     */
    void deleteByIsbn(String isbn) throws DeleteFailureException;
}
