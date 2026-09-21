package libconnect.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.Objects;

/** Represents a reservation using stable member and book identifiers. */
public final class Reservation implements libconnect.storage.repositories.Identifiable {
    private final String reservationId;
    private final String memberId;
    private final String bookId;
    private final LocalDate reservationDate;
    private final LocalDate expiryDate;
    private final ReservationStatus status;

    /** Creates a reservation with a valid date range and initial status. */
    public Reservation(String reservationId, String memberId, String bookId,
                       LocalDate reservationDate, LocalDate expiryDate,
                       ReservationStatus status) {
        this.reservationId = requireText(reservationId, "reservationId");
        this.memberId = requireText(memberId, "memberId");
        this.bookId = requireText(bookId, "bookId");
        this.reservationDate = Objects.requireNonNull(reservationDate, "reservationDate");
        this.expiryDate = Objects.requireNonNull(expiryDate, "expiryDate");
        if (expiryDate.isBefore(reservationDate)) {
            throw new IllegalArgumentException("expiryDate must not be before reservationDate");
        }
        this.status = Objects.requireNonNull(status, "status");
    }

    /** Returns the stable reservation identifier. */
    @Override
    @JsonProperty("reservationId")
    public String getId() {
        return reservationId;
    }

    /** Returns the member identifier owning this reservation. */
    public String getMemberId() {
        return memberId;
    }

    /** Returns the book identifier being reserved. */
    public String getBookId() {
        return bookId;
    }

    /** Returns the date on which the reservation was created. */
    public LocalDate getReservationDate() {
        return reservationDate;
    }

    /** Returns the date on which a pending reservation expires. */
    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    /** Returns the current reservation status. */
    public ReservationStatus getStatus() {
        return status;
    }

    /** Returns whether the reservation is still pending on the supplied date. */
    public boolean isPendingOn(LocalDate date) {
        Objects.requireNonNull(date, "date");
        return status == ReservationStatus.PENDING && !date.isAfter(expiryDate);
    }

    /** Returns a copy with the supplied lifecycle status. */
    public Reservation withStatus(ReservationStatus newStatus) {
        return new Reservation(reservationId, memberId, bookId, reservationDate,
                expiryDate, newStatus);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
