package libconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import libconnect.models.AccountStatus;
import libconnect.models.Fine;
import libconnect.models.FineStatus;
import libconnect.models.Librarian;
import libconnect.models.Notification;
import libconnect.models.NotificationType;
import libconnect.models.Reservation;
import libconnect.models.ReservationStatus;
import libconnect.storage.StorageManager;
import libconnect.storage.file.FileFineRepository;
import libconnect.storage.file.FileLibrarianRepository;
import libconnect.storage.file.FileNotificationRepository;
import libconnect.storage.file.FileReservationRepository;
import libconnect.storage.repositories.RepositoryException;

/** Tests file-backed persistence using isolated temporary directories. */
class FileRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    /** Verifies that librarian records survive a repository reload. */
    @Test
    void librarianRepository_roundTrip_persistsRecord() {
        StorageManager storageManager = new StorageManager(temporaryDirectory);
        Path file = temporaryDirectory.resolve("librarians.json");
        FileLibrarianRepository repository = new FileLibrarianRepository(storageManager, file);
        Librarian librarian = new Librarian("e1", "Ada", "ada@example.com", AccountStatus.ACTIVE);

        repository.save(librarian);

        assertEquals(librarian.getEmail(), new FileLibrarianRepository(storageManager, file)
                .findById("e1").orElseThrow().getEmail());
    }

    /** Verifies that reservation, fine, and notification queries filter by stable IDs. */
    @Test
    void repositories_queries_filterByStableIds() {
        StorageManager storageManager = new StorageManager(temporaryDirectory);
        FileReservationRepository reservations = new FileReservationRepository(storageManager,
                temporaryDirectory.resolve("reservations.json"));
        reservations.save(new Reservation("r1", "m1", "b1", LocalDate.now(),
                LocalDate.now().plusDays(7), ReservationStatus.PENDING));
        assertEquals(1, reservations.findByMemberId("m1").size());

        FileFineRepository fines = new FileFineRepository(storageManager,
                temporaryDirectory.resolve("fines.json"));
        fines.save(new Fine("f1", "l1", "m1", BigDecimal.TEN, "overdue",
                FineStatus.OUTSTANDING, LocalDate.now()));
        assertEquals(1, fines.findByLoanId("l1").size());

        FileNotificationRepository notifications = new FileNotificationRepository(storageManager,
                temporaryDirectory.resolve("notifications.json"));
        notifications.save(new Notification("n1", "m1", NotificationType.OVERDUE_ALERT,
                "l1", "Overdue", LocalDateTime.now(), false));
        assertTrue(notifications.existsByRecipientAndReference("m1", NotificationType.OVERDUE_ALERT, "l1"));
    }

    @Test
    void repository_missingFile_isInitializedAsEmptyArray() {
        StorageManager storageManager = new StorageManager(temporaryDirectory);
        FileLibrarianRepository repository = new FileLibrarianRepository(storageManager,
                temporaryDirectory.resolve("new-librarians.json"));

        assertTrue(repository.findAll().isEmpty());
        assertTrue(temporaryDirectory.resolve("new-librarians.json").toFile().exists());
    }

    @Test
    void repository_saveExistingId_replacesWithoutDuplicating() {
        StorageManager storageManager = new StorageManager(temporaryDirectory);
        FileLibrarianRepository repository = new FileLibrarianRepository(storageManager,
                temporaryDirectory.resolve("librarians.json"));
        repository.save(new Librarian("e1", "Ada", "ada@example.com", AccountStatus.ACTIVE));
        repository.save(new Librarian("e1", "Grace", "grace@example.com", AccountStatus.INACTIVE));

        assertEquals(1, repository.findAll().size());
        assertEquals("Grace", repository.findById("e1").orElseThrow().getName());
        assertEquals(AccountStatus.INACTIVE, repository.findById("e1").orElseThrow().getStatus());
    }

    @Test
    void repository_delete_existingAndMissing_reportsResult() {
        StorageManager storageManager = new StorageManager(temporaryDirectory);
        FileLibrarianRepository repository = new FileLibrarianRepository(storageManager,
                temporaryDirectory.resolve("librarians.json"));
        repository.save(new Librarian("e1", "Ada", "ada@example.com", AccountStatus.ACTIVE));

        assertTrue(repository.deleteById("e1"));
        assertFalse(repository.deleteById("e1"));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void repository_blankQueryIds_rejected() {
        StorageManager storageManager = new StorageManager(temporaryDirectory);
        FileReservationRepository reservations = new FileReservationRepository(storageManager,
                temporaryDirectory.resolve("reservations.json"));

        assertThrows(IllegalArgumentException.class, () -> reservations.findById(" "));
        assertThrows(IllegalArgumentException.class, () -> reservations.findByMemberId(" "));
        assertThrows(IllegalArgumentException.class, () -> reservations.findByBookId(" "));
        assertThrows(IllegalArgumentException.class, () -> reservations.deleteById(" "));
    }

    @Test
    void repository_queries_filterByAllSupportedFields() {
        StorageManager storageManager = new StorageManager(temporaryDirectory);
        FileReservationRepository reservations = new FileReservationRepository(storageManager,
                temporaryDirectory.resolve("reservations.json"));
        reservations.save(new Reservation("r1", "m1", "b1", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 7), ReservationStatus.PENDING));
        reservations.save(new Reservation("r2", "m2", "b1", LocalDate.of(2026, 1, 2),
                LocalDate.of(2026, 1, 8), ReservationStatus.FULFILLED));

        assertEquals(1, reservations.findByMemberId("m1").size());
        assertEquals(2, reservations.findByBookId("b1").size());
        assertEquals(1, reservations.findByStatus(ReservationStatus.FULFILLED).size());

        FileFineRepository fines = new FileFineRepository(storageManager,
                temporaryDirectory.resolve("fines.json"));
        fines.save(new Fine("f1", "l1", "m1", BigDecimal.TEN, "overdue",
                FineStatus.OUTSTANDING, LocalDate.of(2026, 1, 1)));
        assertEquals(1, fines.findByMemberId("m1").size());
        assertEquals(1, fines.findByLoanId("l1").size());

        FileNotificationRepository notifications = new FileNotificationRepository(storageManager,
                temporaryDirectory.resolve("notifications.json"));
        notifications.save(new Notification("n1", "m1", NotificationType.OVERDUE_ALERT,
                "l1", "Overdue", LocalDateTime.of(2026, 1, 1, 9, 0), false));
        assertEquals(1, notifications.findByRecipientUserId("m1").size());
        assertFalse(notifications.existsByRecipientAndReference("m1",
                NotificationType.RESERVATION_REMINDER, "l1"));
    }

    @Test
    void librarianRepository_emailLookup_isCaseInsensitiveAndValidatesInput() {
        StorageManager storageManager = new StorageManager(temporaryDirectory);
        FileLibrarianRepository repository = new FileLibrarianRepository(storageManager,
                temporaryDirectory.resolve("librarians.json"));
        repository.save(new Librarian("e1", "Ada", "Ada@Example.com", AccountStatus.ACTIVE));

        assertEquals("e1", repository.findByEmail(" ada@example.com ").orElseThrow().getId());
        assertThrows(IllegalArgumentException.class, () -> repository.findByEmail(" "));
    }

    @Test
    void repository_roundTrip_preservesDatesStatusesAndReferences() {
        StorageManager storageManager = new StorageManager(temporaryDirectory);
        Path file = temporaryDirectory.resolve("notifications.json");
        FileNotificationRepository repository = new FileNotificationRepository(storageManager, file);
        Notification notification = new Notification("n1", "m1", NotificationType.RESERVATION_REMINDER,
                "r1", "Ready", LocalDateTime.of(2026, 1, 2, 10, 30), false);

        repository.save(notification);

        Notification loaded = new FileNotificationRepository(storageManager, file)
                .findById("n1").orElseThrow();
        assertEquals(notification.getCreatedAt(), loaded.getCreatedAt());
        assertEquals(notification.getType(), loaded.getType());
        assertEquals(notification.getReferenceId(), loaded.getReferenceId());
    }
}
