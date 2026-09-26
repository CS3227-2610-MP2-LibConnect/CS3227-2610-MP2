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
        BorrowService borrowService = new BorrowService();
        SessionManager sessionManager = new SessionManager();
        JavaFxLibrarianView librarianView = new JavaFxLibrarianView();
        LibrarianRuntime librarianRuntime = LibrarianCompositionRoot.create(
                Path.of("data"), Clock.systemDefaultZone(), librarianView);
        SceneNavigator sceneNavigator = new SceneNavigator(stage, authenticationService,
                sessionManager, bookService, bookCopyService, borrowService,
                new libconnect.services.LoanService(), new libconnect.services.MemberService(),
                librarianRuntime, librarianView);

        sceneNavigator.showLoginPage();
    }
}
