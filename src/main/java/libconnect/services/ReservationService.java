package libconnect.services;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import libconnect.integration.BookCatalogue;
import libconnect.integration.MemberDirectory;
import libconnect.models.Reservation;
import libconnect.models.ReservationStatus;
import libconnect.storage.repositories.ReservationRepository;

/** Coordinates librarian reservation rules without depending on file storage. */
public final class ReservationService {
    private final ReservationRepository repository;
    private final MemberDirectory memberDirectory;
    private final BookCatalogue bookCatalogue;
    private final Clock clock;
    private final Period reservationPeriod;

    /** Creates a reservation service with an injectable clock and expiry period. */
    public ReservationService(ReservationRepository repository, MemberDirectory memberDirectory,
                              BookCatalogue bookCatalogue, Clock clock, Period reservationPeriod) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.memberDirectory = Objects.requireNonNull(memberDirectory, "memberDirectory");
        this.bookCatalogue = Objects.requireNonNull(bookCatalogue, "bookCatalogue");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.reservationPeriod = Objects.requireNonNull(reservationPeriod, "reservationPeriod");
        if (reservationPeriod.isNegative() || reservationPeriod.isZero()) {
            throw new IllegalArgumentException("reservationPeriod must be positive");
        }
    }

    /** Creates a pending reservation for an unavailable book. */
    public Reservation reserveBook(String memberId, String bookId) {
        requireText(memberId, "memberId");
        requireText(bookId, "bookId");
        if (!memberDirectory.exists(memberId) || !memberDirectory.isActive(memberId)) {
            throw new IllegalArgumentException("Member does not exist or is inactive");
        }
        if (!bookCatalogue.exists(bookId)) {
            throw new IllegalArgumentException("Book does not exist");
        }
        if (bookCatalogue.isAvailable(bookId)) {
            throw new IllegalStateException("Available books do not need a reservation");
        }
        boolean duplicate = repository.findByMemberId(memberId).stream()
                .anyMatch(reservation -> reservation.getBookId().equals(bookId)
                        && reservation.getStatus() == ReservationStatus.PENDING);
        if (duplicate) {
            throw new IllegalStateException("Member already has a pending reservation for this book");
        }
        LocalDate reservationDate = LocalDate.now(clock);
        Reservation reservation = new Reservation(UUID.randomUUID().toString(), memberId, bookId,
                reservationDate, reservationDate.plus(reservationPeriod), ReservationStatus.PENDING);
        repository.save(reservation);
        return reservation;
    }

    /** Cancels a pending reservation. */
    public Reservation cancelReservation(String reservationId) {
        Reservation reservation = getRequired(reservationId);
        ensurePending(reservation);
        Reservation cancelled = reservation.withStatus(ReservationStatus.CANCELLED);
        repository.save(cancelled);
        return cancelled;
    }

    /** Marks a pending reservation as fulfilled. */
    public Reservation fulfilReservation(String reservationId) {
        Reservation reservation = getRequired(reservationId);
        ensurePending(reservation);
        Reservation fulfilled = reservation.withStatus(ReservationStatus.FULFILLED);
        repository.save(fulfilled);
        return fulfilled;
    }

    /** Marks all reservations past their expiry date as expired. */
    public List<Reservation> expireReservations(LocalDate date) {
        Objects.requireNonNull(date, "date");
        List<Reservation> expired = repository.findByStatus(ReservationStatus.PENDING).stream()
                .filter(reservation -> date.isAfter(reservation.getExpiryDate()))
                .map(reservation -> reservation.withStatus(ReservationStatus.EXPIRED))
                .toList();
        expired.forEach(repository::save);
        return expired;
    }

    /** Returns all reservations for a member in creation order. */
    public List<Reservation> getReservations(String memberId) {
        requireText(memberId, "memberId");
        return repository.findByMemberId(memberId).stream()
                .sorted(Comparator.comparing(Reservation::getReservationDate)
                        .thenComparing(Reservation::getId))
                .toList();
    }

    /** Returns all reservations in creation order. */
    public List<Reservation> getAllReservations() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(Reservation::getReservationDate)
                        .thenComparing(Reservation::getId))
                .toList();
    }

    /** Returns a reservation by stable identifier. */
    public Reservation getReservation(String reservationId) {
        return getRequired(reservationId);
    }

    /** Returns pending reservations for a book in fair queue order. */
    public List<Reservation> processPendingReservations(String bookId) {
        requireText(bookId, "bookId");
        return repository.findByBookId(bookId).stream()
                .filter(reservation -> reservation.isPendingOn(LocalDate.now(clock)))
                .sorted(Comparator.comparing(Reservation::getReservationDate)
                        .thenComparing(Reservation::getId))
                .toList();
    }

    private Reservation getRequired(String reservationId) {
        requireText(reservationId, "reservationId");
        return repository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation does not exist"));
    }

    private static void ensurePending(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only pending reservations can change state");
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
