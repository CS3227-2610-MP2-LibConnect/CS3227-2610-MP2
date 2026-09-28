package libconnect.storage.repositories;

import java.util.List;

import libconnect.models.Reservation;
import libconnect.models.ReservationStatus;

/** Defines reservation persistence and query operations. */
public interface ReservationRepository extends Repository<Reservation> {
    /** Returns reservations belonging to a member. */
    List<Reservation> findByMemberId(String memberId);

    /** Returns reservations for a book. */
    List<Reservation> findByBookId(String bookId);

    /** Returns reservations in the supplied lifecycle state. */
    List<Reservation> findByStatus(ReservationStatus status);
}
