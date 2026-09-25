package libconnect.librarian;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import libconnect.integration.BookDetails;
import libconnect.integration.BookCopyManagement;
import libconnect.integration.BookManagement;
import libconnect.integration.BookSummary;
import libconnect.integration.LoanQuery;
import libconnect.integration.LoanSummary;
import libconnect.integration.MemberManagement;
import libconnect.integration.MemberSummary;
import libconnect.models.Fine;
import libconnect.models.Notification;
import libconnect.models.Reservation;
import libconnect.services.FineService;
import libconnect.services.LibrarianService;
import libconnect.services.NotificationService;
import libconnect.services.ReservationService;

/** Coordinates authorized librarian actions without containing business rules. */
public final class LibrarianController {
    private final LibrarianService librarianService;
    private final ReservationService reservationService;
    private final FineService fineService;
    private final NotificationService notificationService;
    private final LoanQuery loanQuery;
    private final LibrarianView view;
    private final Clock clock;
    private final MemberManagement memberManagement;
    private final BookManagement bookManagement;
    private final BookCopyManagement bookCopyManagement;

    /** Creates a librarian controller with injected services and presentation boundary. */
    public LibrarianController(LibrarianService librarianService,
                               ReservationService reservationService, FineService fineService,
                               NotificationService notificationService, LoanQuery loanQuery,
                               LibrarianView view, Clock clock, MemberManagement memberManagement,
                               BookManagement bookManagement, BookCopyManagement bookCopyManagement) {
        this.librarianService = Objects.requireNonNull(librarianService, "librarianService");
        this.reservationService = Objects.requireNonNull(reservationService, "reservationService");
        this.fineService = Objects.requireNonNull(fineService, "fineService");
        this.notificationService = Objects.requireNonNull(notificationService, "notificationService");
        this.loanQuery = Objects.requireNonNull(loanQuery, "loanQuery");
        this.view = Objects.requireNonNull(view, "view");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.memberManagement = Objects.requireNonNull(memberManagement, "memberManagement");
        this.bookManagement = Objects.requireNonNull(bookManagement, "bookManagement");
        this.bookCopyManagement = Objects.requireNonNull(bookCopyManagement, "bookCopyManagement");
    }

    /** Returns all active loans after checking librarian authorization. */
    public List<LoanSummary> viewLoans(String employeeId) {
        librarianService.requireActive(employeeId);
        return loanQuery.findActiveLoans();
    }

    /** Returns overdue loans after checking librarian authorization. */
    public List<LoanSummary> viewOverdueLoans(String employeeId) {
        librarianService.requireActive(employeeId);
        return loanQuery.findOverdueLoans(LocalDate.now(clock));
    }

    /** Returns a member's reservations after checking librarian authorization. */
    public List<Reservation> viewReservations(String employeeId, String memberId) {
        librarianService.requireActive(employeeId);
        return reservationService.getReservations(memberId);
    }

    /** Returns all reservations after checking librarian authorization. */
    public List<Reservation> viewAllReservations(String employeeId) {
        librarianService.requireActive(employeeId);
        return reservationService.getAllReservations();
    }

    /** Creates a reservation after checking librarian authorization. */
    public Reservation createReservation(String employeeId, String memberId, String bookId) {
        librarianService.requireActive(employeeId);
        return reservationService.reserveBook(memberId, bookId);
    }

    /** Cancels a reservation after checking librarian authorization. */
    public Reservation cancelReservation(String employeeId, String reservationId) {
        librarianService.requireActive(employeeId);
        return reservationService.cancelReservation(reservationId);
    }

    /** Fulfils a reservation after checking librarian authorization. */
    public Reservation fulfilReservation(String employeeId, String reservationId) {
        librarianService.requireActive(employeeId);
        return reservationService.fulfilReservation(reservationId);
    }

    /** Returns pending reservations for a book after checking librarian authorization. */
    public List<Reservation> viewPendingReservations(String employeeId, String bookId) {
        librarianService.requireActive(employeeId);
        return reservationService.processPendingReservations(bookId);
    }

    /** Returns a member's fines after checking librarian authorization. */
    public List<Fine> viewFines(String employeeId, String memberId) {
        librarianService.requireActive(employeeId);
        return fineService.getMemberFines(memberId);
    }

    /** Returns all fines after checking librarian authorization. */
    public List<Fine> viewAllFines(String employeeId) {
        librarianService.requireActive(employeeId);
        return fineService.getAllFines();
    }

    /** Creates an overdue fine after checking librarian authorization. */
    public Fine createFine(String employeeId, String loanId) {
        librarianService.requireActive(employeeId);
        return fineService.createFine(loanId);
    }

    /** Returns notifications for a user after checking librarian authorization. */
    public List<Notification> viewNotifications(String employeeId, String userId) {
        librarianService.requireActive(employeeId);
        return notificationService.getNotifications(userId);
    }

    /** Marks a notification as read after checking librarian authorization. */
    public Notification markNotificationRead(String employeeId, String notificationId) {
        librarianService.requireActive(employeeId);
        return notificationService.markAsRead(notificationId);
    }

    /** Registers a member through the member-role integration contract. */
    public void registerMember(String employeeId, String memberId, String name, String email) {
        librarianService.requireActive(employeeId);
        memberManagement.registerMember(memberId, name, email);
    }

    /** Edits a member through the member-role integration contract. */
    public void editMember(String employeeId, String memberId, String name, String email) {
        librarianService.requireActive(employeeId);
        memberManagement.editMember(memberId, name, email);
    }

    /** Deactivates a member through the member-role integration contract. */
    public void deactivateMember(String employeeId, String memberId) {
        librarianService.requireActive(employeeId);
        memberManagement.deactivateMember(memberId);
    }

    /** Searches members through the member-role integration contract. */
    public List<MemberSummary> searchMembers(String employeeId, String query) {
        librarianService.requireActive(employeeId);
        return memberManagement.searchMembers(query);
    }

    /** Adds a book through the member-role catalogue contract. */
    public String addBook(String employeeId, BookDetails details) {
        librarianService.requireActive(employeeId);
        return bookManagement.addBook(details);
    }

    /** Edits a book through the member-role catalogue contract. */
    public void editBook(String employeeId, String bookId, BookDetails details) {
        librarianService.requireActive(employeeId);
        bookManagement.editBook(bookId, details);
    }

    /** Removes a book through the member-role catalogue contract. */
    public void removeBook(String employeeId, String bookId) {
        librarianService.requireActive(employeeId);
        bookManagement.removeBook(bookId);
    }

    /** Searches books through the member-role catalogue contract. */
    public List<BookSummary> searchBooks(String employeeId, String query) {
        librarianService.requireActive(employeeId);
        return bookManagement.searchBooks(query);
    }

    /** Records a damaged book copy through the member-role contract. */
    public void recordDamagedBook(String employeeId, String copyId) {
        librarianService.requireActive(employeeId);
        bookCopyManagement.recordDamagedBook(copyId);
    }

    /** Records a lost book copy through the member-role contract. */
    public void recordLostBook(String employeeId, String copyId) {
        librarianService.requireActive(employeeId);
        bookCopyManagement.recordLostBook(copyId);
    }

    /** Edits a fine after checking librarian authorization. */
    public Fine editFine(String employeeId, String fineId, BigDecimal amount) {
        librarianService.requireActive(employeeId);
        return fineService.editFine(fineId, amount);
    }

    /** Removes a fine after checking librarian authorization. */
    public boolean removeFine(String employeeId, String fineId) {
        librarianService.requireActive(employeeId);
        return fineService.removeFine(fineId);
    }

    /** Sends an overdue alert after checking librarian authorization. */
    public Notification sendOverdueAlert(String employeeId, String loanId) {
        librarianService.requireActive(employeeId);
        LoanSummary loan = loanQuery.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan does not exist"));
        Notification notification = notificationService.sendOverdueAlert(loan);
        view.showMessage("Overdue alert sent");
        return notification;
    }

    /** Sends a reservation reminder after checking librarian authorization. */
    public Notification sendReservationReminder(String employeeId, String reservationId) {
        librarianService.requireActive(employeeId);
        Reservation reservation = reservationService.getReservation(reservationId);
        if (!reservation.isPendingOn(LocalDate.now(clock))) {
            throw new IllegalArgumentException("Pending reservation does not exist");
        }
        Notification notification = notificationService.sendReservationReminder(reservation);
        view.showMessage("Reservation reminder sent");
        return notification;
    }

    /** Displays a controller error without exposing storage details to the view. */
    public void showError(Exception exception) {
        view.showError(Objects.requireNonNull(exception, "exception").getMessage());
    }
}
