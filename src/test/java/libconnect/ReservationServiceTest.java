package libconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.List;

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

    @Test
    void reserveBook_invalidMemberBookOrAvailableBook_rejected() {
        ReservationService service = createService();

        assertThrows(IllegalArgumentException.class, () -> service.reserveBook("missing", "b1"));
        assertThrows(IllegalArgumentException.class, () -> service.reserveBook("m1", "missing"));
        assertThrows(IllegalArgumentException.class, () -> service.reserveBook(" ", "b1"));
        assertThrows(IllegalArgumentException.class, () -> service.reserveBook("m1", " "));
        assertThrows(IllegalStateException.class,
                () -> createService(true, true, true, true).reserveBook("m1", "b1"));
    }

    @Test
    void reserveBook_inactiveMember_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> createService(true, false, true, false).reserveBook("m1", "b1"));
    }

    @Test
    void reservationLifecycle_fulfilAndCancel_updateState() {
        ReservationService service = createService();
        var reservation = service.reserveBook("m1", "b1");

        assertEquals(ReservationStatus.CANCELLED,
                service.cancelReservation(reservation.getId()).getStatus());

        var fulfilment = service.reserveBook("m1", "b1");
        assertEquals(ReservationStatus.FULFILLED,
                service.fulfilReservation(fulfilment.getId()).getStatus());
        assertThrows(IllegalStateException.class, () -> service.cancelReservation(fulfilment.getId()));
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
    void reservationLifecycle_missingReservation_rejected() {
        ReservationService service = createService();

        assertThrows(IllegalArgumentException.class, () -> service.cancelReservation("missing"));
        assertThrows(IllegalArgumentException.class, () -> service.getReservation("missing"));
    }

    @Test
    void expireReservations_expiryDateIsInclusive() {
        ServiceTestDoubles.Reservations repository = new ServiceTestDoubles.Reservations();
        repository.save(new libconnect.models.Reservation("r1", "m1", "b1",
                LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 28), ReservationStatus.PENDING));
        repository.save(new libconnect.models.Reservation("r2", "m1", "b1",
                LocalDate.of(2026, 9, 19), LocalDate.of(2026, 9, 20), ReservationStatus.PENDING));
        repository.save(new libconnect.models.Reservation("r3", "m1", "b1",
                LocalDate.of(2026, 9, 19), LocalDate.of(2026, 9, 20), ReservationStatus.CANCELLED));
        ReservationService service = createService(repository);

        assertEquals(List.of("r2"), service.expireReservations(LocalDate.of(2026, 9, 28)).stream()
                .map(reservation -> reservation.getId()).toList());
        assertEquals(List.of("r1"), service.processPendingReservations("b1").stream()
                .map(reservation -> reservation.getId()).toList());
        assertEquals(List.of("r1"), service.expireReservations(LocalDate.of(2026, 9, 29)).stream()
                .map(reservation -> reservation.getId()).toList());
        assertEquals(ReservationStatus.EXPIRED, service.getReservation("r1").getStatus());
        assertEquals(ReservationStatus.EXPIRED, service.getReservation("r2").getStatus());
        assertEquals(ReservationStatus.CANCELLED, service.getReservation("r3").getStatus());
    }

    @Test
    void processPendingReservations_ordersByDateAndId_andExcludesInactiveReservations() {
        ServiceTestDoubles.Reservations repository = new ServiceTestDoubles.Reservations();
        repository.save(new libconnect.models.Reservation("r2", "m1", "b1",
                LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 30), ReservationStatus.PENDING));
        repository.save(new libconnect.models.Reservation("r1", "m1", "b1",
                LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 30), ReservationStatus.PENDING));
        repository.save(new libconnect.models.Reservation("r3", "m1", "b1",
                LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 20), ReservationStatus.PENDING));
        repository.save(new libconnect.models.Reservation("r4", "m1", "b1",
                LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 30), ReservationStatus.FULFILLED));
        ReservationService service = createService(repository);

        assertEquals(List.of("r1", "r2"), service.processPendingReservations("b1").stream()
                .map(reservation -> reservation.getId()).toList());
        assertFalse(service.getReservations("m1").isEmpty());
    }

    @Test
    void reservationService_nonPositivePeriod_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> createService(new ServiceTestDoubles.Reservations(), Period.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> createService(new ServiceTestDoubles.Reservations(), Period.ofDays(-1)));
    }

    private static ReservationService createService() {
        return createService(new ServiceTestDoubles.Reservations(), Period.ofDays(7));
    }

    private static ReservationService createService(ServiceTestDoubles.Reservations repository) {
        return createService(repository, Period.ofDays(7));
    }

    private static ReservationService createService(ServiceTestDoubles.Reservations repository,
                                                    Period reservationPeriod) {
        return createService(true, true, true, false, repository, reservationPeriod);
    }

    private static ReservationService createService(boolean memberExists, boolean memberActive,
                                                    boolean bookExists, boolean bookAvailable) {
        return createService(memberExists, memberActive, bookExists, bookAvailable,
                new ServiceTestDoubles.Reservations(), Period.ofDays(7));
    }

    private static ReservationService createService(boolean memberExists, boolean memberActive,
                                                    boolean bookExists, boolean bookAvailable,
                                                    ServiceTestDoubles.Reservations repository,
                                                    Period reservationPeriod) {
        MemberDirectory members = new MemberDirectory() {
            @Override
            public boolean exists(String memberId) {
                return memberExists && memberId.equals("m1");
            }

            @Override
            public boolean isActive(String memberId) {
                return memberActive && memberId.equals("m1");
            }
        };
        BookCatalogue books = new BookCatalogue() {
            @Override
            public boolean exists(String bookId) {
                return bookExists && bookId.equals("b1");
            }

            @Override
            public boolean isAvailable(String bookId) {
                return bookAvailable && bookId.equals("b1");
            }
        };
        return new ReservationService(repository, members, books, CLOCK, reservationPeriod);
    }
}
