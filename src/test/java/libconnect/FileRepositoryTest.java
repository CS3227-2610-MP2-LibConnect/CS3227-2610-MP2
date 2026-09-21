package libconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
}
