package libconnect.ui.pages;

import java.util.Objects;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import libconnect.models.Member;
import libconnect.models.User;
import libconnect.services.MemberService;
import libconnect.services.ServiceException;
import libconnect.storage.repositories.RepositoryException;
import libconnect.ui.SceneNavigator;
import libconnect.ui.SessionManager;
import libconnect.ui.components.DashboardNavbar;
import libconnect.ui.components.FeedbackMessage;
import libconnect.ui.components.FormField;
import libconnect.ui.components.PageHeader;

/** Provides profile and password management for the authenticated member. */
public final class ProfilePage extends BorderPane {
    private static final String EMPTY_PROFILE_FIELDS_MESSAGE =
            "Name and email are required.";
    private static final String EMPTY_PASSWORD_MESSAGE = "Password is required.";
    private static final String PASSWORD_MISMATCH_MESSAGE = "Passwords do not match.";
    private static final String PROFILE_SUCCESS_MESSAGE = "Profile updated successfully.";
    private static final String PASSWORD_SUCCESS_MESSAGE = "Password updated successfully.";
    private static final String PROFILE_ERROR_MESSAGE =
            "Unable to update profile. Please try again.";
    private static final String PASSWORD_ERROR_MESSAGE =
            "Unable to update password. Please try again.";
    private static final String STORAGE_ERROR_MESSAGE =
            "Unable to access account data. Please try again.";
    private static final String MEMBER_ONLY_MESSAGE =
            "Only an authenticated member can update profile information.";

    private final MemberService memberService;
    private final SessionManager sessionManager;
    private final TextField nameField;
    private final TextField emailField;
    private final PasswordField newPasswordField;
    private final PasswordField confirmationPasswordField;
    private final FeedbackMessage profileFeedbackMessage;
    private final FeedbackMessage passwordFeedbackMessage;

    /**
     * Creates a profile page backed by the supplied member service and session.
     *
     * @param memberService the service used to update member data.
     * @param sessionManager the session containing the authenticated member.
     * @param sceneNavigator the navigator used for page transitions.
     * @throws NullPointerException if any dependency is null.
     */
    public ProfilePage(MemberService memberService, SessionManager sessionManager,
                       SceneNavigator sceneNavigator) {
        this.memberService = Objects.requireNonNull(memberService, "memberService");
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
        Objects.requireNonNull(sceneNavigator, "sceneNavigator");
        nameField = new TextField();
        emailField = new TextField();
        newPasswordField = new PasswordField();
        confirmationPasswordField = new PasswordField();
        profileFeedbackMessage = new FeedbackMessage();
        passwordFeedbackMessage = new FeedbackMessage();

        configureFields();
        populateProfileFields();
        setTop(createNavbar(sceneNavigator));
        setCenter(createContent());
    }

    /** Configures labels, prompts, and input widths for the profile form. */
    private void configureFields() {
        nameField.setPromptText("Enter your name");
        emailField.setPromptText("Enter your email");
        newPasswordField.setPromptText("Enter your new password");
        confirmationPasswordField.setPromptText("Re-type your new password");

        nameField.setMaxWidth(Double.MAX_VALUE);
        emailField.setMaxWidth(Double.MAX_VALUE);
        newPasswordField.setMaxWidth(Double.MAX_VALUE);
        confirmationPasswordField.setMaxWidth(Double.MAX_VALUE);
    }

    /** Populates the profile fields using the member stored in the current session. */
    private void populateProfileFields() {
        Member currentMember = getCurrentMember();
        if (currentMember == null) {
            profileFeedbackMessage.showError(MEMBER_ONLY_MESSAGE);
            return;
        }

        nameField.setText(currentMember.getName());
        emailField.setText(currentMember.getEmail());
    }

    /**
     * Creates the authenticated-page navigation bar for the profile page.
     *
     * @param sceneNavigator the navigator used by the navigation actions.
     * @return the configured navigation bar.
     */
    private DashboardNavbar createNavbar(SceneNavigator sceneNavigator) {
        return new DashboardNavbar(sceneNavigator::showDashboardPage,
                sceneNavigator::showBorrowPage, sceneNavigator::showMyLoansPage,
                sceneNavigator::showProfilePage, () -> {
                    sessionManager.logout();
                    sceneNavigator.showLoginPage();
                }, DashboardNavbar.ActivePage.PROFILE);
    }

    /**
     * Creates the scrollable profile page content.
     *
     * @return the scrollable page content.
     */
    private ScrollPane createContent() {
        VBox content = new VBox(20,
                new PageHeader("Profile", "Manage your LibConnect account"),
                createProfileSection(),
                createPasswordSection());
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(20));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        return scrollPane;
    }

    /**
     * Creates the section for updating the member's name and email.
     *
     * @return the profile-information section.
     */
    private VBox createProfileSection() {
        Button updateButton = new Button("Update information");
        updateButton.setOnAction(event -> updateProfile());

        HBox buttonRow = new HBox(updateButton);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        VBox section = new VBox(12,
                createSectionHeading("Update information"),
                new FormField("Name", nameField),
                new FormField("Email", emailField),
                profileFeedbackMessage,
                buttonRow);
        section.setMaxWidth(520);
        return section;
    }

    /**
     * Creates the section for resetting the member's password.
     *
     * @return the password-reset section.
     */
    private VBox createPasswordSection() {
        Button resetPasswordButton = new Button("Reset password");
        resetPasswordButton.setOnAction(event -> resetPassword());

        HBox buttonRow = new HBox(resetPasswordButton);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        VBox section = new VBox(12,
                createSectionHeading("Reset password"),
                new FormField("New password", newPasswordField),
                new FormField("Re-type new password", confirmationPasswordField),
                passwordFeedbackMessage,
                buttonRow);
        section.setMaxWidth(520);
        return section;
    }

    /**
     * Creates a heading for one profile-management section.
     *
     * @param text the heading text.
     * @return the styled section heading.
     */
    private Label createSectionHeading(String text) {
        Label heading = new Label(text);
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        return heading;
    }

    /** Handles validation and persistence for the profile-information form. */
    private void updateProfile() {
        profileFeedbackMessage.clearMessage();
        Member currentMember = getCurrentMember();
        if (currentMember == null) {
            profileFeedbackMessage.showError(MEMBER_ONLY_MESSAGE);
            return;
        }

        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        if (name.isBlank() || email.isBlank()) {
            profileFeedbackMessage.showError(EMPTY_PROFILE_FIELDS_MESSAGE);
            return;
        }

        try {
            Member updatedMember = memberService.updateMemberProfile(
                    currentMember.getMembershipId(), name, email);
            sessionManager.login(updatedMember);
            nameField.setText(updatedMember.getName());
            emailField.setText(updatedMember.getEmail());
            profileFeedbackMessage.showSuccess(PROFILE_SUCCESS_MESSAGE);
        } catch (ServiceException | IllegalArgumentException exception) {
            profileFeedbackMessage.showError(getMessageOrDefault(
                    exception, PROFILE_ERROR_MESSAGE));
        } catch (RepositoryException exception) {
            profileFeedbackMessage.showError(STORAGE_ERROR_MESSAGE);
        }
    }

    /** Handles validation and persistence for the password-reset form. */
    private void resetPassword() {
        passwordFeedbackMessage.clearMessage();
        Member currentMember = getCurrentMember();
        if (currentMember == null) {
            passwordFeedbackMessage.showError(MEMBER_ONLY_MESSAGE);
            return;
        }

        String newPassword = newPasswordField.getText();
        String confirmationPassword = confirmationPasswordField.getText();
        if (newPassword.isBlank()) {
            passwordFeedbackMessage.showError(EMPTY_PASSWORD_MESSAGE);
            return;
        }
        if (!newPassword.equals(confirmationPassword)) {
            passwordFeedbackMessage.showError(PASSWORD_MISMATCH_MESSAGE);
            return;
        }

        try {
            Member updatedMember = memberService.updatePassword(
                    currentMember.getMembershipId(), newPassword);
            sessionManager.login(updatedMember);
            newPasswordField.clear();
            confirmationPasswordField.clear();
            passwordFeedbackMessage.showSuccess(PASSWORD_SUCCESS_MESSAGE);
        } catch (ServiceException | IllegalArgumentException exception) {
            newPasswordField.clear();
            confirmationPasswordField.clear();
            passwordFeedbackMessage.showError(getMessageOrDefault(
                    exception, PASSWORD_ERROR_MESSAGE));
        } catch (RepositoryException exception) {
            newPasswordField.clear();
            confirmationPasswordField.clear();
            passwordFeedbackMessage.showError(STORAGE_ERROR_MESSAGE);
        }
    }

    /**
     * Returns the authenticated member, if the current session contains one.
     *
     * @return the authenticated member, or null for a non-member session.
     */
    private Member getCurrentMember() {
        User currentUser = sessionManager.getCurrentUser();
        return currentUser instanceof Member member ? member : null;
    }

    /**
     * Returns an exception message unless it is blank, in which case a fallback is returned.
     *
     * @param exception the exception whose message should be displayed.
     * @param fallback the message used when the exception has no useful message.
     * @return a non-blank user-facing message.
     */
    private String getMessageOrDefault(RuntimeException exception, String fallback) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }
}
