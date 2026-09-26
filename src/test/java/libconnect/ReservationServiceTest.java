package libconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import libconnect.integration.BookCatalogue;
import libconnect.integration.MemberDirectory;
import libconnect.models.ReservationStatus;
import libconnect.services.ReservationService;

/** Tests reservation validation and lifecycle behavior. */
class ReservationServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-21T00:00:00Z"), ZoneOffset.UTC);

    /** Verifies that a valid unavailable-book reservation is created. */
    @Test
    void reserveBook_validUnavailableBook_createsPendingReservation() {
        ReservationService service = createService();

        var reservation = service.reserveBook("m1", "b1");

        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(LocalDate.of(2026, 9, 28), reservation.getExpiryDate());
    }

    /** Verifies that duplicate pending reservations are rejected. */
    @Test
    void reserveBook_duplicatePendingReservation_rejected() {
        ReservationService service = createService();
        service.reserveBook("m1", "b1");

        assertThrows(IllegalStateException.class, () -> service.reserveBook("m1", "b1"));
    }

    /** Verifies that only pending reservations can be fulfilled. */
    @Test
    void fulfilReservation_cancelledReservation_rejected() {
        ReservationService service = createService();
        var reservation = service.reserveBook("m1", "b1");
        service.cancelReservation(reservation.getId());

        assertThrows(IllegalStateException.class, () -> service.fulfilReservation(reservation.getId()));
    }

    private static ReservationService createService() {
        MemberDirectory members = new MemberDirectory() {
            @Override
            public boolean exists(String memberId) {
                return memberId.equals("m1");
            }

            @Override
            public boolean isActive(String memberId) {
                return memberId.equals("m1");
            }
        };
        BookCatalogue books = new BookCatalogue() {
            @Override
            public boolean exists(String bookId) {
                return bookId.equals("b1");
            }

            @Override
            public boolean isAvailable(String bookId) {
                return false;
            }
        };
        return new ReservationService(new ServiceTestDoubles.Reservations(), members, books,
                CLOCK, Period.ofDays(7));
    }
}
