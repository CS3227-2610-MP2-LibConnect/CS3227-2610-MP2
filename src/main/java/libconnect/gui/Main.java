package libconnect.gui;

import java.nio.file.Path;
import java.time.Clock;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import libconnect.app.LibrarianCompositionRoot;
import libconnect.app.LibrarianRuntime;

/** Starts the JavaFX librarian desktop application. */
public final class Main extends Application {
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 760;

    private Stage stage;
    private LibrarianRuntime runtime;
    private JavaFxLibrarianView view;

    /** Starts the JavaFX application with the login screen. */
    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        view = new JavaFxLibrarianView();
        runtime = LibrarianCompositionRoot.create(Path.of("data"), Clock.systemDefaultZone(), view);
        stage.setTitle("LibConnect Librarian");
        showLogin();
        stage.show();
    }

    /** Returns the application to the login screen after logout. */
    private void showLogin() {
        LoginView loginView = new LoginView(runtime, view, this::showShell);
        stage.setScene(new Scene(loginView, WINDOW_WIDTH, WINDOW_HEIGHT));
    }

    /** Shows the authorized librarian shell for the supplied employee ID. */
    private void showShell(String employeeId) {
        LibrarianShell shell = new LibrarianShell(runtime, view, employeeId, this::showLogin);
        stage.setScene(new Scene(shell, WINDOW_WIDTH, WINDOW_HEIGHT));
    }

    /** Starts the desktop application. */
    public static void main(String[] args) {
        launch(args);
    }
}
