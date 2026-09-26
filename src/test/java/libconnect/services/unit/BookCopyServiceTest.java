package libconnect.services.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void createCopy_duplicateId_returnsFalseAndPreservesOriginal() {
        BookCopy originalCopy = copy("COPY-1", "978-1", CopyStatus.AVAILABLE, "A-1");
        copyRepository.save(originalCopy);

        assertThrows(ServiceException.class, () -> copyService.createCopy("COPY-1", "978-2", CopyStatus.LOST, "B-1"));
        assertEquals(originalCopy, copyService.getCopyById("COPY-1").orElseThrow());
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
    void returnAndMarkOperations_updateCopyStatus() {
        copyRepository.save(copy("COPY-1", "978-1", CopyStatus.BORROWED, "A-1"));
        copyService.returnCopy("COPY-1");
        assertEquals(CopyStatus.AVAILABLE, copyRepository.findById("COPY-1").orElseThrow().getStatus());

        copyService.markCopyAsDamaged("COPY-1");
        assertEquals(CopyStatus.DAMAGED, copyRepository.findById("COPY-1").orElseThrow().getStatus());

        copyService.markCopyAsLost("COPY-1");
        assertEquals(CopyStatus.LOST, copyRepository.findById("COPY-1").orElseThrow().getStatus());
    }

    @Test
    void markCopyAsLost_alreadyLostCopy_throwsServiceException() {
        copyRepository.save(copy("COPY-1", "978-1", CopyStatus.LOST, "A-1"));

        assertThrows(ServiceException.class, () -> copyService.markCopyAsLost("COPY-1"));
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

    private static BookCopy copy(String copyId, String isbn, CopyStatus status, String shelfLocation) {
        return new BookCopy(copyId, isbn, status, shelfLocation);
    }
}
