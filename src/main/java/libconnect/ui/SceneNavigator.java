package libconnect.ui;

import java.util.Objects;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import libconnect.models.Librarian;
import libconnect.services.AuthenticationService;
import libconnect.services.BookCopyService;
import libconnect.services.BookService;
import libconnect.services.BorrowService;
import libconnect.services.FineService;
import libconnect.services.LoanService;
import libconnect.services.MemberService;
import libconnect.services.NotificationService;
import libconnect.services.ReservationService;
import libconnect.ui.components.JavaFxLibrarianView;
import libconnect.ui.pages.BookInfoPage;
import libconnect.ui.pages.BorrowPage;
import libconnect.ui.pages.DashboardPage;
import libconnect.ui.pages.LoginPage;
import libconnect.ui.pages.LibrarianPage;
import libconnect.ui.pages.NotificationsPage;
import libconnect.ui.pages.ProfilePage;
import libconnect.ui.pages.RegistrationPage;
import libconnect.ui.pages.ReservationPage;

/** Coordinates page transitions within the LibConnect application window. */
public final class SceneNavigator {
    private static final double WINDOW_WIDTH = 900;
    private static final double WINDOW_HEIGHT = 600;
    private static final String APPLICATION_TITLE = "LibConnect";

    private final Stage stage;
    private final AuthenticationService authenticationService;
    private final BookService bookService;
    private final BookCopyService bookCopyService;
    private final BorrowService borrowService;
    private final FineService fineService;
    private final LoanService loanService;
    private final MemberService memberService;
    private final NotificationService notificationService;
    private final SessionManager sessionManager;
    private final ReservationService reservationService;
    private final Scene scene;
    private LibrarianRuntime librarianRuntime;
    private JavaFxLibrarianView librarianView;

    /**
     * Creates a navigator for the supplied application window and dependencies.
     *
     * @param stage the application window used for navigation.
     * @param authenticationService the service used by the login page.
     * @param sessionManager the session shared by authenticated pages.
     */
    public SceneNavigator(Stage stage, AuthenticationService authenticationService,
                          SessionManager sessionManager) {
        this(stage, authenticationService, sessionManager, new BookService(),
                new BookCopyService(), new BorrowService(), new LoanService());
    }

    /**
     * Creates a navigator with explicit authentication, catalogue, and session dependencies.
     *
     * @param stage the application window used for navigation.
     * @param authenticationService the service used by the login page.
     * @param sessionManager the session shared by authenticated pages.
     * @param bookService the service used by the dashboard catalogue.
     */
    public SceneNavigator(Stage stage, AuthenticationService authenticationService,
                          SessionManager sessionManager, BookService bookService) {
        this(stage, authenticationService, sessionManager, bookService,
                new BookCopyService(), new BorrowService(), new LoanService());
    }

    /**
     * Creates a navigator with explicit authentication, catalogue, copy, and session dependencies.
     *
     * @param stage the application window used for navigation.
     * @param authenticationService the service used by the login page.
     * @param sessionManager the session shared by authenticated pages.
     * @param bookService the service used by catalogue pages.
     * @param bookCopyService the service used to load physical book copies.
     */
    public SceneNavigator(Stage stage, AuthenticationService authenticationService,
                          SessionManager sessionManager, BookService bookService,
                          BookCopyService bookCopyService) {
        this(stage, authenticationService, sessionManager, bookService, bookCopyService,
                new BorrowService(), new LoanService());
    }

    /**
     * Creates a navigator with explicit services for authenticated pages.
     *
     * @param stage the application window used for navigation.
     * @param authenticationService the service used by the login page.
     * @param sessionManager the session shared by authenticated pages.
     * @param bookService the service used by catalogue pages.
     * @param bookCopyService the service used to load physical book copies.
     * @param borrowService the service used to complete borrowing transactions.
     */
    public SceneNavigator(Stage stage, AuthenticationService authenticationService,
                          SessionManager sessionManager, BookService bookService,
                          BookCopyService bookCopyService, BorrowService borrowService) {
        this(stage, authenticationService, sessionManager, bookService, bookCopyService,
                borrowService, new LoanService());
    }

    /**
     * Creates a navigator with explicit services for authenticated pages.
     *
     * @param stage the application window used for navigation.
     * @param authenticationService the service used by the login page.
     * @param sessionManager the session shared by authenticated pages.
     * @param bookService the service used by catalogue pages.
     * @param bookCopyService the service used to load physical book copies.
     * @param borrowService the service used to complete borrowing and return transactions.
     * @param loanService the service used to load and renew loans.
     */
    public SceneNavigator(Stage stage, AuthenticationService authenticationService,
                          SessionManager sessionManager, BookService bookService,
                          BookCopyService bookCopyService, BorrowService borrowService,
                          LoanService loanService) {
        this(stage, authenticationService, sessionManager, bookService, bookCopyService,
                borrowService, loanService, new MemberService());
    }

    /**
     * Creates a navigator with explicit services for all application pages.
     *
     * @param stage the application window used for navigation.
     * @param authenticationService the service used by the login page.
     * @param sessionManager the session shared by authenticated pages.
     * @param bookService the service used by catalogue pages.
     * @param bookCopyService the service used to load physical book copies.
     * @param borrowService the service used to complete borrowing and return transactions.
     * @param loanService the service used to load and renew loans.
     * @param memberService the service used to update member profiles and passwords.
     */
    public SceneNavigator(Stage stage, AuthenticationService authenticationService,
                          SessionManager sessionManager, BookService bookService,
                          BookCopyService bookCopyService, BorrowService borrowService,
                          LoanService loanService, MemberService memberService) {
        this(stage, authenticationService, sessionManager, bookService, bookCopyService,
                borrowService, loanService, memberService, null);
    }

    /**
     * Creates a navigator with explicit services for member pages, including notifications.
     *
     * @param stage the application window used for navigation.
     * @param authenticationService the service used by the login page.
     * @param sessionManager the session shared by authenticated pages.
     * @param bookService the service used by catalogue pages.
     * @param bookCopyService the service used to load physical book copies.
     * @param borrowService the service used to complete borrowing and return transactions.
     * @param loanService the service used to load and renew loans.
     * @param memberService the service used to update member profiles and passwords.
     * @param notificationService the service used to load and mark member notifications.
     */
    public SceneNavigator(Stage stage, AuthenticationService authenticationService,
                          SessionManager sessionManager, BookService bookService,
                          BookCopyService bookCopyService, BorrowService borrowService,
                          LoanService loanService, MemberService memberService,
                          NotificationService notificationService) {
        this(stage, authenticationService, sessionManager, bookService, bookCopyService,
                borrowService, loanService, memberService, notificationService, null);
    }

    /**
     * Creates a navigator with explicit services for member pages, including fines.
     *
     * @param stage the application window used for navigation.
     * @param authenticationService the service used by the login page.
     * @param sessionManager the session shared by authenticated pages.
     * @param bookService the service used by catalogue pages.
     * @param bookCopyService the service used to load physical book copies.
     * @param borrowService the service used to complete borrowing and return transactions.
     * @param loanService the service used to load and renew loans.
     * @param memberService the service used to update member profiles and passwords.
     * @param notificationService the service used to load and mark member notifications.
     * @param fineService the service used to load and pay member fines.
     */
    public SceneNavigator(Stage stage, AuthenticationService authenticationService,
                          SessionManager sessionManager, BookService bookService,
                          BookCopyService bookCopyService, BorrowService borrowService,
                          LoanService loanService, MemberService memberService,
                          NotificationService notificationService, FineService fineService) {
        this(stage, authenticationService, sessionManager, bookService, bookCopyService,
                borrowService, loanService, memberService, notificationService, fineService, null);
    }

    /**
     * Creates a navigator with explicit services for member pages, including reservations.
     *
     * @param stage the application window used for navigation.
     * @param authenticationService the service used by the login page.
     * @param sessionManager the session shared by authenticated pages.
     * @param bookService the service used by catalogue pages.
     * @param bookCopyService the service used to load physical book copies.
     * @param borrowService the service used to complete borrowing and return transactions.
     * @param loanService the service used to load and renew loans.
     * @param memberService the service used to update member profiles and passwords.
     * @param notificationService the service used to load and mark member notifications.
     * @param fineService the service used to load and pay member fines.
     * @param reservationService the service used to create and cancel member reservations.
     */
    public SceneNavigator(Stage stage, AuthenticationService authenticationService,
                          SessionManager sessionManager, BookService bookService,
                          BookCopyService bookCopyService, BorrowService borrowService,
                          LoanService loanService, MemberService memberService,
                          NotificationService notificationService, FineService fineService,
                          ReservationService reservationService) {
        this.stage = stage;
        this.authenticationService = authenticationService;
        this.sessionManager = sessionManager;
        this.bookService = bookService;
        this.bookCopyService = bookCopyService;
        this.borrowService = borrowService;
        this.loanService = loanService;
        this.memberService = memberService;
        this.notificationService = notificationService;
        this.fineService = fineService;
        this.reservationService = reservationService;
        this.scene = new Scene(new javafx.scene.layout.StackPane(), WINDOW_WIDTH,
                WINDOW_HEIGHT);
        String stylesheet = Objects.requireNonNull(
                getClass().getResource("/libconnect.css"), "Application stylesheet not found")
                .toExternalForm();
        this.scene.getStylesheets().add(stylesheet);
        this.stage.setTitle(APPLICATION_TITLE);
        this.stage.setScene(scene);
        this.stage.setMinWidth(WINDOW_WIDTH);
        this.stage.setMinHeight(WINDOW_HEIGHT);
    }

    /**
     * Creates a navigator with the librarian workspace dependencies in addition to member pages.
     *
     * @param stage the application window used for navigation.
     * @param authenticationService the service used by the login page.
     * @param sessionManager the session shared by authenticated pages.
     * @param bookService the service used by member pages.
     * @param bookCopyService the service used by member pages.
     * @param borrowService the service used by member pages.
     * @param loanService the service used by member pages.
     * @param memberService the service used by member pages.
     * @param librarianRuntime the runtime used by the librarian page.
     * @param librarianView the view receiving librarian controller feedback.
     */
    public SceneNavigator(Stage stage, AuthenticationService authenticationService,
                          SessionManager sessionManager, BookService bookService,
                          BookCopyService bookCopyService, BorrowService borrowService,
                          LoanService loanService, MemberService memberService,
                          LibrarianRuntime librarianRuntime, JavaFxLibrarianView librarianView) {
        this(stage, authenticationService, sessionManager, bookService, bookCopyService,
                borrowService, loanService, memberService, librarianRuntime.notificationService(),
                librarianRuntime.fineService(), librarianRuntime.reservationService());
        this.librarianRuntime = Objects.requireNonNull(librarianRuntime, "librarianRuntime");
        this.librarianView = Objects.requireNonNull(librarianView, "librarianView");
    }

    /**
     * Displays the login page and removes any previous authenticated page.
     */
    public void showLoginPage() {
        showLoginPage(null);
    }

    /**
     * Displays the login page with an optional success message.
     *
     * @param successMessage the message displayed after a successful operation, or null.
     */
    public void showLoginPage(String successMessage) {
        showPage(new LoginPage(authenticationService, sessionManager, this, successMessage));
    }

    /** Displays the account registration page. */
    public void showRegistrationPage() {
        showPage(new RegistrationPage(authenticationService, this));
    }

    /**
     * Displays the authenticated dashboard page.
     *
     * If no user is currently logged in, the navigator returns to the login page.
     */
    public void showDashboardPage() {
        showDashboardPage(null);
    }

    /**
     * Displays the authenticated dashboard page with an optional success message.
     *
     * @param successMessage the message displayed after a successful operation, or null.
     */
    public void showDashboardPage(String successMessage) {
        if (!sessionManager.isLoggedIn()) {
            showLoginPage();
            return;
        }

        if (notificationService == null) {
            showPage(new DashboardPage(bookService, sessionManager, this, successMessage));
        } else {
            showPage(new DashboardPage(bookService, sessionManager, this, successMessage,
                    notificationService));
        }
    }

    /** Displays all notifications for the authenticated member. */
    public void showNotificationsPage() {
        if (!sessionManager.isLoggedIn()) {
            showLoginPage();
            return;
        }
        if (notificationService == null) {
            showDashboardPage();
            return;
        }

        showPage(new NotificationsPage(notificationService, sessionManager, this));
    }

    /** Displays the authenticated librarian workspace. */
    public void showLibrarianPage() {
        if (!sessionManager.isLoggedIn()) {
            showLoginPage();
            return;
        }
        if (!(sessionManager.getCurrentUser() instanceof Librarian librarian)) {
            showDashboardPage();
            return;
        }
        if (librarianRuntime == null || librarianView == null) {
            throw new IllegalStateException("Librarian UI dependencies are not configured");
        }

        showPage(new LibrarianPage(librarianRuntime, librarianView, librarian.getEmployeeId(), () -> {
            sessionManager.logout();
            showLoginPage();
        }));
    }

    /** Displays the authenticated borrowing page. */
    public void showBorrowPage() {
        if (!sessionManager.isLoggedIn()) {
            showLoginPage();
            return;
        }

        showPage(new BorrowPage(bookService, bookCopyService, borrowService,
                sessionManager, this));
    }

    /** Displays the authenticated member's reservation page. */
    public void showReservationPage() {
        if (!sessionManager.isLoggedIn()) {
            showLoginPage();
            return;
        }

        if (reservationService == null) {
            showDashboardPage();
            return;
        }

        showPage(new ReservationPage(reservationService, bookService, sessionManager, this));
    }

    /**
     * Displays the authenticated member profile page.
     *
     * If no user is currently logged in, the navigator returns to the login page.
     */
    public void showProfilePage() {
        if (!sessionManager.isLoggedIn()) {
            showLoginPage();
            return;
        }

        showPage(new ProfilePage(memberService, loanService, bookService, bookCopyService,
                borrowService, sessionManager, this, fineService));
    }

    /**
     * Displays the details page for a book in the catalogue.
     *
     * @param isbn the ISBN of the book whose details should be displayed.
     */
    public void showBookInfoPage(String isbn) {
        if (!sessionManager.isLoggedIn()) {
            showLoginPage();
            return;
        }

        showPage(new BookInfoPage(bookService, bookCopyService, sessionManager, isbn, this));
    }

    private void showPage(Parent page) {
        scene.setRoot(page);
        stage.show();
    }
}
