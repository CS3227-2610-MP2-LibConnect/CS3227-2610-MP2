package libconnect.storage.repositories;

import java.util.List;
import java.util.Optional;

import libconnect.models.BookCopy;
import libconnect.models.CopyStatus;
import libconnect.storage.exceptions.DeleteFailureException;

/**
 * Defines persistence operations for physical book copies.
 */
public interface BookCopyRepository {
    /**
     * Finds a book copy by its stable identifier.
     *
     * @param copyId the copy identifier to find.
     * @return the matching copy, or an empty optional if no copy exists.
     */
    Optional<BookCopy> findById(String copyId);

    /**
     * Returns every persisted book copy.
     *
     * @return all persisted copies.
     */
    List<BookCopy> findAll();

    /**
     * Finds all copies belonging to a catalogue book.
     *
     * @param isbn the ISBN to search for.
     * @return all copies associated with the ISBN.
     */
    List<BookCopy> findByIsbn(String isbn);

    /**
     * Deletes every book copy associated with the supplied ISBN.
     *
     * @param isbn the ISBN whose copies should be deleted.
     */
    void deleteByIsbn(String isbn);

    /**
     * Finds all copies currently having the supplied status.
     *
     * @param status the status to search for.
     * @return all copies with the supplied status.
     */
    List<BookCopy> findByStatus(CopyStatus status);

    /**
     * Inserts a new copy or replaces the existing copy with the same stable identifier.
     *
     * @param bookCopy the copy to persist.
     */
    void save(BookCopy bookCopy);

    /**
     * Deletes a copy by its stable identifier.
     *
     * @param copyId the copy identifier to delete.
     * @throws DeleteFailureException if no copy with the supplied identifier can be deleted.
     */
    void deleteById(String copyId) throws DeleteFailureException;
}
