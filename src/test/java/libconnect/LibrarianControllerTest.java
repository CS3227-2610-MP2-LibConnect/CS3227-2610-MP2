package libconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import libconnect.integration.BookCatalogue;
import libconnect.integration.BookDetails;
import libconnect.integration.BookManagement;
import libconnect.integration.BookSummary;
import libconnect.integration.BookCopyManagement;
import libconnect.integration.LoanQuery;
import libconnect.integration.LoanSummary;
import libconnect.integration.MemberManagement;
import libconnect.integration.MemberSummary;
import libconnect.librarian.LibrarianController;
import libconnect.librarian.LibrarianView;
import libconnect.models.AccountStatus;
import libconnect.models.Fine;
import libconnect.services.FineService;
import libconnect.services.LibrarianService;
import libconnect.services.NotificationService;
import libconnect.services.ReservationService;

/** Tests controller authorization and delegation to cross-role contracts. */
class LibrarianControllerTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-21T00:00:00Z"), ZoneOffset.UTC);

    /** Verifies that authorized member searches delegate to the member role. */
    @Test
    void searchMembers_activeLibrarian_delegatesToMemberRole() {
        ServiceTestDoubles.Librarians librarians = new ServiceTestDoubles.Librarians();
        librarians.save(new libconnect.models.Librarian("e1", "Ada", "ada@example.com",
                AccountStatus.ACTIVE));
        RecordingMemberManagement members = new RecordingMemberManagement();
        LibrarianController controller = createController(librarians, members);

        assertEquals(1, controller.searchMembers("e1", "Ada").size());
        assertEquals("Ada", members.lastQuery);
    }

    /** Verifies that inactive librarians cannot reach member-owned operations. */
    @Test
    void searchMembers_inactiveLibrarian_rejectedBeforeDelegation() {
        ServiceTestDoubles.Librarians librarians = new ServiceTestDoubles.Librarians();
        librarians.save(new libconnect.models.Librarian("e1", "Ada", "ada@example.com",
                AccountStatus.INACTIVE));
        RecordingMemberManagement members = new RecordingMemberManagement();
        LibrarianController controller = createController(librarians, members);

        assertThrows(IllegalStateException.class, () -> controller.searchMembers("e1", "Ada"));
        assertEquals(null, members.lastQuery);
    }

    @Test
    void memberOperations_activeLibrarian_delegateAllChanges() {
        ServiceTestDoubles.Librarians librarians = activeLibrarians();
        RecordingMemberManagement members = new RecordingMemberManagement();
        LibrarianController controller = createController(librarians, members);

        controller.registerMember("e1", "m1", "Ada", "ada@example.com");
        controller.editMember("e1", "m1", "Ada Updated", "updated@example.com");
        controller.deactivateMember("e1", "m1");

        assertEquals("m1", members.lastMemberId);
        assertEquals("Ada Updated", members.lastName);
        assertEquals("updated@example.com", members.lastEmail);
        assertTrue(members.registered);
        assertTrue(members.edited);
        assertTrue(members.deactivated);
    }

    @Test
    void bookAndCopyOperations_activeLibrarian_delegateAllChanges() {
        ServiceTestDoubles.Librarians librarians = activeLibrarians();
        RecordingMemberManagement members = new RecordingMemberManagement();
        RecordingBookManagement books = new RecordingBookManagement();
        RecordingBookCopyManagement copies = new RecordingBookCopyManagement();
        LibrarianController controller = createController(librarians, members, books, copies,
                new NoOpView());
        BookDetails details = new BookDetails("isbn", "Title", "Author", "Publisher", "Category", 2025);

        assertEquals("book-1", controller.addBook("e1", details));
        controller.editBook("e1", "book-1", details);
        controller.removeBook("e1", "book-1");
        controller.recordDamagedBook("e1", "copy-1");
        controller.recordLostBook("e1", "copy-2");
        assertEquals(1, controller.searchBooks("e1", "title").size());

        assertEquals("book-1", books.lastBookId);
        assertEquals("title", books.lastQuery);
        assertEquals("copy-2", copies.lastCopyId);
        assertTrue(copies.damaged);
        assertTrue(copies.lost);
    }

    @Test
    void viewOperations_activeLibrarian_delegateAndUseCurrentDate() {
        ServiceTestDoubles.Librarians librarians = activeLibrarians();
        RecordingMemberManagement members = new RecordingMemberManagement();
        LoanSummary loan = new LoanSummary("l1", "m1", "c1", LocalDate.of(2026, 9, 18));
        RecordingLoanQuery loans = new RecordingLoanQuery(loan);
        LibrarianController controller = createController(librarians, members,
                loans, new NoOpBookManagement(), new NoOpBookCopyManagement(), new NoOpView());

        assertEquals(List.of(loan), controller.viewLoans("e1"));
        assertEquals(List.of(loan), controller.viewOverdueLoans("e1"));
        assertEquals(LocalDate.of(2026, 9, 21), loans.lastOverdueDate);
    }

    @Test
    void fineOperations_activeLibrarian_delegateChanges() {
        ServiceTestDoubles.Librarians librarians = activeLibrarians();
        RecordingMemberManagement members = new RecordingMemberManagement();
        ServiceTestDoubles.Fines finesRepository = new ServiceTestDoubles.Fines();
        FineService fines = new FineService(finesRepository,
                new RecordingLoanQuery(new LoanSummary("l1", "m1", "c1", LocalDate.of(2026, 9, 18))),
                members, CLOCK, BigDecimal.ONE);
        LibrarianController controller = createController(librarians, members, fines,
                new NoOpView());
        Fine fine = new libconnect.models.Fine("f1", "l1", "m1", BigDecimal.ONE, "reason",
                libconnect.models.FineStatus.OUTSTANDING, LocalDate.of(2026, 9, 21));
        finesRepository.save(fine);

        assertEquals(BigDecimal.valueOf(2).setScale(2), controller.editFine("e1", "f1", BigDecimal.TWO)
                .getAmount());
        assertTrue(controller.removeFine("e1", "f1"));
    }

    @Test
    void alertOperations_activeLibrarian_sendMessagesAndRejectMissingLoan() {
        ServiceTestDoubles.Librarians librarians = activeLibrarians();
        RecordingMemberManagement members = new RecordingMemberManagement();
        RecordingView view = new RecordingView();
        LoanSummary loan = new LoanSummary("l1", "m1", "c1", LocalDate.of(2026, 9, 18));
        LibrarianController controller = createController(librarians, members,
                new RecordingLoanQuery(loan), new NoOpBookManagement(),
                new NoOpBookCopyManagement(), view);

        assertEquals("l1", controller.sendOverdueAlert("e1", "l1").getReferenceId());
        assertEquals("Overdue alert sent", view.lastMessage);
        assertThrows(IllegalArgumentException.class, () -> controller.sendOverdueAlert("e1", "missing"));
    }

    @Test
    void showError_forwardsExceptionMessageToView() {
        RecordingView view = new RecordingView();
        LibrarianController controller = createController(activeLibrarians(),
                new RecordingMemberManagement(), new NoOpBookManagement(),
                new NoOpBookCopyManagement(), view);

        controller.showError(new IllegalArgumentException("bad input"));

        assertEquals("bad input", view.lastError);
    }

    private static LibrarianController createController(ServiceTestDoubles.Librarians librarians,
                                                        MemberManagement members) {
        LoanQuery loans = new LoanQuery() {
            @Override
            public Optional<LoanSummary> findById(String loanId) {
                return Optional.empty();
            }

            @Override
            public List<LoanSummary> findActiveLoans() {
                return List.of();
            }

            @Override
            public List<LoanSummary> findOverdueLoans(LocalDate date) {
                return List.of();
            }
        };
        BookCatalogue books = new BookCatalogue() {
            @Override
            public boolean exists(String bookId) {
                return false;
            }

            @Override
            public boolean isAvailable(String bookId) {
                return false;
            }
        };
        ReservationService reservations = new ReservationService(new ServiceTestDoubles.Reservations(),
                members, books, CLOCK, Period.ofDays(7));
        FineService fines = new FineService(new ServiceTestDoubles.Fines(), loans, members,
                CLOCK, BigDecimal.ONE);
        return createController(librarians, members, loans, new NoOpBookManagement(),
                new NoOpBookCopyManagement(), new NoOpView(), reservations, fines,
                new NotificationService(new ServiceTestDoubles.Notifications(), CLOCK));
    }

    private static LibrarianController createController(ServiceTestDoubles.Librarians librarians,
                                                        MemberManagement members,
                                                        BookManagement books,
                                                        BookCopyManagement copies,
                                                        LibrarianView view) {
        LoanQuery loans = new RecordingLoanQuery();
        return createController(librarians, members, loans, books, copies, view);
    }

    private static LibrarianController createController(ServiceTestDoubles.Librarians librarians,
                                                        MemberManagement members,
                                                        LoanQuery loans, BookManagement books,
                                                        BookCopyManagement copies, LibrarianView view) {
        ReservationService reservations = new ReservationService(new ServiceTestDoubles.Reservations(),
                members, books, CLOCK, Period.ofDays(7));
        FineService fines = new FineService(new ServiceTestDoubles.Fines(), loans, members,
                CLOCK, BigDecimal.ONE);
        return createController(librarians, members, loans, books, copies, view, reservations,
                fines, new NotificationService(new ServiceTestDoubles.Notifications(), CLOCK));
    }

    private static LibrarianController createController(ServiceTestDoubles.Librarians librarians,
                                                        MemberManagement members, FineService fines,
                                                        LibrarianView view) {
        BookManagement books = new NoOpBookManagement();
        ReservationService reservations = new ReservationService(new ServiceTestDoubles.Reservations(),
                members, books, CLOCK, Period.ofDays(7));
        return createController(librarians, members, new RecordingLoanQuery(), books,
                new NoOpBookCopyManagement(), view, reservations, fines,
                new NotificationService(new ServiceTestDoubles.Notifications(), CLOCK));
    }

    private static LibrarianController createController(ServiceTestDoubles.Librarians librarians,
                                                        MemberManagement members, LoanQuery loans,
                                                        BookManagement books, BookCopyManagement copies,
                                                        LibrarianView view, ReservationService reservations,
                                                        FineService fines, NotificationService notifications) {
        return new LibrarianController(new LibrarianService(librarians), reservations, fines,
                notifications, loans, view, CLOCK, members, books, copies);
    }

    private static ServiceTestDoubles.Librarians activeLibrarians() {
        ServiceTestDoubles.Librarians librarians = new ServiceTestDoubles.Librarians();
        librarians.save(new libconnect.models.Librarian("e1", "Ada", "ada@example.com",
                AccountStatus.ACTIVE));
        return librarians;
    }

    private static final class RecordingMemberManagement implements MemberManagement {
        private String lastQuery;
        private String lastMemberId;
        private String lastName;
        private String lastEmail;
        private boolean registered;
        private boolean edited;
        private boolean deactivated;

        /** Checks member existence for the controller setup. */
        @Override
        public boolean exists(String memberId) {
            return true;
        }

        /** Checks member activity for the controller setup. */
        @Override
        public boolean isActive(String memberId) {
            return true;
        }

        /** Records a registration call. */
        @Override
        public void registerMember(String memberId, String name, String email) {
            lastMemberId = memberId;
            lastName = name;
            lastEmail = email;
            registered = true;
        }

        /** Records an edit call. */
        @Override
        public void editMember(String memberId, String name, String email) {
            lastMemberId = memberId;
            lastName = name;
            lastEmail = email;
            edited = true;
        }

        /** Records a deactivation call. */
        @Override
        public void deactivateMember(String memberId) {
            lastMemberId = memberId;
            deactivated = true;
        }

        /** Returns one member and records the query. */
        @Override
        public List<MemberSummary> searchMembers(String query) {
            lastQuery = query;
            return List.of(new MemberSummary("m1", "Ada", "ada@example.com", true));
        }
    }

    private static class NoOpBookManagement implements BookManagement {
        /** Returns no books for the controller setup. */
        @Override
        public List<BookSummary> searchBooks(String query) {
            return List.of();
        }

        /** Reports that no configured book exists. */
        @Override
        public boolean exists(String bookId) {
            return false;
        }

        /** Reports that no configured book is available. */
        @Override
        public boolean isAvailable(String bookId) {
            return false;
        }

        /** Returns a placeholder identifier. */
        @Override
        public String addBook(BookDetails details) {
            return "book";
        }

        /** Does nothing in the controller setup. */
        @Override
        public void editBook(String bookId, BookDetails details) {
        }

        /** Does nothing in the controller setup. */
        @Override
        public void removeBook(String bookId) {
        }
    }

    private static final class RecordingBookManagement extends NoOpBookManagement {
        private String lastBookId;
        private String lastQuery;

        @Override
        public String addBook(BookDetails details) {
            lastBookId = "book-1";
            return lastBookId;
        }

        @Override
        public void editBook(String bookId, BookDetails details) {
            lastBookId = bookId;
        }

        @Override
        public void removeBook(String bookId) {
            lastBookId = bookId;
        }

        @Override
        public List<BookSummary> searchBooks(String query) {
            lastQuery = query;
            return List.of(new BookSummary("book-1",
                    new BookDetails("isbn", "Title", "Author", "Publisher", "Category", 2025), 1));
        }
    }

    private static class NoOpBookCopyManagement implements BookCopyManagement {
        /** Does nothing in the controller setup. */
        @Override
        public void recordDamagedBook(String copyId) {
        }

        /** Does nothing in the controller setup. */
        @Override
        public void recordLostBook(String copyId) {
        }
    }

    private static final class RecordingBookCopyManagement extends NoOpBookCopyManagement {
        private String lastCopyId;
        private boolean damaged;
        private boolean lost;

        @Override
        public void recordDamagedBook(String copyId) {
            lastCopyId = copyId;
            damaged = true;
        }

        @Override
        public void recordLostBook(String copyId) {
            lastCopyId = copyId;
            lost = true;
        }
    }

    private static class RecordingLoanQuery implements LoanQuery {
        private final LoanSummary loan;
        private LocalDate lastOverdueDate;

        private RecordingLoanQuery() {
            this.loan = null;
        }

        private RecordingLoanQuery(LoanSummary loan) {
            this.loan = loan;
        }

        @Override
        public Optional<LoanSummary> findById(String loanId) {
            return loan != null && loan.getLoanId().equals(loanId) ? Optional.of(loan) : Optional.empty();
        }

        @Override
        public List<LoanSummary> findActiveLoans() {
            return loan == null ? List.of() : List.of(loan);
        }

        @Override
        public List<LoanSummary> findOverdueLoans(LocalDate date) {
            lastOverdueDate = date;
            return loan == null ? List.of() : List.of(loan);
        }
    }

    private static final class RecordingView implements LibrarianView {
        private String lastMessage;
        private String lastError;

        @Override
        public void showMessage(String message) {
            lastMessage = message;
        }

        @Override
        public void showError(String message) {
            lastError = message;
        }
    }

    private static final class NoOpView implements LibrarianView {
        /** Does nothing in the controller setup. */
        @Override
        public void showMessage(String message) {
        }

        /** Does nothing in the controller setup. */
        @Override
        public void showError(String message) {
        }
    }
}
