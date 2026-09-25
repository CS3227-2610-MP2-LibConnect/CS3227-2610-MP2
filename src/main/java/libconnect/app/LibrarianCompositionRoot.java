package libconnect.app;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Period;
import java.util.Objects;

import libconnect.cli.DemoBookCopyManagement;
import libconnect.cli.DemoBookManagement;
import libconnect.cli.DemoLoanQuery;
import libconnect.cli.DemoMemberManagement;
import libconnect.librarian.LibrarianController;
import libconnect.librarian.LibrarianView;
import libconnect.services.FineService;
import libconnect.services.LibrarianService;
import libconnect.services.NotificationService;
import libconnect.services.ReservationService;
import libconnect.storage.StorageManager;
import libconnect.storage.file.FileFineRepository;
import libconnect.storage.file.FileLibrarianRepository;
import libconnect.storage.file.FileNotificationRepository;
import libconnect.storage.file.FileReservationRepository;

/** Composes librarian services for desktop and manual application entry points. */
public final class LibrarianCompositionRoot {
    private LibrarianCompositionRoot() {
    }

    /** Creates a runtime backed by the supplied data directory and clock. */
    public static LibrarianRuntime create(Path dataDirectory, Clock clock, LibrarianView view) {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        Objects.requireNonNull(clock, "clock");
        Objects.requireNonNull(view, "view");

        StorageManager storageManager = new StorageManager(dataDirectory);
        FileLibrarianRepository librarianRepository = new FileLibrarianRepository(storageManager,
                dataDirectory.resolve("librarians.json"));
        FileReservationRepository reservationRepository = new FileReservationRepository(storageManager,
                dataDirectory.resolve("reservations.json"));
        FileFineRepository fineRepository = new FileFineRepository(storageManager,
                dataDirectory.resolve("fines.json"));
        FileNotificationRepository notificationRepository = new FileNotificationRepository(storageManager,
                dataDirectory.resolve("notifications.json"));

        DemoMemberManagement members = new DemoMemberManagement();
        DemoBookManagement books = new DemoBookManagement();
        DemoBookCopyManagement copies = new DemoBookCopyManagement();
        DemoLoanQuery loans = new DemoLoanQuery(clock);
        LibrarianService librarianService = new LibrarianService(librarianRepository);
        ReservationService reservationService = new ReservationService(reservationRepository, members, books,
                clock, Period.ofDays(7));
        FineService fineService = new FineService(fineRepository, loans, members, clock,
                BigDecimal.valueOf(1.50));
        NotificationService notificationService = new NotificationService(notificationRepository, clock);
        LibrarianController controller = new LibrarianController(librarianService, reservationService, fineService,
                notificationService, loans, view, clock, members, books, copies);

        return new LibrarianRuntime(librarianService, reservationService, fineService, notificationService,
                controller);
    }
}
