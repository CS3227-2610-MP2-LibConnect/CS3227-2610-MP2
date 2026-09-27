package libconnect.ui.pages;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import libconnect.models.Book;
import libconnect.models.Member;
import libconnect.models.Reservation;
import libconnect.models.ReservationStatus;
import libconnect.models.User;
import libconnect.services.BookService;
import libconnect.services.ReservationService;
import libconnect.storage.repositories.RepositoryException;
import libconnect.ui.SceneNavigator;
import libconnect.ui.SessionManager;
import libconnect.ui.components.Navbar;
import libconnect.ui.components.FeedbackMessage;
import libconnect.ui.components.PageHeader;

/** Provides reservation creation and cancellation for an authenticated member. */
public final class ReservationPage extends BorderPane {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final String STORAGE_ERROR_MESSAGE =
            "Unable to access reservation data. Please try again.";
    private static final String ACTION_ERROR_MESSAGE =
            "Unable to update the reservation. Please try again.";

    private final ReservationService reservationService;
    private final BookService bookService;
    private final SessionManager sessionManager;
    private final TextField isbnField;
    private final VBox reservationList;
    private final FeedbackMessage feedbackMessage;

    /**
     * Creates a reservation page backed by the supplied services and session.
     *
     * @param reservationService the service used to create, load, and cancel reservations.
     * @param bookService the service used to resolve an ISBN to a catalogue book.
     * @param sessionManager the session used by the logout action.
     * @param sceneNavigator the navigator used by the navbar actions.
     */
    public ReservationPage(ReservationService reservationService, BookService bookService,
                           SessionManager sessionManager, SceneNavigator sceneNavigator) {
        this.reservationService = Objects.requireNonNull(reservationService, "reservationService");
        this.bookService = Objects.requireNonNull(bookService, "bookService");
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
        Objects.requireNonNull(sceneNavigator, "sceneNavigator");

        Navbar navbar = new Navbar(sceneNavigator, sessionManager.getCurrentUser(),
                Navbar.ActivePage.RESERVATION, () -> {
            sessionManager.logout();
            sceneNavigator.showLoginPage();
        });
        setTop(navbar);
        isbnField = new TextField();
        isbnField.setPromptText("Enter book ISBN");
        isbnField.setId("reservation-isbn");
        reservationList = new VBox(12);
        feedbackMessage = new FeedbackMessage();
        setCenter(createContent());
        refreshReservations();
    }

    private VBox createContent() {
        Button createButton = new Button("Create reservation");
        createButton.setId("create-reservation-button");
        createButton.setOnAction(event -> createReservation());
        isbnField.setOnAction(event -> createReservation());

        HBox form = new HBox(8, isbnField, createButton);
        form.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(16,
                new PageHeader("Reservations", "Reserve an unavailable book and manage your reservations"),
                form,
                feedbackMessage,
                new Label("My reservations"),
                reservationList);
        content.setPadding(new Insets(20));
        VBox.setVgrow(reservationList, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox wrapper = new VBox(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return wrapper;
    }

    private void createReservation() {
        Member member = getAuthenticatedMember();
        if (member == null) {
            return;
        }

        String isbn = isbnField.getText().trim();
        if (isbn.isBlank()) {
            showError("Enter an ISBN first.");
            return;
        }

        try {
            Book book = bookService.getBookByIsbn(isbn)
                    .orElseThrow(() -> new IllegalArgumentException("Book does not exist"));
            reservationService.reserveBook(member.getMembershipId(), book.getIsbn());
            feedbackMessage.showSuccess("Reservation created successfully.");
            isbnField.clear();
            refreshReservations();
        } catch (RepositoryException exception) {
            showError(STORAGE_ERROR_MESSAGE);
        } catch (RuntimeException exception) {
            showError(getActionErrorMessage(exception));
        }
    }

    private void cancelReservation(Reservation reservation) {
        try {
            reservationService.cancelReservation(reservation.getId());
            feedbackMessage.showSuccess("Reservation cancelled successfully.");
            refreshReservations();
        } catch (RepositoryException exception) {
            showError(STORAGE_ERROR_MESSAGE);
        } catch (RuntimeException exception) {
            showError(getActionErrorMessage(exception));
        }
    }

    private void refreshReservations() {
        reservationList.getChildren().clear();
        Member member = getAuthenticatedMember();
        if (member == null) {
            return;
        }

        try {
            List<Reservation> reservations = reservationService.getReservations(member.getMembershipId());
            if (reservations.isEmpty()) {
                reservationList.getChildren().add(new Label("You have no reservations."));
                return;
            }
            reservations.stream()
                    .map(this::createReservationCard)
                    .forEach(reservationList.getChildren()::add);
        } catch (RepositoryException exception) {
            showError(STORAGE_ERROR_MESSAGE);
        } catch (RuntimeException exception) {
            showError(getActionErrorMessage(exception));
        }
    }

    private BorderPane createReservationCard(Reservation reservation) {
        GridPane details = new GridPane();
        details.setHgap(16);
        details.setVgap(5);
        addDetail(details, "Reservation ID", reservation.getId(), 0);
        addDetail(details, "ISBN", reservation.getBookId(), 1);
        addDetail(details, "Reserved", DATE_FORMATTER.format(reservation.getReservationDate()), 2);
        addDetail(details, "Expires", DATE_FORMATTER.format(reservation.getExpiryDate()), 3);
        addDetail(details, "Status", reservation.getStatus().toString(), 4);

        Label heading = new Label("Book reservation");
        heading.getStyleClass().add("card-heading");
        VBox content = new VBox(8, heading, details);
        BorderPane card = new BorderPane(content);
        card.setPadding(new Insets(12));
        card.getStyleClass().add("loan-card");

        if (reservation.getStatus() == ReservationStatus.PENDING) {
            Button cancelButton = new Button("Cancel reservation");
            cancelButton.setId("cancel-reservation-" + reservation.getId());
            cancelButton.setOnAction(event -> cancelReservation(reservation));
            BorderPane.setAlignment(cancelButton, Pos.CENTER_RIGHT);
            card.setRight(cancelButton);
        }
        return card;
    }

    private void addDetail(GridPane details, String fieldName, String value, int row) {
        Label fieldLabel = new Label(fieldName + ":");
        fieldLabel.getStyleClass().add("bold-label");
        details.add(fieldLabel, 0, row);
        details.add(new Label(value), 1, row);
    }

    private Member getAuthenticatedMember() {
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser instanceof Member member) {
            return member;
        }
        showError("Only an authenticated member can manage reservations.");
        return null;
    }

    private String getActionErrorMessage(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? ACTION_ERROR_MESSAGE : exception.getMessage();
    }

    private void showError(String message) {
        feedbackMessage.showError(message);
    }
}
