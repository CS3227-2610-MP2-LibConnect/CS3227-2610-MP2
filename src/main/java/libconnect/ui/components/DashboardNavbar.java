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

    /**
     * Creates a dashboard navbar with a disabled profile action and a logout action.
     *
     * @param logoutAction the action run when the logout button is selected.
     */
    public DashboardNavbar(Runnable logoutAction) {
        Objects.requireNonNull(logoutAction, "logoutAction");
        Label titleLabel = new Label("LibConnect");
        titleLabel.setStyle(TITLE_STYLE);

        Button dashboardButton = new Button("Dashboard");
        dashboardButton.setDisable(true);
        dashboardButton.setStyle(ACTIVE_NAVIGATION_STYLE);

        Button profileButton = new Button("Profile");
        profileButton.setDisable(true);

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(event -> logoutAction.run());

        HBox actions = new HBox(10, dashboardButton, profileButton, logoutButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        setLeft(titleLabel);
        setRight(actions);
        setPadding(new Insets(12, 20, 12, 20));
        setStyle(NAVBAR_STYLE);
    }
}
