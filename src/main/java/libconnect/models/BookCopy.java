package libconnect.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

import libconnect.util.ValidationUtils;

/**
 * Represents one physical copy of a catalogue book.
 */
public class BookCopy implements libconnect.storage.repositories.Identifiable {
    private final String copyId;
    private final String isbn;
    private CopyStatus status;
    private String shelfLocation;

    /**
     * Creates an available book copy.
     *
     * @param copyId the stable identifier for the physical copy.
     * @param isbn the ISBN of the associated catalogue book.
     * @param shelfLocation the copy's shelf location.
     * @throws IllegalArgumentException if a required value is blank.
     */
    public BookCopy(String copyId, String isbn, String shelfLocation) {
        this(copyId, isbn, CopyStatus.AVAILABLE, shelfLocation);
    }

    /**
     * Creates a book copy with an explicitly supplied status.
     *
     * <p>This constructor is useful when reconstructing a copy from persistence.</p>
     *
     * @param copyId the stable identifier for the physical copy.
     * @param isbn the ISBN of the associated catalogue book.
     * @param status the copy's current status.
     * @param shelfLocation the copy's shelf location.
     * @throws IllegalArgumentException if a text value is blank.
     * @throws NullPointerException if the status is null.
    */
    @JsonCreator
    public BookCopy(@JsonProperty("copyId") String copyId,
                    @JsonProperty("isbn") String isbn,
                    @JsonProperty("status") CopyStatus status,
                    @JsonProperty("shelfLocation") String shelfLocation) {
        this.copyId = ValidationUtils.requireNonBlank(copyId, "copyId");
        this.isbn = ValidationUtils.requireNonBlank(isbn, "isbn");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.shelfLocation = ValidationUtils.requireNonBlank(shelfLocation, "shelfLocation");
    }

    /**
     * Returns the stable identifier for this physical copy.
     *
     * @return the copy ID.
     */
    public String getCopyId() {
        return copyId;
    }

    /**
     * Returns the ISBN of the associated catalogue book.
     *
     * @return the book ISBN.
     */
    public String getIsbn() {
        return isbn;
    }

    /**
     * Returns the current copy status.
     *
     * @return the copy status.
     */
    public CopyStatus getStatus() {
        return status;
    }

    /**
     * Returns the shelf location of this copy.
     *
     * @return the shelf location.
     */
    public String getShelfLocation() {
        return shelfLocation;
    }

    /**
     * Returns whether this copy is available for borrowing.
     *
     * @return true if the copy is available.
     */
    public boolean isAvailable() {
        return status == CopyStatus.AVAILABLE;
    }

    /**
     * Updates the shelf location of this copy.
     *
     * @param shelfLocation the new shelf location.
     * @throws IllegalArgumentException if the shelf location is blank.
     */
    public void updateShelfLocation(String shelfLocation) {
        this.shelfLocation = ValidationUtils.requireNonBlank(shelfLocation, "shelfLocation");
    }

    /**
     * Marks this copy as available.
     */
    public void markAvailable() {
        status = CopyStatus.AVAILABLE;
    }

    /**
     * Marks this copy as borrowed.
     *
     * @throws IllegalStateException if the copy is not currently available.
     */
    public void markBorrowed() {
        if (!isAvailable()) {
            throw new IllegalStateException("Only an available copy can be borrowed");
        }
        status = CopyStatus.BORROWED;
    }

    /**
     * Marks this copy as lost.
     */
    public void markLost() {
        status = CopyStatus.LOST;
    }

    /**
     * Marks this copy as damaged.
     */
    public void markDamaged() {
        status = CopyStatus.DAMAGED;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BookCopy otherBookCopy)) {
            return false;
        }
        return copyId.equals(otherBookCopy.copyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(copyId);
    }

    @Override
    public String toString() {
        return "BookCopy{" + "copyId='" + copyId + '\'' + ", isbn='" + isbn + '\''
                + ", status=" + status + ", shelfLocation='" + shelfLocation + '\'' + '}';
    }

    @Override 
    public String getId() {
        return copyId;
    }

}
