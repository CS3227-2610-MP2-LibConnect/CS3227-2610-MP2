package libconnect.gui;

import java.util.Objects;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import libconnect.app.LibrarianRuntime;
import libconnect.models.Librarian;

/** Provides the temporary librarian login screen until shared authentication is integrated. */
public final class LoginView extends VBox {
    private final Label statusLabel = new Label();
    private final TextField employeeIdField = new TextField();
    private final Button loginButton = new Button("Log in");
    private final Button demoButton = new Button("Prepare demo account");

    /** Creates a simple employee-ID login screen. */
    public LoginView(LibrarianRuntime runtime, JavaFxLibrarianView view,
                     Consumer<String> onLogin) {
        Objects.requireNonNull(runtime, "runtime");
        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(onLogin, "onLogin");

        setSpacing(12);
        setPadding(new Insets(40));
        setMaxWidth(420);

        Label title = new Label("LibConnect Librarian");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        Label description = new Label("Sign in with an active employee ID.");
        employeeIdField.setId("employee-id");
        employeeIdField.setPromptText("Employee ID");
        loginButton.setId("login-button");
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(event -> {
            try {
                Librarian librarian = runtime.librarianService().requireActive(employeeIdField.getText());
                view.showMessage("Logged in as " + librarian.getName());
                onLogin.accept(librarian.getId());
            } catch (RuntimeException exception) {
                view.showError(exception.getMessage());
            }
        });
        demoButton.setId("demo-account-button");
        demoButton.setOnAction(event -> {
            try {
                if (runtime.librarianService().search("e1").isEmpty()) {
                    runtime.librarianService().register(new Librarian("e1", "Demo Librarian",
                            "librarian@example.com", libconnect.models.AccountStatus.ACTIVE));
                }
                view.showMessage("Demo account ready. Use employee ID e1.");
            } catch (RuntimeException exception) {
                view.showError(exception.getMessage());
            }
        });
        statusLabel.setId("login-status");
        view.bind(statusLabel);

        getChildren().addAll(title, description, employeeIdField,
                new javafx.scene.layout.HBox(8, loginButton, demoButton), statusLabel);
    }

    /** Returns the label used for login status messages. */
    public Label getStatusLabel() {
        return statusLabel;
    }

    /** Returns the employee-ID field for accessible UI integration tests. */
    public TextField getEmployeeIdField() {
        return employeeIdField;
    }

    /** Returns the login action for deterministic UI automation. */
    public Button getLoginButton() {
        return loginButton;
    }

    /** Returns the demo-account action for deterministic UI automation. */
    public Button getDemoButton() {
        return demoButton;
    }
}
