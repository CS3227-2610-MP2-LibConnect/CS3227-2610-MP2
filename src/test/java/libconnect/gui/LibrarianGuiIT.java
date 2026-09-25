package libconnect.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.control.LabeledMatchers.hasText;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import libconnect.app.LibrarianCompositionRoot;
import libconnect.app.LibrarianRuntime;
import libconnect.models.AccountStatus;
import libconnect.models.Librarian;

/** Exercises the primary JavaFX login, navigation, and feature-panel workflow. */
@ExtendWith(ApplicationExtension.class)
class LibrarianGuiIT {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-21T00:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path temporaryDirectory;

    private LibrarianRuntime runtime;
    private JavaFxLibrarianView view;
    private LoginView loginView;
    private TextField employeeIdField;

    /** Starts the login scene with isolated temporary persistence. */
    @Start
    void start(Stage stage) {
        view = new JavaFxLibrarianView();
        runtime = LibrarianCompositionRoot.create(temporaryDirectory, CLOCK, view);
        showLogin(stage);
    }

    @Test
    void login_validEmployeeId_opensShell(FxRobot robot) {
        registerActiveLibrarian();
        enterEmployeeId(robot, "e1");
        fireLogin(robot);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("#librarian-shell", isVisible());
        verifyThat("Signed in: Ada", isVisible());
    }

    @Test
    void login_unknownEmployeeId_showsError(FxRobot robot) {
        enterEmployeeId(robot, "missing");
        fireLogin(robot);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("#login-status", hasText("Librarian does not exist"));
    }

    @Test
    void demoAccountAndNavigation_exposeAllLibrarianPanels(FxRobot robot) {
        robot.interact(() -> loginView.getDemoButton().fire());
        enterEmployeeId(robot, "e1");
        fireLogin(robot);
        WaitForAsyncUtils.waitForFxEvents();

        TabPane navigation = robot.lookup("#librarian-navigation").queryAs(TabPane.class);
        String[] tabs = {"Dashboard", "Members", "Books", "Loans", "Reservations", "Fines",
            "Notifications"};
        for (int index = 0; index < tabs.length; index++) {
            int selectedIndex = index;
            robot.interact(() -> navigation.getSelectionModel().select(selectedIndex));
            assertEquals(tabs[index], navigation.getSelectionModel().getSelectedItem().getText());
        }
    }

    @Test
    void memberPanel_searchDisplaysMemberResults(FxRobot robot) {
        registerActiveLibrarian();
        enterEmployeeId(robot, "e1");
        fireLogin(robot);
        WaitForAsyncUtils.waitForFxEvents();
        TabPane navigation = robot.lookup("#librarian-navigation").queryAs(TabPane.class);
        robot.interact(() -> navigation.getSelectionModel().select(1));
        Button search = robot.lookup("#members-search").queryAs(Button.class);
        robot.interact(search::fire);

        TableView<?> table = robot.lookup(".table-view").query();
        assertEquals(1, table.getItems().size());
    }

    @Test
    void logout_returnsToLoginScreen(FxRobot robot) {
        registerActiveLibrarian();
        enterEmployeeId(robot, "e1");
        fireLogin(robot);
        WaitForAsyncUtils.waitForFxEvents();
        Button logout = robot.lookup("#logout-button").queryAs(Button.class);
        robot.interact(logout::fire);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("#login-button", isVisible());
    }

    private void showLogin(Stage stage) {
        loginView = new LoginView(runtime, view,
                employeeId -> stage.setScene(new Scene(new LibrarianShell(runtime, view, employeeId,
                        () -> showLogin(stage)), 1200, 760)));
        employeeIdField = loginView.getEmployeeIdField();
        stage.setScene(new Scene(loginView, 1200, 760));
        stage.show();
    }

    private void enterEmployeeId(FxRobot robot, String employeeId) {
        robot.interact(() -> employeeIdField.setText(employeeId));
    }

    private void fireLogin(FxRobot robot) {
        robot.interact(() -> loginView.getLoginButton().fire());
    }

    private void registerActiveLibrarian() {
        runtime.librarianService().register(new Librarian("e1", "Ada", "ada@example.com",
                AccountStatus.ACTIVE));
    }
}
