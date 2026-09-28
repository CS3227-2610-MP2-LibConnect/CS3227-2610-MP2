package libconnect.ui;

import java.util.Objects;

import libconnect.librarian.LibrarianController;
import libconnect.services.BookCopyService;
import libconnect.services.BookService;
import libconnect.services.FineService;
import libconnect.services.LibrarianService;
import libconnect.services.NotificationService;
import libconnect.services.ReservationService;

/** Holds the services and controller used by the librarian UI. */
public final class LibrarianRuntime {
    private final LibrarianService librarianService;
    private final BookService bookService;
    private final BookCopyService bookCopyService;
    private final ReservationService reservationService;
    private final FineService fineService;
    private final NotificationService notificationService;
    private final LibrarianController controller;

    /**
     * Creates a librarian runtime from its injected dependencies.
     *
     * @param librarianService the service used to authorize librarians.
     * @param bookService the service used to validate catalogue ISBNs.
     * @param bookCopyService the service used to manage physical book copies.
     * @param reservationService the service used to manage reservations.
     * @param fineService the service used to manage fines.
     * @param notificationService the service used to manage notifications.
     * @param controller the controller used for authorized librarian actions.
     */
    public LibrarianRuntime(LibrarianService librarianService, BookService bookService,
                            BookCopyService bookCopyService, ReservationService reservationService,
                            FineService fineService, NotificationService notificationService,
                            LibrarianController controller) {
        this.librarianService = Objects.requireNonNull(librarianService, "librarianService");
        this.bookService = Objects.requireNonNull(bookService, "bookService");
        this.bookCopyService = Objects.requireNonNull(bookCopyService, "bookCopyService");
        this.reservationService = Objects.requireNonNull(reservationService, "reservationService");
        this.fineService = Objects.requireNonNull(fineService, "fineService");
        this.notificationService = Objects.requireNonNull(notificationService, "notificationService");
        this.controller = Objects.requireNonNull(controller, "controller");
    }

    /** Returns the librarian account service. */
    public LibrarianService librarianService() {
        return librarianService;
    }

    /** Returns the catalogue book service used by librarian pages. */
    public BookService bookService() {
        return bookService;
    }

    /** Returns the physical book-copy service used by librarian pages. */
    public BookCopyService bookCopyService() {
        return bookCopyService;
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
