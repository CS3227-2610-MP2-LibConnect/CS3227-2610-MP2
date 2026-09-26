package libconnect.storage.file;

import java.nio.file.Path;
import java.util.List;

import libconnect.models.Reservation;
import libconnect.models.ReservationStatus;
import libconnect.storage.StorageManager;
import libconnect.storage.repositories.ReservationRepository;

/** Stores reservations in a JSON array and provides librarian queries. */
public final class FileReservationRepository extends AbstractFileRepository<Reservation>
        implements ReservationRepository {
    /** Creates a file-backed reservation repository. */
    public FileReservationRepository(StorageManager storageManager, Path file) {
        super(storageManager, file, Reservation.class);
    }

    /** Returns reservations belonging to a member. */
    @Override
    public List<Reservation> findByMemberId(String memberId) {
        requireText(memberId, "memberId");
        return readAll().stream().filter(reservation -> reservation.getMemberId().equals(memberId)).toList();
    }

    /** Returns reservations for a book. */
    @Override
    public List<Reservation> findByBookId(String bookId) {
        requireText(bookId, "bookId");
        return readAll().stream().filter(reservation -> reservation.getBookId().equals(bookId)).toList();
    }

    /** Returns reservations in the supplied lifecycle state. */
    @Override
    public List<Reservation> findByStatus(ReservationStatus status) {
        return readAll().stream().filter(reservation -> reservation.getStatus() == status).toList();
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
