package libconnect.services.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import libconnect.models.BookCopy;
import libconnect.models.CopyStatus;
import libconnect.services.BookCopyService;
import libconnect.services.NotFoundException;
import libconnect.services.ServiceException;
import libconnect.storage.file.FileBookCopyRepository;

/** Tests physical-copy behavior implemented by {@link BookCopyService}. */
class BookCopyServiceTest {
    @TempDir
    Path temporaryDirectory;

    private FileBookCopyRepository copyRepository;
    private BookCopyService copyService;

    @BeforeEach
    void setUp() {
        copyRepository = new FileBookCopyRepository(temporaryDirectory.resolve("copies.json"));
        copyService = new BookCopyService(copyRepository);
    }

    @Test
    void createCopy_duplicateId_throwsAndPreservesOriginal() {
        BookCopy originalCopy = copy("COPY-1", "978-1", CopyStatus.AVAILABLE, "A-1");
        copyRepository.save(originalCopy);

        assertThrows(ServiceException.class, () -> copyService.createCopy("COPY-1", "978-2", CopyStatus.LOST, "B-1"));
        assertEquals(originalCopy, copyService.getCopyById("COPY-1").orElseThrow());
    }

    @Test 
    void createCopy_sameIdDifferentCapitalization_creationIsSuccessful() {
        BookCopy originalCopy = copy("COPY-1", "978-1", CopyStatus.AVAILABLE, "A-1");
        copyRepository.save(originalCopy);

        assertDoesNotThrow(() -> copyService.createCopy("copy-1", "978-2", CopyStatus.LOST, "B-1"));
        assertNotEquals(originalCopy, copyService.getCopyById("copy-1").orElseThrow());
    }

    @Test
    void createCopy_withoutId_generatesPersistedCopyId() {
        assertTrue(copyService.createCopy("978-1", CopyStatus.AVAILABLE, "A-1"));

        BookCopy createdCopy = copyRepository.findAll().get(0);
        assertTrue(createdCopy.getCopyId().startsWith("COPY-"));
        assertEquals("978-1", createdCopy.getIsbn());
    }

    @Test
    void getCopiesByIsbnAndShelfLocation_returnsMatchingCopies() {
        BookCopy firstCopy = copy("COPY-1", "978-1", CopyStatus.AVAILABLE, "A-1");
        BookCopy secondCopy = copy("COPY-2", "978-1", CopyStatus.BORROWED, "A-1");
        copyRepository.save(firstCopy);
        copyRepository.save(secondCopy);

        assertEquals(List.of(firstCopy, secondCopy), copyService.getCopiesByIsbn("978-1"));
        assertEquals(List.of(firstCopy, secondCopy), copyService.getByShelfLocation("A-1"));
        assertEquals(List.of(firstCopy), copyService.getByStatus(CopyStatus.AVAILABLE));
    }

    @Test
    void getAllCopiesAndGetCopyById_returnPersistedCopies() {
        BookCopy firstCopy = copy("COPY-1", "978-1", CopyStatus.AVAILABLE, "A-1");
        BookCopy secondCopy = copy("COPY-2", "978-2", CopyStatus.LOST, "B-1");
        copyRepository.save(firstCopy);
        copyRepository.save(secondCopy);

        assertEquals(List.of(firstCopy, secondCopy), copyService.getAllCopies());
        assertEquals(firstCopy, copyService.getCopyById("COPY-1").orElseThrow());
        assertTrue(copyService.getCopyById("COPY-unknown").isEmpty());
    }

    @Test
    void copyQueries_blankArguments_rejected() {
        assertThrows(IllegalArgumentException.class, () -> copyService.getCopiesByIsbn("   "));
        assertThrows(IllegalArgumentException.class, () -> copyService.getCopyById("   "));
        assertThrows(IllegalArgumentException.class, () -> copyService.getByShelfLocation("   "));
        assertThrows(NullPointerException.class, () -> copyService.getByStatus(null));
    }

    @Test
    void borrowCopy_availableCopy_persistsBorrowedStatus() {
        copyRepository.save(copy("COPY-1", "978-1", CopyStatus.AVAILABLE, "A-1"));

        copyService.borrowCopy("COPY-1");

        assertEquals(CopyStatus.BORROWED, copyRepository.findById("COPY-1").orElseThrow().getStatus());
    }

    @Test
    void borrowCopy_unavailableCopy_throwsServiceExceptionAndKeepsStatus() {
        copyRepository.save(copy("COPY-1", "978-1", CopyStatus.LOST, "A-1"));

        assertThrows(ServiceException.class, () -> copyService.borrowCopy("COPY-1"));
        assertEquals(CopyStatus.LOST, copyRepository.findById("COPY-1").orElseThrow().getStatus());
    }

    @Test
    void returnCopy_borrowedCopy_persistsAvailableStatus() {
        copyRepository.save(copy("COPY-1", "978-1", CopyStatus.BORROWED, "A-1"));
        copyService.returnCopy("COPY-1");
        assertEquals(CopyStatus.AVAILABLE, copyRepository.findById("COPY-1").orElseThrow().getStatus());
    }

    @Test
    void markCopyAsDamaged_availableCopy_persistsDamagedStatus() {
        copyRepository.save(copy("COPY-1", "978-1", CopyStatus.AVAILABLE, "A-1"));
        copyService.markCopyAsDamaged("COPY-1");
        assertEquals(CopyStatus.DAMAGED, copyRepository.findById("COPY-1").orElseThrow().getStatus());
    }

    @Test
    void markCopyAsLost_damagedCopy_persistsLostStatus() {
        copyRepository.save(copy("COPY-1", "978-1", CopyStatus.DAMAGED, "A-1"));
        copyService.markCopyAsLost("COPY-1");
        assertEquals(CopyStatus.LOST, copyRepository.findById("COPY-1").orElseThrow().getStatus());
    }

    @Test
    void markCopyAsLost_alreadyLostCopy_throwsServiceException() {
        copyRepository.save(copy("COPY-1", "978-1", CopyStatus.LOST, "A-1"));

        assertThrows(ServiceException.class, () -> copyService.markCopyAsLost("COPY-1"));
    }

    @Test
    void markCopyAsDamaged_alreadyDamagedCopy_throwsServiceException() {
        copyRepository.save(copy("COPY-1", "978-1", CopyStatus.DAMAGED, "A-1"));

        assertThrows(ServiceException.class, () -> copyService.markCopyAsDamaged("COPY-1"));
    }

    @Test
    void markCopyAsAvailable_alreadyAvailableCopy_throwsServiceException() {
        copyRepository.save(copy("COPY-1", "978-1", CopyStatus.AVAILABLE, "A-1"));

        assertThrows(ServiceException.class, () -> copyService.markCopyAsAvailable("COPY-1"));
    }

    @Test
    void explicitCopyCreation_preservesSuppliedFields() {
        copyService.createCopy("COPY-1", "978-1", CopyStatus.DAMAGED, "A-1");

        assertEquals(copy("COPY-1", "978-1", CopyStatus.DAMAGED, "A-1"),
                copyService.getCopyById("COPY-1").orElseThrow());
    }

    @Test
    void copyCreation_nullStatus_rejected() {
        assertThrows(NullPointerException.class,
                () -> copyService.createCopy("COPY-1", "978-1", null, "A-1"));
    }

    @Test
    void updateCopyShelfLocation_persistsNewLocation() {
        copyRepository.save(copy("COPY-1", "978-1", CopyStatus.AVAILABLE, "A-1"));

        copyService.updateCopyShelfLocation("COPY-1", "B-2");

        assertEquals("B-2", copyRepository.findById("COPY-1").orElseThrow().getShelfLocation());
    }

    @Test
    void deleteCopy_unknownId_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> copyService.deleteCopy("COPY-unknown"));
    }

    @Test
    void deleteCopiesByIsbn_removesOnlyCopiesForRequestedBook() {
        copyRepository.save(copy("COPY-1", "978-1", CopyStatus.AVAILABLE, "A-1"));
        copyRepository.save(copy("COPY-2", "978-2", CopyStatus.AVAILABLE, "A-2"));

        copyService.deleteCopiesByIsbn("978-1");

        assertTrue(copyRepository.findById("COPY-1").isEmpty());
        assertTrue(copyRepository.findById("COPY-2").isPresent());
    }

    @Test
    void deleteCopiesByIsbn_withoutMatchingCopies_leavesRepositoryUnchanged() {
        copyRepository.save(copy("COPY-1", "978-1", CopyStatus.AVAILABLE, "A-1"));

        copyService.deleteCopiesByIsbn("978-unknown");

        assertEquals(1, copyRepository.findAll().size());
    }

    @Test
    void copyStatusOperations_unknownId_throwNotFoundException() {
        assertThrows(NotFoundException.class, () -> copyService.borrowCopy("COPY-unknown"));
        assertThrows(NotFoundException.class, () -> copyService.returnCopy("COPY-unknown"));
        assertThrows(NotFoundException.class, () -> copyService.markCopyAsLost("COPY-unknown"));
        assertThrows(NotFoundException.class, () -> copyService.markCopyAsDamaged("COPY-unknown"));
        assertThrows(NotFoundException.class, () -> copyService.markCopyAsAvailable("COPY-unknown"));
        assertThrows(NotFoundException.class,
                () -> copyService.updateCopyShelfLocation("COPY-unknown", "A-1"));
    }

    @Test
    void updateCopyShelfLocation_blankLocation_rejected() {
        copyRepository.save(copy("COPY-1", "978-1", CopyStatus.AVAILABLE, "A-1"));

        assertThrows(IllegalArgumentException.class,
                () -> copyService.updateCopyShelfLocation("COPY-1", "   "));
    }

    private static BookCopy copy(String copyId, String isbn, CopyStatus status, String shelfLocation) {
        return new BookCopy(copyId, isbn, status, shelfLocation);
    }
}
