package libconnect.ui.components;

import java.util.Objects;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

/** Displays the navigation bar for authenticated dashboard pages. */
public final class DashboardNavbar extends BorderPane {
    /** Identifies which navigation item represents the current page. */
    public enum ActivePage {
        /** Indicates that the dashboard is the current page. */
        DASHBOARD,

        /** Indicates that the borrowing page is the current page. */
        BORROW,

        /** Indicates that the my loans page is the current page. */
        MY_LOANS,

        /** Indicates that the profile page is the current page. */
        PROFILE,

        /** Indicates that no navigation item is the current page. */
        NONE
    }

    /**
     * Creates a dashboard navbar with navigation, profile, and logout actions.
     *
     * @param dashboardAction the action run when the dashboard button is selected.
     * @param borrowAction the action run when the borrow books button is selected.
     * @param loansAction the action run when the my loans button is selected.
     * @param profileAction the action run when the profile button is selected.
     * @param logoutAction the action run when the logout button is selected.
     * @param activePage the page represented by the highlighted navigation item.
     */
    public DashboardNavbar(Runnable dashboardAction, Runnable borrowAction,
                           Runnable loansAction, Runnable profileAction, Runnable logoutAction,
                           ActivePage activePage) {
        Objects.requireNonNull(dashboardAction, "dashboardAction");
        Objects.requireNonNull(borrowAction, "borrowAction");
        Objects.requireNonNull(loansAction, "loansAction");
        Objects.requireNonNull(profileAction, "profileAction");
        Objects.requireNonNull(logoutAction, "logoutAction");
        Objects.requireNonNull(activePage, "activePage");
        Label titleLabel = new Label("LibConnect");
        titleLabel.getStyleClass().add("navbar-title");

        Button dashboardButton = new Button("Dashboard");
        dashboardButton.setOnAction(event -> dashboardAction.run());

        Button borrowButton = new Button("Borrow books");
        borrowButton.setOnAction(event -> borrowAction.run());

        Button loansButton = new Button("My loans");
        loansButton.setOnAction(event -> loansAction.run());

        applyActiveStyle(dashboardButton, activePage == ActivePage.DASHBOARD);
        applyActiveStyle(borrowButton, activePage == ActivePage.BORROW);
        applyActiveStyle(loansButton, activePage == ActivePage.MY_LOANS);

        Button profileButton = new Button("Profile");
        profileButton.setOnAction(event -> profileAction.run());
        applyActiveStyle(profileButton, activePage == ActivePage.PROFILE);

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(event -> logoutAction.run());

        HBox actions = new HBox(10, dashboardButton, borrowButton, loansButton,
                profileButton, logoutButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        setLeft(titleLabel);
        setRight(actions);
        setPadding(new Insets(12, 20, 12, 20));
        getStyleClass().add("navbar");
    }

    /**
     * Applies the active-page style and disables the corresponding navigation button.
     *
     * @param button the navigation button to update.
     * @param isActive whether the button represents the current page.
     */
    private void applyActiveStyle(Button button, boolean isActive) {
        if (isActive) {
            button.setDisable(true);
            button.getStyleClass().add("navbar-active");
        }
    }
}
