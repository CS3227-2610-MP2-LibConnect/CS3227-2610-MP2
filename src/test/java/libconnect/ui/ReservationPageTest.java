package libconnect.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;

import libconnect.integration.BookCatalogue;
import libconnect.integration.MemberDirectory;
import libconnect.models.Book;
import libconnect.models.Member;
import libconnect.models.Reservation;
import libconnect.models.ReservationStatus;
import libconnect.services.AuthenticationService;
import libconnect.services.BookCopyService;
import libconnect.services.BorrowService;
import libconnect.services.LoanService;
import libconnect.services.MemberService;
import libconnect.services.ReservationService;
import libconnect.storage.repositories.ReservationRepository;
import libconnect.ui.pages.ReservationPage;

/** Tests member reservation creation and cancellation through the JavaFX page. */
class ReservationPageTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-21T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void createReservationByIsbn_persistsReservationAndShowsIt() {
        UiTestSupport.runOnFxThread(() -> {
            TestContext context = createContext();
            try {
                Book book = new Book("978-1", "First", "Author", "Publisher", "Category", 2020);
                context.bookService().books.add(book);
                InMemoryReservationRepository repository = new InMemoryReservationRepository();
                ReservationPage page = createPage(context, repository, book);

                TextField isbnField = UiTestSupport.findTextFields(page).get(0);
                isbnField.setText(book.getIsbn());
                UiTestSupport.findButton(page, "Create reservation").fire();

                assertEquals(1, repository.findAll().size());
                assertEquals("MEM-1", repository.findAll().get(0).getMemberId());
                assertEquals(book.getIsbn(), repository.findAll().get(0).getBookId());
                assertTrue(UiTestSupport.labelTexts(page).contains(book.getIsbn()));
            } finally {
                context.close();
            }
        });
    }

    @Test
    void cancelReservation_marksSelectedReservationCancelled() {
        UiTestSupport.runOnFxThread(() -> {
            TestContext context = createContext();
            try {
                Book book = new Book("978-1", "First", "Author", "Publisher", "Category", 2020);
                context.bookService().books.add(book);
                InMemoryReservationRepository repository = new InMemoryReservationRepository();
                ReservationService service = createService(repository, book);
                Reservation reservation = service.reserveBook("MEM-1", book.getIsbn());
                ReservationPage page = new ReservationPage(service, context.bookService(),
                        context.sessionManager(), context.navigator());

                Button cancelButton = UiTestSupport.findButton(page, "Cancel reservation");
                cancelButton.fire();

                assertEquals(ReservationStatus.CANCELLED,
                        repository.findById(reservation.getId()).orElseThrow().getStatus());
                assertTrue(UiTestSupport.labelTexts(page).contains("CANCELLED"));
                assertTrue(UiTestSupport.findButtons(page).stream()
                        .noneMatch(button -> "Cancel reservation".equals(button.getText())));
            } finally {
                context.close();
            }
        });
    }

    private static ReservationPage createPage(TestContext context,
                                              InMemoryReservationRepository repository, Book book) {
        return new ReservationPage(createService(repository, book), context.bookService(),
                context.sessionManager(), context.navigator());
    }

    private static TestContext createContext() {
        Stage stage = new Stage();
        SessionManager sessionManager = new SessionManager();
        sessionManager.login(new Member("USER-1", "Alex Member", "alex@example.com", "hash", "MEM-1"));
        TestBookService bookService = new TestBookService();
        SceneNavigator navigator = new SceneNavigator(stage, new AuthenticationService(), sessionManager,
                bookService, new BookCopyService(), new BorrowService(), new LoanService(), new MemberService());
        return new TestContext(stage, sessionManager, bookService, navigator);
    }

    private static ReservationService createService(InMemoryReservationRepository repository, Book book) {
        MemberDirectory members = new MemberDirectory() {
            @Override
            public boolean exists(String memberId) {
                return "MEM-1".equals(memberId);
            }

            @Override
            public boolean isActive(String memberId) {
                return "MEM-1".equals(memberId);
            }
        };
        BookCatalogue books = new BookCatalogue() {
            @Override
            public boolean exists(String bookId) {
                return book.getIsbn().equals(bookId);
            }

            @Override
            public boolean isAvailable(String bookId) {
                return false;
            }
        };
        return new ReservationService(repository, members, books, CLOCK, Period.ofDays(7));
    }

    private static final class InMemoryReservationRepository implements ReservationRepository {
        private final Map<String, Reservation> reservations = new HashMap<>();

        @Override
        public Optional<Reservation> findById(String id) {
            return Optional.ofNullable(reservations.get(id));
        }

        @Override
        public boolean deleteById(String id) {
            return reservations.remove(id) != null;
        }

        @Override
        public void save(Reservation reservation) {
            reservations.put(reservation.getId(), reservation);
        }

        @Override
        public List<Reservation> findAll() {
            return new ArrayList<>(reservations.values());
        }

        @Override
        public List<Reservation> findByMemberId(String memberId) {
            return reservations.values().stream()
                    .filter(reservation -> reservation.getMemberId().equals(memberId))
                    .toList();
        }

        @Override
        public List<Reservation> findByBookId(String bookId) {
            return reservations.values().stream()
                    .filter(reservation -> reservation.getBookId().equals(bookId))
                    .toList();
        }

        @Override
        public List<Reservation> findByStatus(ReservationStatus status) {
            return reservations.values().stream()
                    .filter(reservation -> reservation.getStatus() == status)
                    .toList();
        }
    }

    private static final class TestBookService extends libconnect.services.BookService {
        private final List<Book> books = new ArrayList<>();

        @Override
        public Optional<Book> getBookByIsbn(String isbn) {
            return books.stream().filter(book -> book.getIsbn().equals(isbn)).findFirst();
        }
    }

    private record TestContext(Stage stage, SessionManager sessionManager,
                               TestBookService bookService, SceneNavigator navigator) {
        private void close() {
            stage.close();
        }
    }
}
