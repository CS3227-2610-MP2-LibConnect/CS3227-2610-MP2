package libconnect.storage.file.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import libconnect.models.Reservation;
import libconnect.models.ReservationStatus;
import libconnect.storage.StorageManager;
import libconnect.storage.file.FileReservationRepository;

/** Tests reservation-specific queries in the file-backed repository. */
class FileReservationRepositoryTest extends AbstractFileRepositoryTest<Reservation,
        FileReservationRepository> {

    @Override
    protected FileReservationRepository createRepository(Path dataFile) {
        return new FileReservationRepository(new StorageManager(temporaryDirectory), dataFile);
    }

    @Override
    protected void save(FileReservationRepository repository, Reservation reservation) {
        repository.save(reservation);
    }

    @Override
    protected List<Reservation> findAll(FileReservationRepository repository) {
        return repository.findAll();
    }

    @Override
    protected String getIdentifier(Reservation reservation) {
        return reservation.getId();
    }

    @Override
    protected Reservation createEntity(String identifier, String variant) {
        LocalDate reservationDate = LocalDate.of(2026, 9, 1);
        return new Reservation(identifier, "MEM-" + variant, "BOOK-" + variant,
                reservationDate, reservationDate.plusDays(7), ReservationStatus.PENDING);
    }

    @Test
    void queries_filterByMemberBookAndStatus() {
        FileReservationRepository repository = createRepository(
                temporaryDirectory.resolve("reservations.json"));
        Reservation pending = createEntity("RES-1", "1");
        Reservation fulfilled = new Reservation("RES-2", "MEM-1", "BOOK-2",
                pending.getReservationDate(), pending.getExpiryDate(), ReservationStatus.FULFILLED);
        repository.save(pending);
        repository.save(fulfilled);

        assertEquals(List.of("RES-1", "RES-2"), repository.findByMemberId("MEM-1").stream()
                .map(Reservation::getId).toList());
        assertEquals(List.of("RES-1"), repository.findByBookId("BOOK-1").stream()
                .map(Reservation::getId).toList());
        assertEquals(List.of("RES-1"), repository.findByStatus(ReservationStatus.PENDING).stream()
                .map(Reservation::getId).toList());
        assertTrue(repository.findByStatus(ReservationStatus.CANCELLED).isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void findByMemberId_blankId_throwsIllegalArgumentException(String memberId) {
        FileReservationRepository repository = createRepository(
                temporaryDirectory.resolve("reservations.json"));

        assertThrows(IllegalArgumentException.class, () -> repository.findByMemberId(memberId));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void findByBookId_blankId_throwsIllegalArgumentException(String bookId) {
        FileReservationRepository repository = createRepository(
                temporaryDirectory.resolve("reservations.json"));

        assertThrows(IllegalArgumentException.class, () -> repository.findByBookId(bookId));
    }

    @Test
    void findByStatus_nullStatus_throwsNullPointerException() {
        FileReservationRepository repository = createRepository(
                temporaryDirectory.resolve("reservations.json"));

        assertThrows(NullPointerException.class, () -> repository.findByStatus(null));
    }
}
