package libconnect.ui.pages;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import libconnect.models.AccountType;
import libconnect.services.AuthenticationService;
import libconnect.services.ServiceException;
import libconnect.storage.repositories.RepositoryException;
import libconnect.ui.SceneNavigator;
import libconnect.ui.components.FeedbackMessage;
import libconnect.ui.components.FormField;
import libconnect.ui.components.MemberRegistrationForm;
import libconnect.ui.components.PageContainer;
import libconnect.ui.components.PageHeader;

/** Provides the account registration page. */
public final class RegistrationPage extends BorderPane {
    private static final String EMPTY_FIELDS_MESSAGE = "Name, email and password are required.";
    private static final String PASSWORD_MISMATCH_MESSAGE = "Passwords do not match.";
    private static final String SELECT_ACCOUNT_TYPE_MESSAGE = "Select an account type.";
    private static final String STORAGE_ERROR_MESSAGE =
            "Unable to access account data. Please try again.";
    private static final String LIBRARIAN_ERROR_MESSAGE =
            "Librarian registration is not available yet.";

    private final AuthenticationService authenticationService;
    private final SceneNavigator sceneNavigator;
    private final ComboBox<AccountType> accountTypeSelector;
    private final MemberRegistrationForm memberRegistrationForm;
    private final FeedbackMessage feedbackMessage;
    private final Button registerButton;

    /**
     * Creates a registration page connected to the application services.
     *
     * @param authenticationService the service used to create the account.
     * @param sceneNavigator the navigator used after registration or cancellation.
     */
    public RegistrationPage(AuthenticationService authenticationService,
                            SceneNavigator sceneNavigator) {
        this.authenticationService = authenticationService;
        this.sceneNavigator = sceneNavigator;
        accountTypeSelector = createAccountTypeSelector();
        memberRegistrationForm = new MemberRegistrationForm();
        feedbackMessage = new FeedbackMessage();
        registerButton = new Button("Register");

        memberRegistrationForm.setVisible(false);
        memberRegistrationForm.setManaged(false);
        registerButton.setDisable(true);
        registerButton.setOnAction(event -> registerAccount());

        Button backButton = new Button("Back to login");
        backButton.setOnAction(event -> sceneNavigator.showLoginPage());

        HBox navigationButtons = new HBox(12, registerButton, backButton);
        navigationButtons.setAlignment(Pos.CENTER);

        PageContainer pageContainer = new PageContainer(
                new PageHeader("Create an account", "Register for LibConnect"),
                new FormField("Account type", accountTypeSelector),
                memberRegistrationForm,
                feedbackMessage,
                navigationButtons);
        pageContainer.setPadding(new Insets(32));
        setCenter(pageContainer);
        setPadding(new Insets(24));
    }

    private ComboBox<AccountType> createAccountTypeSelector() {
        ComboBox<AccountType> selector = new ComboBox<>();
        ObservableList<AccountType> accountTypes = selector.getItems();
        accountTypes.addAll(AccountType.MEMBER, AccountType.LIBRARIAN);
        selector.setPromptText("Select account type");
        selector.setMaxWidth(Double.MAX_VALUE);
        selector.setCellFactory(listView -> new AccountTypeCell());
        selector.setButtonCell(new AccountTypeCell());
        selector.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == AccountType.LIBRARIAN) {
                selector.setValue(oldValue);
                feedbackMessage.showError(LIBRARIAN_ERROR_MESSAGE);
                return;
            }

            feedbackMessage.clearMessage();
            updateFormVisibility(newValue);
        });
        return selector;
    }

    private void updateFormVisibility(AccountType accountType) {
        boolean isMember = accountType == AccountType.MEMBER;
        memberRegistrationForm.setVisible(isMember);
        memberRegistrationForm.setManaged(isMember);
        registerButton.setDisable(!isMember);
    }

    private void registerAccount() {
        feedbackMessage.clearMessage();

        AccountType accountType = accountTypeSelector.getValue();
        if (accountType == null) {
            feedbackMessage.showError(SELECT_ACCOUNT_TYPE_MESSAGE);
            return;
        }
        if (accountType == AccountType.LIBRARIAN) {
            feedbackMessage.showError(LIBRARIAN_ERROR_MESSAGE);
            return;
        }

        String name = memberRegistrationForm.getName().trim();
        String email = memberRegistrationForm.getEmail().trim();
        String password = memberRegistrationForm.getPassword();
        String passwordConfirmation = memberRegistrationForm.getPasswordConfirmation();
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            feedbackMessage.showError(EMPTY_FIELDS_MESSAGE);
            return;
        }
        if (!password.equals(passwordConfirmation)) {
            feedbackMessage.showError(PASSWORD_MISMATCH_MESSAGE);
            return;
        }

        try {
            authenticationService.register(accountType, name, email, password);
            sceneNavigator.showLoginPage("Account created successfully. Please log in.");
        } catch (ServiceException | IllegalArgumentException exception) {
            feedbackMessage.showError(exception.getMessage());
        } catch (RepositoryException exception) {
            feedbackMessage.showError(STORAGE_ERROR_MESSAGE);
        }
    }

    private static final class AccountTypeCell extends ListCell<AccountType> {
        @Override
        protected void updateItem(AccountType accountType, boolean empty) {
            super.updateItem(accountType, empty);
            if (empty || accountType == null) {
                setText(null);
                setDisable(false);
                getStyleClass().remove("registration-unavailable");
                return;
            }

            boolean isLibrarian = accountType == AccountType.LIBRARIAN;
            setText(isLibrarian ? "Librarian (unavailable)" : "Member");
            setDisable(isLibrarian);
            if (isLibrarian) {
                getStyleClass().add("registration-unavailable");
            } else {
                getStyleClass().remove("registration-unavailable");
            }
        }
    }
}
