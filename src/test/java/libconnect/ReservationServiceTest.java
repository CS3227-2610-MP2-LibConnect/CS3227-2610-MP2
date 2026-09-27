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

    @Test
    void cancelReservation_pendingReservation_marksCancelled() {
        ReservationService service = createService();
        var reservation = service.reserveBook("m1", "b1");

        assertEquals(ReservationStatus.CANCELLED,
                service.cancelReservation(reservation.getId()).getStatus());
    }

    @Test
    void fulfilReservation_pendingReservation_marksFulfilled() {
        ReservationService service = createService();
        var reservation = service.reserveBook("m1", "b1");

        assertEquals(ReservationStatus.FULFILLED,
                service.fulfilReservation(reservation.getId()).getStatus());
    }

    @Test
    void expireReservations_expiryBoundaryAndAfterBoundary() {
        ReservationService service = createService();
        var reservation = service.reserveBook("m1", "b1");

        assertEquals(0, service.expireReservations(reservation.getExpiryDate()).size());
        assertEquals(1, service.expireReservations(reservation.getExpiryDate().plusDays(1)).size());
        assertEquals(ReservationStatus.EXPIRED,
                service.getReservation(reservation.getId()).getStatus());
    }

    @Test
    void processPendingReservations_returnsPendingQueueInCreationOrder() {
        ReservationService service = createService();
        var first = service.reserveBook("m1", "b1");
        service.cancelReservation(first.getId());
        var second = service.reserveBook("m1", "b1");

        assertEquals(java.util.List.of(second), service.processPendingReservations("b1"));
        assertEquals(java.util.Set.of(first.getId(), second.getId()), service.getReservations("m1").stream()
                .map(reservation -> reservation.getId()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(java.util.Set.of(first.getId(), second.getId()), service.getAllReservations().stream()
                .map(reservation -> reservation.getId()).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void reservationOperations_invalidInputs_rejected() {
        ReservationService service = createService();

        assertThrows(IllegalArgumentException.class, () -> service.reserveBook(" ", "b1"));
        assertThrows(IllegalArgumentException.class, () -> service.reserveBook("m1", " "));
        assertThrows(IllegalArgumentException.class, () -> service.getReservations(" "));
        assertThrows(IllegalArgumentException.class, () -> service.processPendingReservations(" "));
        assertThrows(NullPointerException.class, () -> service.expireReservations(null));
        assertThrows(IllegalArgumentException.class, () -> service.getReservation("unknown"));
    }

    @Test
    void reserveBook_invalidMemberBookOrAvailability_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> createService(false, true, true, false).reserveBook("unknown", "b1"));
        assertThrows(IllegalArgumentException.class,
                () -> createService(true, false, true, false).reserveBook("m1", "unknown"));
        assertThrows(IllegalStateException.class,
                () -> createService(true, true, true, true).reserveBook("m1", "b1"));
    }

    @Test
    void constructor_nonPositiveReservationPeriod_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> createService(true, true, true, false, Period.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> createService(true, true, true, false, Period.ofDays(-1)));
    }

    private static ReservationService createService() {
        return createService(true, true, true, false, Period.ofDays(7));
    }

    private static ReservationService createService(boolean memberExists, boolean memberActive,
                                                    boolean bookExists, boolean bookAvailable) {
        return createService(memberExists, memberActive, bookExists, bookAvailable, Period.ofDays(7));
    }

    private static ReservationService createService(boolean memberExists, boolean memberActive,
                                                    boolean bookExists, boolean bookAvailable,
                                                    Period reservationPeriod) {
        MemberDirectory members = new MemberDirectory() {
            @Override
            public boolean exists(String memberId) {
                return memberExists && memberId.equals("m1");
            }

            @Override
            public boolean isActive(String memberId) {
                return memberExists && memberActive && memberId.equals("m1");
            }
        };
        BookCatalogue books = new BookCatalogue() {
            @Override
            public boolean exists(String bookId) {
                return bookExists && bookId.equals("b1");
            }

            @Override
            public boolean isAvailable(String bookId) {
                return bookAvailable;
            }
        };
        return new ReservationService(new ServiceTestDoubles.Reservations(), members, books,
                CLOCK, reservationPeriod);
    }
}
