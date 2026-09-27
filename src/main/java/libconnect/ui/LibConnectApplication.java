package libconnect.ui;

import java.nio.file.Path;
import java.time.Clock;

import javafx.application.Application;
import javafx.stage.Stage;

import libconnect.services.AuthenticationService;
import libconnect.services.BookCopyService;
import libconnect.services.BookService;
import libconnect.services.BorrowService;
import libconnect.ui.components.JavaFxLibrarianView;

/**
 * Starts the LibConnect desktop application.
 */
public final class LibConnectApplication extends Application {
    /**
     * Creates and displays the initial JavaFX application window.
     *
     * @param stage the primary application window.
     */
    @Override
    public void start(Stage stage) {
        AuthenticationService authenticationService = new AuthenticationService();
        BookCopyService bookCopyService = new BookCopyService();
        BookService bookService = new BookService();
        SessionManager sessionManager = new SessionManager();
        JavaFxLibrarianView librarianView = new JavaFxLibrarianView();
        Clock clock = Clock.systemDefaultZone();
        LibrarianRuntime librarianRuntime = LibrarianCompositionRoot.create(
                Path.of("data"), clock, librarianView);
        BorrowService borrowService = new BorrowService(librarianRuntime.fineService(), clock);
        SceneNavigator sceneNavigator = new SceneNavigator(stage, authenticationService,
                sessionManager, bookService, bookCopyService, borrowService,
                new libconnect.services.LoanService(), new libconnect.services.MemberService(),
                librarianRuntime, librarianView);

        sceneNavigator.showLoginPage();
    }
}
