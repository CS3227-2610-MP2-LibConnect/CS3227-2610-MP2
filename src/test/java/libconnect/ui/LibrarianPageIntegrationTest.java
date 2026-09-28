package libconnect.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import libconnect.models.AccountStatus;
import libconnect.models.Book;
import libconnect.models.BookCopy;
import libconnect.models.CopyStatus;
import libconnect.models.Fine;
import libconnect.models.FineStatus;
import libconnect.models.Librarian;
import libconnect.models.Loan;
import libconnect.models.Notification;
import libconnect.models.NotificationType;
import libconnect.models.Reservation;
import libconnect.models.ReservationStatus;
import libconnect.storage.StorageManager;
import libconnect.storage.file.FileBookCopyRepository;
import libconnect.storage.file.FileBookRepository;
import libconnect.storage.file.FileFineRepository;
import libconnect.storage.file.FileLibrarianRepository;
import libconnect.storage.file.FileLoanRepository;
import libconnect.storage.file.FileMemberRepository;
import libconnect.storage.file.FileNotificationRepository;
import libconnect.storage.file.FileReservationRepository;
import libconnect.ui.components.JavaFxLibrarianView;
import libconnect.ui.pages.LibrarianPage;

/** Exercises the librarian page through JavaFX, services, repositories, and the composition root. */
class LibrarianPageIntegrationTest {
    private static final String EMPLOYEE_ID = "LIB-1";
    private static final String BOOK_ID = "ISBN-1";
    private static final String MEMBER_ID = "MEM-1";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-21T00:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path temporaryDirectory;

    @Test
    void activeLibrarian_constructsWorkspaceWithAllTabs() {
        UiTestSupport.runOnFxThread(() -> {
            TestContext context = createContext();
            try {
                TabPane navigation = navigation(context.page());

                assertEquals("librarian-page", context.page().getId());
                assertEquals(List.of("Dashboard", "Members", "Books", "BookCopy", "Loans",
                        "Reservations", "Fines", "Notifications"),
                        navigation.getTabs().stream().map(Tab::getText).toList());
                assertTrue(UiTestSupport.labelTexts(context.page()).contains("Signed in: Ada Librarian"));
            } finally {
                context.close();
            }
        });
    }

    @Test
    void inactiveOrUnknownLibrarian_cannotConstructWorkspace() {
        UiTestSupport.runOnFxThread(() -> {
            TestContext context = createContext();
            try {
                assertThrows(IllegalStateException.class,
                        () -> new LibrarianPage(context.runtime(), new JavaFxLibrarianView(), "LIB-2",
                                () -> { }));
            } finally {
                context.close();
            }
        });
    }

    @Test
    void logoutButton_invokesNavigationCallback() {
        UiTestSupport.runOnFxThread(() -> {
            AtomicBoolean loggedOut = new AtomicBoolean();
            TestContext context = createContext(loggedOut);
            try {
                UiTestSupport.findButton(context.page(), "Log out").fire();

                assertTrue(loggedOut.get());
            } finally {
                context.close();
            }
        });
    }

    @Test
    void dashboardRefresh_displaysCountsFromComposedServices() {
        UiTestSupport.runOnFxThread(() -> {
            TestContext context = createContext();
            try {
                UiTestSupport.findButton(context.page(), "Refresh summary").fire();

                assertTrue(UiTestSupport.labelTexts(context.page()).contains(
                        "Active loans: 1 | Overdue: 1 | Reservations: 1 | Fines: 1"));
            } finally {
                context.close();
            }
        });
    }

    @Test
    void memberSearchAndLifecycleActions_updateFileBackedMembers() {
        UiTestSupport.runOnFxThread(() -> {
            TestContext context = createContext();
            try {
                selectTab(context.page(), "Members");
                TableView<?> members = firstTable(context.page(), "Members");
                TextField query = findTextFields(context.page(), "Members").get(0);
                query.setText("MEM-1");
                findButton(context.page(), "Members", "Search").fire();
                assertEquals(1, members.getItems().size());

                members.getSelectionModel().selectFirst();
                acceptTextFields("Updated Member", "updated@example.com");
                findButton(context.page(), "Members", "Edit selected").fire();
                assertEquals("Updated Member", context.members().findByMembershipId(MEMBER_ID)
                        .orElseThrow().getName());

                members.getSelectionModel().selectFirst();
                acceptPasswordFields("new-password", "new-password");
                findButton(context.page(), "Members", "Reset password").fire();

                members.getSelectionModel().selectFirst();
                findButton(context.page(), "Members", "Deactivate selected").fire();
                assertFalse(context.members().findByMembershipId(MEMBER_ID).orElseThrow().isActive());

                members.getSelectionModel().selectFirst();
                findButton(context.page(), "Members", "Activate selected").fire();
                assertTrue(context.members().findByMembershipId(MEMBER_ID).orElseThrow().isActive());
            } finally {
                context.close();
            }
        });
    }

    @Test
    void memberRegistration_addsMemberThroughDialogAndRepository() {
        UiTestSupport.runOnFxThread(() -> {
            TestContext context = createContext();
            try {
                selectTab(context.page(), "Members");
                acceptRegistrationFields("New Member", "new@example.com", "password", "password");
                findButton(context.page(), "Members", "Register").fire();

                assertEquals(3, context.members().findAll().size());
                assertTrue(context.members().findAll().stream()
                        .anyMatch(member -> member.getEmail().equals("new@example.com")));
            } finally {
                context.close();
            }
        });
    }

    @Test
    void bookSearchAndLifecycleActions_updateCatalogueAndCopies() {
        UiTestSupport.runOnFxThread(() -> {
            TestContext context = createContext();
            try {
                selectTab(context.page(), "Books");
                TableView<?> books = firstTable(context.page(), "Books");
                TextField query = findTextFields(context.page(), "Books").get(0);
                query.setText("first book");
                findButton(context.page(), "Books", "Search").fire();
                assertEquals(1, books.getItems().size());

                books.getSelectionModel().selectFirst();
                acceptTextFields(BOOK_ID, "Updated title", "Author", "Publisher", "Category", "2021");
                findButton(context.page(), "Books", "Edit selected").fire();
                assertEquals("Updated title", context.books().findByIsbn(BOOK_ID).orElseThrow().getTitle());

                query.clear();
                acceptTextFields("ISBN-2", "Second book", "Author", "Publisher", "Category", "2022");
                findButton(context.page(), "Books", "Add").fire();
                assertTrue(context.books().findByIsbn("ISBN-2").isPresent());
                assertEquals(2, books.getItems().size());

                findButton(context.page(), "Books", "Search").fire();
                books.getSelectionModel().selectFirst();
                acceptButton(ButtonType.OK);
                findButton(context.page(), "Books", "Remove selected").fire();
                assertFalse(context.books().findByIsbn(BOOK_ID).isPresent());
                assertTrue(context.copies().findByIsbn(BOOK_ID).isEmpty());
            } finally {
                context.close();
            }
        });
    }

    @Test
    void bookCopyActions_updateStatusAndShelfLocation() {
        UiTestSupport.runOnFxThread(() -> {
            TestContext context = createContext();
            try {
                selectTab(context.page(), "BookCopy");
                TableView<?> copies = firstTable(context.page(), "BookCopy");
                copies.getSelectionModel().selectFirst();
                findButton(context.page(), "BookCopy", "Mark damaged").fire();
                assertEquals(CopyStatus.DAMAGED, context.copies().findById("COPY-1").orElseThrow().getStatus());

                copies.getSelectionModel().selectFirst();
                acceptTextFields("A-2");
                findButton(context.page(), "BookCopy", "Edit selected").fire();
                assertEquals("A-2", context.copies().findById("COPY-1").orElseThrow().getShelfLocation());

                copies.getSelectionModel().selectFirst();
                findButton(context.page(), "BookCopy", "Mark available").fire();
                assertEquals(CopyStatus.AVAILABLE, context.copies().findById("COPY-1").orElseThrow().getStatus());
            } finally {
                context.close();
            }
        });
    }

    @Test
    void loanFiltersAndOverdueAlert_updateNotificationStorage() {
        UiTestSupport.runOnFxThread(() -> {
            TestContext context = createContext();
            try {
                selectTab(context.page(), "Loans");
                TableView<?> loans = firstTable(context.page(), "Loans");
                findButton(context.page(), "Loans", "Overdue").fire();
                assertEquals(1, loans.getItems().size());

                findButton(context.page(), "Loans", "Active").fire();
                assertEquals(0, loans.getItems().size());
                findButton(context.page(), "Loans", "Overdue").fire();

                loans.getSelectionModel().selectFirst();
                findButton(context.page(), "Loans", "Alert selected member").fire();
                assertTrue(context.notifications().findAll().stream()
                        .anyMatch(notification -> notification.getType() == NotificationType.OVERDUE_ALERT
                                && notification.getReferenceId().equals("LOAN-1")));
            } finally {
                context.close();
            }
        });
    }

    @Test
    void reservationActions_createCancelFulfilAndRemind() {
        UiTestSupport.runOnFxThread(() -> {
            TestContext context = createContext();
            try {
                selectTab(context.page(), "Reservations");
                List<TextField> fields = findTextFields(context.page(), "Reservations");
                fields.get(0).setText("MEM-2");
                fields.get(1).setText(BOOK_ID);
                findButton(context.page(), "Reservations", "Create").fire();
                assertEquals(2, context.reservations().findAll().size());

                fields.get(0).clear();
                findButton(context.page(), "Reservations", "List").fire();
                TableView<Reservation> reservations = table(context.page(), "Reservations");
                reservations.getSelectionModel().selectFirst();
                String cancelledReservationId = reservations.getSelectionModel().getSelectedItem().getId();
                findButton(context.page(), "Reservations", "Send reminder").fire();
                findButton(context.page(), "Reservations", "Cancel selected").fire();
                assertEquals(ReservationStatus.CANCELLED, context.reservations()
                        .findById(cancelledReservationId).orElseThrow().getStatus());

                reservations.getItems().stream()
                        .filter(reservation -> reservation.getStatus() == ReservationStatus.PENDING)
                        .findFirst()
                        .ifPresent(reservation -> reservations.getSelectionModel().select(reservation));
                findButton(context.page(), "Reservations", "Fulfil selected").fire();
                assertTrue(context.reservations().findAll().stream()
                        .anyMatch(reservation -> reservation.getStatus() == ReservationStatus.FULFILLED));
            } finally {
                context.close();
            }
        });
    }

    @Test
    void fineActions_listCreateEditAndRemovePersistChanges() {
        UiTestSupport.runOnFxThread(() -> {
            TestContext context = createContext();
            try {
                selectTab(context.page(), "Fines");
                findButton(context.page(), "Fines", "List all fines").fire();
                TableView<?> fines = firstTable(context.page(), "Fines");
                fines.getSelectionModel().selectFirst();
                acceptTextFields("9.99");
                findButton(context.page(), "Fines", "Edit selected").fire();
                assertEquals(new BigDecimal("9.99"), context.fines().findById("FINE-1").orElseThrow().getAmount());

                fines.getSelectionModel().selectFirst();
                findButton(context.page(), "Fines", "Remove selected").fire();
                assertTrue(context.fines().findById("FINE-1").isEmpty());

                TextField loanId = findTextFields(context.page(), "Fines").get(1);
                loanId.setText("LOAN-1");
                findButton(context.page(), "Fines", "Create from loan").fire();
                assertEquals(1, context.fines().findAll().size());
                assertEquals(FineStatus.OUTSTANDING, context.fines().findAll().get(0).getStatus());
            } finally {
                context.close();
            }
        });
    }

    @Test
    void notificationActions_listAndMarkReadPersistReadState() {
        UiTestSupport.runOnFxThread(() -> {
            TestContext context = createContext();
            try {
                selectTab(context.page(), "Notifications");
                TextField recipient = findTextFields(context.page(), "Notifications").get(0);
                recipient.setText(MEMBER_ID);
                findButton(context.page(), "Notifications", "List").fire();
                TableView<?> notifications = firstTable(context.page(), "Notifications");
                assertEquals(1, notifications.getItems().size());
                notifications.getSelectionModel().selectFirst();
                findButton(context.page(), "Notifications", "Mark selected read").fire();

                assertTrue(context.notifications().findById("NOTIF-1").orElseThrow().isRead());
            } finally {
                context.close();
            }
        });
    }

    @Test
    void invalidPageAction_displaysUserFacingError() {
        UiTestSupport.runOnFxThread(() -> {
            TestContext context = createContext();
            try {
                selectTab(context.page(), "Notifications");
                findButton(context.page(), "Notifications", "List").fire();

                assertTrue(UiTestSupport.labelTexts(context.page()).contains("userId must not be blank"));
            } finally {
                context.close();
            }
        });
    }

    private TestContext createContext() {
        return createContext(new AtomicBoolean());
    }

    private TestContext createContext(AtomicBoolean loggedOut) {
        seedData(temporaryDirectory);
        JavaFxLibrarianView view = new JavaFxLibrarianView();
        LibrarianRuntime runtime = LibrarianCompositionRoot.create(temporaryDirectory, CLOCK, view);
        Stage stage = new Stage();
        LibrarianPage page = new LibrarianPage(runtime, view, EMPLOYEE_ID, () -> loggedOut.set(true));
        stage.setScene(new Scene(page));
        stage.show();
        StorageManager storageManager = new StorageManager(temporaryDirectory);
        return new TestContext(stage, page,
                new FileMemberRepository(storageManager, temporaryDirectory.resolve("members.json")),
                new FileBookRepository(storageManager, temporaryDirectory.resolve("books.json")),
                new FileBookCopyRepository(storageManager, temporaryDirectory.resolve("book-copies.json")),
                new FileLoanRepository(storageManager, temporaryDirectory.resolve("loans.json")),
                new FileReservationRepository(storageManager, temporaryDirectory.resolve("reservations.json")),
                new FileFineRepository(storageManager, temporaryDirectory.resolve("fines.json")),
                new FileNotificationRepository(storageManager, temporaryDirectory.resolve("notifications.json")),
                runtime);
    }

    private static void seedData(Path dataDirectory) {
        StorageManager storageManager = new StorageManager(dataDirectory);
        FileLibrarianRepository librarians = new FileLibrarianRepository(storageManager,
                dataDirectory.resolve("librarians.json"));
        FileMemberRepository members = new FileMemberRepository(storageManager,
                dataDirectory.resolve("members.json"));
        FileBookRepository books = new FileBookRepository(storageManager, dataDirectory.resolve("books.json"));
        FileBookCopyRepository copies = new FileBookCopyRepository(storageManager,
                dataDirectory.resolve("book-copies.json"));
        FileLoanRepository loans = new FileLoanRepository(storageManager, dataDirectory.resolve("loans.json"));
        FileReservationRepository reservations = new FileReservationRepository(storageManager,
                dataDirectory.resolve("reservations.json"));
        FileFineRepository fines = new FileFineRepository(storageManager, dataDirectory.resolve("fines.json"));
        FileNotificationRepository notifications = new FileNotificationRepository(storageManager,
                dataDirectory.resolve("notifications.json"));

        librarians.save(new Librarian("LIB-USER-1", EMPLOYEE_ID, "Ada Librarian", "ada@library.example",
                "password-hash", AccountStatus.ACTIVE));
        librarians.save(new Librarian("LIB-USER-2", "LIB-2", "Inactive Librarian", "inactive@library.example",
                "password-hash", AccountStatus.INACTIVE));
        members.save(new libconnect.models.Member("USER-1", "Alex Member", "alex@example.com",
                "password-hash", MEMBER_ID));
        members.save(new libconnect.models.Member("USER-2", "Second Member", "second@example.com",
                "password-hash", "MEM-2"));
        books.save(new Book(BOOK_ID, "First Book", "Author", "Publisher", "Category", 2020));
        copies.save(new BookCopy("COPY-1", BOOK_ID, CopyStatus.BORROWED, "A-1"));

        LocalDate borrowDate = LocalDate.now().minusDays(60);
        loans.save(new Loan("LOAN-1", MEMBER_ID, "COPY-1", borrowDate));
        reservations.save(new Reservation("RES-1", MEMBER_ID, BOOK_ID, LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(5), ReservationStatus.PENDING));
        fines.save(new Fine("FINE-1", "LOAN-1", MEMBER_ID, BigDecimal.valueOf(3.00), "Overdue loan",
                FineStatus.OUTSTANDING, LocalDate.now().minusDays(1), "First Book"));
        notifications.save(new Notification("NOTIF-1", MEMBER_ID, NotificationType.OVERDUE_ALERT, "LOAN-1",
                "Overdue book", java.time.LocalDateTime.now().minusHours(1), false));
    }

    private static TabPane navigation(LibrarianPage page) {
        return UiTestSupport.findNodes(page, TabPane.class).stream()
                .filter(tabPane -> "librarian-navigation".equals(tabPane.getId()))
                .findFirst()
                .orElseThrow();
    }

    private static void selectTab(LibrarianPage page, String title) {
        navigation(page).getSelectionModel().select(navigation(page).getTabs().stream()
                .filter(tab -> title.equals(tab.getText()))
                .findFirst()
                .orElseThrow());
    }

    private static TableView<?> firstTable(LibrarianPage page, String tabTitle) {
        return UiTestSupport.findNodes(tabContent(page, tabTitle), TableView.class).stream()
                .findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static <T> TableView<T> table(LibrarianPage page, String tabTitle) {
        return (TableView<T>) firstTable(page, tabTitle);
    }

    private static Button findButton(LibrarianPage page, String tabTitle, String text) {
        return UiTestSupport.findButton(tabContent(page, tabTitle), text);
    }

    private static List<TextField> findTextFields(LibrarianPage page, String tabTitle) {
        return UiTestSupport.findTextFields(tabContent(page, tabTitle));
    }

    private static Parent tabContent(LibrarianPage page, String tabTitle) {
        return navigation(page).getTabs().stream()
                .filter(tab -> tabTitle.equals(tab.getText()))
                .map(Tab::getContent)
                .filter(Parent.class::isInstance)
                .map(Parent.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private static void acceptTextFields(String... values) {
        acceptDialogFields(values, TextField.class);
    }

    private static void acceptPasswordFields(String... values) {
        acceptDialogFields(values, PasswordField.class);
    }

    private static <T extends TextInputControl> void acceptDialogFields(String[] values, Class<T> fieldType) {
        AtomicInteger index = new AtomicInteger();
        Runnable acceptNext = new Runnable() {
            @Override
            public void run() {
                DialogPane dialogPane = findDialogPane();
                List<T> fields = UiTestSupport.findNodes(dialogPane, fieldType);
                if (fieldType == PasswordField.class) {
                    assertTrue(UiTestSupport.labelTexts(dialogPane).contains("New password"));
                    assertTrue(UiTestSupport.labelTexts(dialogPane).contains("Confirm password"));
                }
                if (fields.size() == values.length) {
                    for (T field : fields) {
                        field.setText(values[index.getAndIncrement()]);
                    }
                } else {
                    fields.get(0).setText(values[index.getAndIncrement()]);
                }
                ((Button) dialogPane.lookupButton(ButtonType.OK)).fire();
                if (index.get() < values.length) {
                    Platform.runLater(this);
                }
            }
        };
        Platform.runLater(acceptNext);
    }

    private static void acceptRegistrationFields(String name, String email, String password,
                                                  String confirmation) {
        Platform.runLater(() -> {
            DialogPane dialogPane = findDialogPane();
            List<TextField> fields = UiTestSupport.findTextFields(dialogPane);
            fields.get(0).setText(name);
            fields.get(1).setText(email);
            fields.get(2).setText(password);
            fields.get(3).setText(confirmation);
            ((Button) dialogPane.lookupButton(ButtonType.OK)).fire();
        });
    }

    private static void acceptButton(ButtonType buttonType) {
        Platform.runLater(() -> ((Button) findDialogPane().lookupButton(buttonType)).fire());
    }

    private static DialogPane findDialogPane() {
        return Window.getWindows().stream()
                .filter(Window::isShowing)
                .map(Window::getScene)
                .filter(scene -> scene != null && scene.getRoot() instanceof DialogPane)
                .map(scene -> (DialogPane) scene.getRoot())
                .findFirst()
                .orElseThrow(() -> new AssertionError("No open dialog was found"));
    }

    private record TestContext(Stage stage, LibrarianPage page, FileMemberRepository members,
                               FileBookRepository books, FileBookCopyRepository copies,
                               FileLoanRepository loans, FileReservationRepository reservations,
                               FileFineRepository fines, FileNotificationRepository notifications,
                               LibrarianRuntime runtime) {
        private void close() {
            stage.close();
        }
    }
}
