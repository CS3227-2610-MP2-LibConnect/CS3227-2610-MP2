package libconnect.ui.components;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import libconnect.models.Loan;

/** Displays the details and available actions for one member loan. */
public final class LoanCard extends BorderPane {
    private static final String CARD_STYLE = "-fx-border-color: #d0d7de;"
            + " -fx-border-radius: 6; -fx-background-radius: 6;"
            + " -fx-background-color: white;";
    private static final String OVERDUE_CARD_STYLE = "-fx-border-color: #c62828;"
            + " -fx-border-width: 2; -fx-border-radius: 6; -fx-background-radius: 6;"
            + " -fx-background-color: #ffebee;";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    /**
     * Creates a card for a loan.
     *
     * @param loan the loan represented by the card.
     * @param title the title of the associated book.
     * @param author the author of the associated book.
     * @param isbn the ISBN of the associated book, or a fallback value.
     * @param showActions whether return and renew actions should be displayed.
     * @param returnAction the action run when Return is pressed.
     * @param renewAction the action run when Renew is pressed.
     * @throws NullPointerException if a required argument is null.
     */
    public LoanCard(Loan loan, String title, String author, String isbn,
                    boolean showActions, Runnable returnAction, Runnable renewAction) {
        Objects.requireNonNull(loan, "loan");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(isbn, "isbn");

        GridPane details = new GridPane();
        details.setHgap(16);
        details.setVgap(5);
        addDetail(details, "Title", title, 0);
        addDetail(details, "Author", author, 1);
        addDetail(details, "ISBN", isbn, 2);
        addDetail(details, "Copy ID", loan.getCopyId(), 3);
        addDetail(details, "Borrowed", formatDate(loan.getBorrowDate()), 4);
        addDetail(details, loan.isActive() ? "Due" : "Returned",
                formatDate(loan.isActive() ? loan.getDueDate() : loan.getReturnDate()), 5);
        addDetail(details, "Status", getStatusText(loan), 6);
        addDetail(details, "Renewed", loan.hasBeenRenewed() ? "Yes" : "No", 7);

        Label heading = new Label(title);
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        VBox content = new VBox(8, heading, details);
        setCenter(content);

        if (showActions) {
            Objects.requireNonNull(returnAction, "returnAction");
            Objects.requireNonNull(renewAction, "renewAction");
            Button returnButton = new Button("Return");
            returnButton.setOnAction(event -> returnAction.run());
            Button renewButton = new Button("Renew");
            renewButton.setDisable(loan.hasBeenRenewed() || loan.isOverdue());
            renewButton.setOnAction(event -> renewAction.run());
            HBox actions = new HBox(8, returnButton, renewButton);
            actions.setAlignment(Pos.CENTER_RIGHT);
            setRight(actions);
        }

        setPadding(new Insets(12));
        setStyle(loan.isActive() && loan.isOverdue() ? OVERDUE_CARD_STYLE : CARD_STYLE);
    }

    private void addDetail(GridPane details, String fieldName, String value, int row) {
        Label fieldLabel = new Label(fieldName + ":");
        fieldLabel.setStyle("-fx-font-weight: bold;");
        details.add(fieldLabel, 0, row);
        details.add(new Label(value), 1, row);
    }

    private String formatDate(java.time.LocalDate date) {
        return date == null ? "—" : DATE_FORMATTER.format(date);
    }

    private String getStatusText(Loan loan) {
        if (!loan.isActive()) {
            return "Returned";
        }
        return loan.isOverdue() ? "Overdue" : "Active";
    }
}
