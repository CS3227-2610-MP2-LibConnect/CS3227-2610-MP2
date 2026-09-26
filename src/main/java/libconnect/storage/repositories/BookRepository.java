package libconnect.storage.repositories;

import java.util.List;
import java.util.Optional;

import libconnect.models.Book;

/**
 * Defines persistence operations for catalogue books.
 */
public interface BookRepository extends Repository<Book> {
    /**
     * Finds a book by its stable ISBN.
     *
     * @param isbn the ISBN to find.
     * @return the matching book, or an empty optional if no book exists.
     * @throws IllegalArgumentException if {@code isbn} is null or blank.
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
     * @throws IllegalArgumentException if {@code title} is null or blank.
     */
    List<Book> findByTitle(String title);

    /**
     * Finds books whose authors contain the supplied text, ignoring case.
     *
     * @param author the author text to search for.
     * @return all books with a matching author.
     * @throws IllegalArgumentException if {@code author} is null or blank.
     */
    List<Book> findByAuthor(String author);

    /**
     * Inserts a new book or replaces the existing book with the same ISBN.
     *
     * @param book the book to persist.
     * @throws NullPointerException if {@code book} is null.
     */
    void save(Book book);

    /**
     * Deletes a book by its stable ISBN.
     *
     * @param isbn the ISBN of the book to delete.
     * @return true if the book was deleted, false otherwise.
     * @throws IllegalArgumentException if {@code isbn} is null or blank.
     */
    boolean deleteByIsbn(String isbn);
}
