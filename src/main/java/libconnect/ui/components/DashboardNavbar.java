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
    private static final String NAVBAR_STYLE = "-fx-background-color: #1f3a5f;";
    private static final String TITLE_STYLE = "-fx-text-fill: white; -fx-font-size: 20px;"
            + " -fx-font-weight: bold;";
    private static final String ACTIVE_NAVIGATION_STYLE = "-fx-background-color: #5b8db8;"
            + " -fx-text-fill: white; -fx-opacity: 1.0;";

    /** Identifies which navigation item represents the current page. */
    public enum ActivePage {
        /** Indicates that the dashboard is the current page. */
        DASHBOARD,

        /** Indicates that the borrowing page is the current page. */
        BORROW,

        /** Indicates that no navigation item is the current page. */
        NONE
    }

    /**
     * Creates a dashboard navbar with navigation, borrowing, and logout actions.
     *
     * @param dashboardAction the action run when the dashboard button is selected.
     * @param borrowAction the action run when the borrow books button is selected.
     * @param logoutAction the action run when the logout button is selected.
     * @param activePage the page represented by the highlighted navigation item.
     */
    public DashboardNavbar(Runnable dashboardAction, Runnable borrowAction,
                           Runnable logoutAction, ActivePage activePage) {
        Objects.requireNonNull(dashboardAction, "dashboardAction");
        Objects.requireNonNull(borrowAction, "borrowAction");
        Objects.requireNonNull(logoutAction, "logoutAction");
        Objects.requireNonNull(activePage, "activePage");
        Label titleLabel = new Label("LibConnect");
        titleLabel.setStyle(TITLE_STYLE);

        Button dashboardButton = new Button("Dashboard");
        dashboardButton.setOnAction(event -> dashboardAction.run());

        Button borrowButton = new Button("Borrow books");
        borrowButton.setOnAction(event -> borrowAction.run());

        applyActiveStyle(dashboardButton, activePage == ActivePage.DASHBOARD);
        applyActiveStyle(borrowButton, activePage == ActivePage.BORROW);

        Button profileButton = new Button("Profile");
        profileButton.setDisable(true);

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(event -> logoutAction.run());

        HBox actions = new HBox(10, dashboardButton, borrowButton, profileButton, logoutButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        setLeft(titleLabel);
        setRight(actions);
        setPadding(new Insets(12, 20, 12, 20));
        setStyle(NAVBAR_STYLE);
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
            button.setStyle(ACTIVE_NAVIGATION_STYLE);
        }
    }
}
