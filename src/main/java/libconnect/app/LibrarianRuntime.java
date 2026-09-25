package libconnect.app;

import java.util.Objects;

import libconnect.librarian.LibrarianController;
import libconnect.services.FineService;
import libconnect.services.LibrarianService;
import libconnect.services.NotificationService;
import libconnect.services.ReservationService;

/** Holds the librarian services and controller shared by desktop entry points. */
public final class LibrarianRuntime {
    private final LibrarianService librarianService;
    private final ReservationService reservationService;
    private final FineService fineService;
    private final NotificationService notificationService;
    private final LibrarianController controller;

    /** Creates a runtime from its injected librarian dependencies. */
    public LibrarianRuntime(LibrarianService librarianService, ReservationService reservationService,
                            FineService fineService, NotificationService notificationService,
                            LibrarianController controller) {
        this.librarianService = Objects.requireNonNull(librarianService, "librarianService");
        this.reservationService = Objects.requireNonNull(reservationService, "reservationService");
        this.fineService = Objects.requireNonNull(fineService, "fineService");
        this.notificationService = Objects.requireNonNull(notificationService, "notificationService");
        this.controller = Objects.requireNonNull(controller, "controller");
    }

    /** Returns the librarian account service. */
    public LibrarianService librarianService() {
        return librarianService;
    }

    /** Returns the reservation service. */
    public ReservationService reservationService() {
        return reservationService;
    }

    /** Returns the fine service. */
    public FineService fineService() {
        return fineService;
    }

    /** Returns the notification service. */
    public NotificationService notificationService() {
        return notificationService;
    }

    /** Returns the authorized librarian controller. */
    public LibrarianController controller() {
        return controller;
    }
}
