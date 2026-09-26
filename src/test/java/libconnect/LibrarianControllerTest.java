package libconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        return new LibrarianController(new LibrarianService(librarians), reservations, fines,
                new NotificationService(new ServiceTestDoubles.Notifications(), CLOCK), loans,
                new NoOpView(), CLOCK, members, new NoOpBookManagement(), new NoOpBookCopyManagement());
    }

    private static final class RecordingMemberManagement implements MemberManagement {
        private String lastQuery;

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
        }

        /** Records an edit call. */
        @Override
        public void editMember(String memberId, String name, String email) {
        }

        /** Records a deactivation call. */
        @Override
        public void deactivateMember(String memberId) {
        }

        /** Returns one member and records the query. */
        @Override
        public List<MemberSummary> searchMembers(String query) {
            lastQuery = query;
            return List.of(new MemberSummary("m1", "Ada", "ada@example.com", true));
        }
    }

    private static final class NoOpBookManagement implements BookManagement {
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

    private static final class NoOpBookCopyManagement implements BookCopyManagement {
        /** Does nothing in the controller setup. */
        @Override
        public void recordDamagedBook(String copyId) {
        }

        /** Does nothing in the controller setup. */
        @Override
        public void recordLostBook(String copyId) {
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
