package libconnect.services;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import libconnect.models.BookCopy;
import libconnect.models.CopyStatus;
import libconnect.storage.file.FileBookCopyRepository;
import libconnect.util.ValidationUtils;

/** Provides book-copy operations by coordinating copies with a file-backed repository. */
public class BookCopyService {
    private final FileBookCopyRepository copyRepository;

    /** Creates a service backed by the default book-copy data file. */
    public BookCopyService() {
        this(new FileBookCopyRepository());
    }

    /**
     * Creates a service backed by the supplied file book-copy repository.
     *
     * @param copyRepository the repository used to persist book copies.
     * @throws NullPointerException if {@code copyRepository} is null.
     */
    public BookCopyService(FileBookCopyRepository copyRepository) {
        this.copyRepository = Objects.requireNonNull(copyRepository, "copyRepository");
    }

    /**
     * Returns all copies associated with a book ISBN.
     *
     * <p>The ISBN is used as the stable identifier for the associated catalogue book.</p>
     *
     * @param bookId the book ID, stored as an ISBN.
     * @return all copies associated with the book.
     * @throws IllegalArgumentException if {@code bookId} is blank.
     */
    public List<BookCopy> getCopiesByIsbn(String bookId) {
        List<BookCopy> copies = copyRepository.findByIsbn(bookId);

        return copies;
    }

    /**
     * Returns a book copy by its identifier.
     *
     * @param copyId the copy ID to search for.
     * @return the matching copy, or an empty optional if it does not exist.
     * @throws IllegalArgumentException if {@code copyId} is blank.
     */
    public Optional<BookCopy> getCopyById(String copyId) {
        return copyRepository.findById(copyId);
    }

    /**
     * Returns all copies stored at a shelf location.
     *
     * @param shelfLocation the shelf location to search for.
     * @return all copies at the shelf location.
     * @throws IllegalArgumentException if {@code shelfLocation} is blank.
     */
    public List<BookCopy> getByShelfLocation(String shelfLocation) {
        String requiredShelfLocation = ValidationUtils.requireNonBlank(shelfLocation, "shelfLocation");
        return copyRepository.findAll().stream()
                .filter(copy -> copy.getShelfLocation().equals(requiredShelfLocation))
                .toList();
    }

    /**
     * Returns all copies currently having the supplied status.
     *
     * @param status the status to search for.
     * @return all copies with the supplied status.
     * @throws NullPointerException if {@code status} is null.
     */
    public List<BookCopy> getByStatus(CopyStatus status) {
        return copyRepository.findByStatus(status);
    }

    /**
     * Creates and persists a new book copy.
     *
     * @param copyId the ID of the new copy.
     * @param isbn the ISBN of the associated book.
     * @param status the initial status of the copy.
     * @param shelfLocation the shelf location of the copy.
     * @return true if the copy was created, or false if the ID already exists.
     * @throws IllegalArgumentException if a text argument is blank.
     * @throws NullPointerException if {@code status} is null.
     */
    public void createCopy(String copyId, String isbn, CopyStatus status, String shelfLocation) {
        if (copyRepository.findById(copyId).isPresent()) {
            throw new ServiceException("Copy with ID already exists: " + copyId);
        }

        copyRepository.save(new BookCopy(copyId, isbn, status, shelfLocation));
    }

    /**
     * Creates and persists a new available or status-specific book copy with a generated unique
     * identifier.
     *
     * @param isbn the ISBN of the associated book.
     * @param status the initial status of the copy.
     * @param shelfLocation the shelf location of the copy.
     * @return true after the copy has been created.
     * @throws IllegalArgumentException if a text argument is blank.
     * @throws NullPointerException if {@code status} is null.
     */
    public boolean createCopy(String isbn, CopyStatus status, String shelfLocation) {
        String copyId = generateUniqueCopyId();
        copyRepository.save(new BookCopy(copyId, isbn, status, shelfLocation));
        return true;
    }

    /**
     * Deletes a book copy from persistence.
     *
     * @param copyId the ID of the copy to delete.
     * @throws NotFoundException if the copy does not exist.
     * @throws IllegalArgumentException if {@code copyId} is blank.
     */
    public void deleteCopy(String copyId) {
        if (!copyRepository.deleteById(copyId)) {
            throw new NotFoundException("Copy not found: " + copyId);
        }
    }

    /**
     * Deletes all book copies associated with a book ISBN.
     *
     * If no copies are associated with the ISBN, the operation completes without changing
     * repository contents.
     *
     * @param bookId the book ID, stored as an ISBN.
     * @throws IllegalArgumentException if {@code bookId} is blank.
     */
    public void deleteCopiesByIsbn(String bookId) {
        List<BookCopy> copies = copyRepository.findByIsbn(bookId);

        for (BookCopy copy : copies) {
            copyRepository.deleteById(copy.getCopyId());
        }
    }

    /**
     * Marks a book copy as borrowed and persists the updated status.
     *
     * @param copyId the ID of the copy to borrow.
     * @throws NotFoundException if the copy does not exist.
     * @throws ServiceException if the copy is not available for borrowing.
     * @throws IllegalArgumentException if {@code copyId} is blank.
     */
    public void borrowCopy(String copyId) {
        BookCopy copy = findCopy(copyId);
        try {
            copy.markBorrowed();
        } catch (IllegalStateException exception) {
            throw new ServiceException("Copy is not available for borrowing: " + copyId, exception);
        }

        copyRepository.save(copy);
    }

    /**
     * Marks a book copy as available and persists the updated status.
     *
     * @param copyId the ID of the copy to return.
     * @throws NotFoundException if the copy does not exist.
     * @throws IllegalArgumentException if {@code copyId} is blank.
     */
    public void returnCopy(String copyId) {
        markCopyAsAvailable(copyId);
    }

    /**
     * Marks a book copy as lost and persists the updated status.
     *
     * @param copyId the ID of the copy to mark as lost.
     * @throws NotFoundException if the copy does not exist.
     * @throws IllegalArgumentException if {@code copyId} is blank.
     */
    public void markCopyAsLost(String copyId) {
        if (findCopy(copyId).getStatus() == CopyStatus.LOST) {
            throw new ServiceException("Copy is already marked as lost: " + copyId);
        }
        updateStatus(copyId, CopyStatus.LOST);
    }

    /**
     * Marks a book copy as damaged and persists the updated status.
     *
     * @param copyId the ID of the copy to mark as damaged.
     * @throws NotFoundException if the copy does not exist.
     * @throws IllegalArgumentException if {@code copyId} is blank.
     * @throws ServiceException if the copy is already marked as damaged.
     */
    public void markCopyAsDamaged(String copyId) {
        if (findCopy(copyId).getStatus() == CopyStatus.DAMAGED) {
            throw new ServiceException("Copy is already marked as damaged: " + copyId);
        }
        updateStatus(copyId, CopyStatus.DAMAGED);
    }

    /**
     * Marks a book copy as available and persists the updated status.
     *
     * @param copyId the ID of the copy to mark as available.
     * @throws NotFoundException if the copy does not exist.
     * @throws IllegalArgumentException if {@code copyId} is blank.
     * @throws ServiceException if the copy is already marked as available.
     */
    public void markCopyAsAvailable(String copyId) {
        if (findCopy(copyId).getStatus() == CopyStatus.AVAILABLE) {
            throw new ServiceException("Copy is already marked as available: " + copyId);
        }
        updateStatus(copyId, CopyStatus.AVAILABLE);
    }

    /**
     * Updates a copy's shelf location and persists the change.
     *
     * @param copyId the ID of the copy to update.
     * @param newShelfLocation the new shelf location.
     * @throws NotFoundException if the copy does not exist.
     * @throws IllegalArgumentException if {@code copyId} or {@code newShelfLocation} is blank.
     */
    public void updateCopyShelfLocation(String copyId, String newShelfLocation) {
        BookCopy copy = findCopy(copyId);
        copy.updateShelfLocation(newShelfLocation);
        copyRepository.save(copy);
    }

    private BookCopy findCopy(String copyId) {
        return copyRepository.findById(copyId)
                .orElseThrow(() -> new NotFoundException("Copy not found: " + copyId));
    }

    private void updateStatus(String copyId, CopyStatus status) {
        BookCopy copy = findCopy(copyId);
        switch (status) {
        case AVAILABLE -> copy.markAvailable();
        case LOST -> copy.markLost();
        case DAMAGED -> copy.markDamaged();
        case BORROWED -> copy.markBorrowed();
        }
        copyRepository.save(copy);
    }

    private String generateUniqueCopyId() {
        String copyId;
        do {
            copyId = "COPY-" + UUID.randomUUID();
        } while (copyRepository.findById(copyId).isPresent());

        return copyId;
    }
}
