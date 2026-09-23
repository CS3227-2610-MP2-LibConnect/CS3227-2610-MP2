package libconnect.ui.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import libconnect.models.AccountType;
import libconnect.models.Librarian;
import libconnect.models.User;
import libconnect.ui.SceneNavigator;

/** Displays the navigation bar for authenticated dashboard pages. */
public final class Navbar extends BorderPane {
    /** Identifies which navigation item represents the current page. */
    public enum ActivePage {
        /** Indicates that the dashboard is the current page. */
        DASHBOARD,

        /** Indicates that the borrowing page is the current page. */
        BORROW,

        /** Indicates that the my loans page is the current page. */
        MY_LOANS,

        /** Indicates that the reservation page is the current page. */
        RESERVATION,

        /** Indicates that the profile page is the current page. */
        PROFILE,

        /** Indicates that no navigation item is the current page. */
        NONE
    }

    public Navbar(SceneNavigator sceneNavigator, User user, ActivePage activePage, Runnable logoutAction) {
        Objects.requireNonNull(sceneNavigator, "sceneNavigator");
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(activePage, "activePage");


        Label titleLabel = new Label("LibConnect");
        titleLabel.getStyleClass().add("navbar-title");

        List<Button> buttons = new ArrayList<>();

        // Librarian exclusive actions
        if (user instanceof Librarian) {
            // TODO: Add librarian-exclusive action buttons here
        }

        Button dashboardButton = new Button("Dashboard");
        dashboardButton.setOnAction(event -> sceneNavigator.showDashboardPage());
        buttons.add(dashboardButton);

        Button borrowButton = new Button("Borrow books");
        borrowButton.setOnAction(event -> sceneNavigator.showBorrowPage());
        buttons.add(borrowButton);

        Button loansButton = new Button("My loans");
        loansButton.setOnAction(event -> sceneNavigator.showMyLoansPage());
        buttons.add(loansButton);

        Button reservationButton = new Button("Reservation");
        reservationButton.setOnAction(event -> sceneNavigator.showReservationPage());
        buttons.add(reservationButton);

        applyActiveStyle(dashboardButton, activePage == ActivePage.DASHBOARD);
        applyActiveStyle(borrowButton, activePage == ActivePage.BORROW);
        applyActiveStyle(loansButton, activePage == ActivePage.MY_LOANS);
        applyActiveStyle(reservationButton, activePage == ActivePage.RESERVATION);

        Button profileButton = new Button("Profile");
        profileButton.setOnAction(event -> sceneNavigator.showProfilePage());
        applyActiveStyle(profileButton, activePage == ActivePage.PROFILE);
        buttons.add(profileButton);

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(event -> logoutAction.run());
        buttons.add(logoutButton);

        HBox actions = new HBox(10);
        actions.getChildren().addAll(buttons);
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
