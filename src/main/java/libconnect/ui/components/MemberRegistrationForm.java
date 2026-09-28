package libconnect.ui.components;

import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Provides the reusable input fields required to register a member account. */
public final class MemberRegistrationForm extends VBox {
    private static final double SPACING = 12;

    private final TextField nameField;
    private final TextField emailField;
    private final PasswordField passwordField;
    private final PasswordField confirmationField;

    /** Creates an empty member registration form. */
    public MemberRegistrationForm() {
        nameField = new TextField();
        emailField = new TextField();
        passwordField = new PasswordField();
        confirmationField = new PasswordField();

        nameField.setPromptText("Enter your full name");
        emailField.setPromptText("Enter your email");
        passwordField.setPromptText("Enter your password");
        confirmationField.setPromptText("Re-enter your password");

        setSpacing(SPACING);
        getChildren().addAll(
                new FormField("Name", nameField),
                new FormField("Email", emailField),
                new FormField("Password", passwordField),
                new FormField("Confirm password", confirmationField));
    }

    /**
     * Returns the name entered by the user.
     *
     * @return the member name.
     */
    public String getName() {
        return nameField.getText();
    }

    /**
     * Returns the email entered by the user.
     *
     * @return the member email.
     */
    public String getEmail() {
        return emailField.getText();
    }

    /**
     * Returns the password entered by the user.
     *
     * @return the member password.
     */
    public String getPassword() {
        return passwordField.getText();
    }

    /**
     * Returns the password confirmation entered by the user.
     *
     * @return the confirmed member password.
     */
    public String getPasswordConfirmation() {
        return confirmationField.getText();
    }
}
