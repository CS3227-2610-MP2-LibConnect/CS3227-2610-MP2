package libconnect.ui.pages;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

import libconnect.models.AccountType;
import libconnect.models.User;
import libconnect.services.AuthenticationException;
import libconnect.services.AuthenticationService;
import libconnect.storage.repositories.RepositoryException;
import libconnect.ui.SceneNavigator;
import libconnect.ui.SessionManager;
import libconnect.ui.components.FeedbackMessage;
import libconnect.ui.components.FormField;
import libconnect.ui.components.PageContainer;
import libconnect.ui.components.PageHeader;

/** Provides the login page for member authentication. */
public final class LoginPage extends BorderPane {
    private static final String EMPTY_FIELDS_MESSAGE = "Username and password are required.";
    private static final String STORAGE_ERROR_MESSAGE =
            "Unable to access account data. Please try again.";

    private final AuthenticationService authenticationService;
    private final SessionManager sessionManager;
    private final SceneNavigator sceneNavigator;
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final FeedbackMessage feedbackMessage;

    /**
     * Creates a login page connected to the application authentication and navigation services.
     *
     * @param authenticationService the service used to authenticate members.
     * @param sessionManager the session that stores the authenticated user.
     * @param sceneNavigator the navigator used after authentication.
     */
    public LoginPage(AuthenticationService authenticationService, SessionManager sessionManager,
                     SceneNavigator sceneNavigator) {
        this(authenticationService, sessionManager, sceneNavigator, null);
    }

    /**
     * Creates a login page with an optional success message.
     *
     * @param authenticationService the service used to authenticate members.
     * @param sessionManager the session that stores the authenticated user.
     * @param sceneNavigator the navigator used after authentication.
     * @param successMessage the message displayed after a successful operation, or null.
     */
    public LoginPage(AuthenticationService authenticationService, SessionManager sessionManager,
                     SceneNavigator sceneNavigator, String successMessage) {
        this.authenticationService = authenticationService;
        this.sessionManager = sessionManager;
        this.sceneNavigator = sceneNavigator;
        usernameField = new TextField();
        passwordField = new PasswordField();
        feedbackMessage = new FeedbackMessage();

        usernameField.setPromptText("Enter your username or email");
        passwordField.setPromptText("Enter your password");

        Button loginButton = new Button("Login");
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(event -> authenticate());

        Button createAccountButton = new Button("Create account");
        createAccountButton.setOnAction(event -> sceneNavigator.showRegistrationPage());

        if (successMessage != null && !successMessage.isBlank()) {
            feedbackMessage.showSuccess(successMessage);
        }

        PageContainer pageContainer = new PageContainer(
                new PageHeader("Welcome to LibConnect", "Sign in to continue"),
                new FormField("Email", usernameField),
                new FormField("Password", passwordField),
                feedbackMessage,
                loginButton,
                createAccountButton);
        pageContainer.setPadding(new Insets(32));
        setCenter(pageContainer);
        setPadding(new Insets(24));
    }

    private void authenticate() {
        feedbackMessage.clearMessage();

        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (username.isBlank() || password.isBlank()) {
            feedbackMessage.showError(EMPTY_FIELDS_MESSAGE);
            return;
        }

        try {
            User authenticatedUser = authenticationService.authenticate(
                    AccountType.MEMBER, username, password);
            sessionManager.login(authenticatedUser);
            passwordField.clear();
            sceneNavigator.showDashboardPage();
        } catch (AuthenticationException exception) {
            passwordField.clear();
            feedbackMessage.showError(exception.getMessage());
        } catch (RepositoryException exception) {
            passwordField.clear();
            feedbackMessage.showError(STORAGE_ERROR_MESSAGE);
        }
    }
}
