package libconnect.cli;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Period;
import java.util.Objects;

import libconnect.services.FineService;
import libconnect.services.LibrarianService;
import libconnect.services.NotificationService;
import libconnect.services.ReservationService;
import libconnect.storage.StorageManager;
import libconnect.storage.file.FileFineRepository;
import libconnect.storage.file.FileLibrarianRepository;
import libconnect.storage.file.FileNotificationRepository;
import libconnect.storage.file.FileReservationRepository;

/** Composes the real librarian services with temporary CLI integration adapters. */
public final class CliApplication {
    private CliApplication() {
    }

    /** Creates a CLI backed by the supplied data directory and I/O streams. */
    public static LibrarianCli create(Path dataDirectory, Reader input, Writer output, Clock clock) {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(clock, "clock");

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
                java.math.BigDecimal.valueOf(1.50));
        NotificationService notificationService = new NotificationService(notificationRepository, clock);
        CliView view = new CliView(output);
        libconnect.librarian.LibrarianController controller = new libconnect.librarian.LibrarianController(
                librarianService, reservationService, fineService, notificationService, loans, view, clock,
                members, books, copies);

        return new LibrarianCli(input, output, librarianService, reservationService, fineService,
                notificationService, controller);
    }
}
