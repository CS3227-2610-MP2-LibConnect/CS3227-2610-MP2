package libconnect;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import libconnect.models.Fine;
import libconnect.models.Librarian;
import libconnect.models.Notification;
import libconnect.models.Reservation;
import libconnect.models.ReservationStatus;
import libconnect.storage.repositories.FineRepository;
import libconnect.storage.repositories.Identifiable;
import libconnect.storage.repositories.LibrarianRepository;
import libconnect.storage.repositories.NotificationRepository;
import libconnect.storage.repositories.Repository;
import libconnect.storage.repositories.ReservationRepository;

/** Provides small in-memory repositories for isolated service tests. */
final class ServiceTestDoubles {
    private ServiceTestDoubles() {
    }

    /** Stores entities in memory with repository semantics. */
    static class InMemoryRepository<T extends Identifiable> implements Repository<T> {
        protected final List<T> entities = new ArrayList<>();

        /** Finds an entity by ID. */
        @Override
        public Optional<T> findById(String id) {
            return entities.stream().filter(entity -> entity.getId().equals(id)).findFirst();
        }

        /** Returns a snapshot of all entities. */
        @Override
        public List<T> findAll() {
            return List.copyOf(entities);
        }

        /** Creates or replaces an entity. */
        @Override
        public void save(T entity) {
            deleteById(entity.getId());
            entities.add(entity);
        }

        /** Deletes an entity by ID. */
        @Override
        public boolean deleteById(String id) {
            return entities.removeIf(entity -> entity.getId().equals(id));
        }
    }

    /** Provides an in-memory librarian repository. */
    static final class Librarians extends InMemoryRepository<Librarian> implements LibrarianRepository {
        /** Finds a librarian by email. */
        @Override
        public Optional<Librarian> findByEmail(String email) {
            return entities.stream().filter(librarian -> librarian.getEmail().equalsIgnoreCase(email)).findFirst();
        }
    }

    /** Provides an in-memory reservation repository. */
    static final class Reservations extends InMemoryRepository<Reservation>
            implements ReservationRepository {
        /** Finds reservations for a member. */
        @Override
        public List<Reservation> findByMemberId(String memberId) {
            return entities.stream().filter(reservation -> reservation.getMemberId().equals(memberId)).toList();
        }

        /** Finds reservations for a book. */
        @Override
        public List<Reservation> findByBookId(String bookId) {
            return entities.stream().filter(reservation -> reservation.getBookId().equals(bookId)).toList();
        }

        /** Finds reservations by status. */
        @Override
        public List<Reservation> findByStatus(ReservationStatus status) {
            return entities.stream().filter(reservation -> reservation.getStatus() == status).toList();
        }
    }

    /** Provides an in-memory fine repository. */
    static final class Fines extends InMemoryRepository<Fine> implements FineRepository {
        /** Finds fines for a member. */
        @Override
        public List<Fine> findByMemberId(String memberId) {
            return entities.stream().filter(fine -> fine.getMemberId().equals(memberId)).toList();
        }

        /** Finds fines for a loan. */
        @Override
        public List<Fine> findByLoanId(String loanId) {
            return entities.stream().filter(fine -> fine.getLoanId().equals(loanId)).toList();
        }
    }

    /** Provides an in-memory notification repository. */
    static final class Notifications extends InMemoryRepository<Notification>
            implements NotificationRepository {
        /** Finds notifications for a recipient. */
        @Override
        public List<Notification> findByRecipientUserId(String recipientUserId) {
            return entities.stream()
                    .filter(notification -> notification.getRecipientUserId().equals(recipientUserId))
                    .toList();
        }

        /** Checks notification idempotency. */
        @Override
        public boolean existsByRecipientAndReference(String recipientUserId,
                                                     libconnect.models.NotificationType type,
                                                     String referenceId) {
            return entities.stream().anyMatch(notification ->
                    notification.getRecipientUserId().equals(recipientUserId)
                            && notification.getType() == type
                            && notification.getReferenceId().equals(referenceId));
        }
    }
}
