package libconnect.storage.repositories;

import java.util.List;
import java.util.Optional;

import libconnect.models.BookCopy;
import libconnect.models.CopyStatus;
import libconnect.storage.exceptions.DeleteFailureException;

/** Defines persistence operations for physical book copies. */
public interface BookCopyRepository extends Repository<BookCopy> {
    /**
     * Finds a book copy by its stable identifier.
     *
     * @param copyId the copy identifier to find.
     * @return the matching copy, or an empty optional if no copy exists.
     * @throws IllegalArgumentException if {@code copyId} is null or blank.
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
     * @throws IllegalArgumentException if {@code isbn} is null or blank.
     */
    List<BookCopy> findByIsbn(String isbn);

    /**
     * Deletes every book copy associated with the supplied ISBN.
     *
     * @param isbn the ISBN whose copies should be deleted.
     * @throws IllegalArgumentException if {@code isbn} is null or blank.
     */
    void deleteByIsbn(String isbn);

    /**
     * Finds all copies currently having the supplied status.
     *
     * @param status the status to search for.
     * @return all copies with the supplied status.
     * @throws NullPointerException if {@code status} is null.
     */
    List<BookCopy> findByStatus(CopyStatus status);

    /**
     * Inserts a new copy or replaces the existing copy with the same stable identifier.
     *
     * @param bookCopy the copy to persist.
     * @throws NullPointerException if {@code bookCopy} is null.
     */
    void save(BookCopy bookCopy);

    /**
     * Deletes a copy by its stable identifier.
     *
     * @param copyId the copy identifier to delete.
     * @return true if the copy was deleted, or false if no matching copy exists.
     * @throws IllegalArgumentException if {@code copyId} is null or blank.
     * @throws DeleteFailureException if no copy with the supplied identifier can be deleted.
     */
    boolean deleteById(String copyId) throws DeleteFailureException;
}
