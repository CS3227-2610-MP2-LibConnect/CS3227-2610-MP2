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

import libconnect.models.Fine;

/** Displays the details and available action for one member fine. */
public final class FineCard extends BorderPane {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    /**
     * Creates a fine card.
     *
     * @param fine the fine represented by the card.
     * @param bookTitle the title of the book associated with the fine.
     * @param showPayAction whether a Pay Fine button should be displayed.
     * @param payAction the action run when Pay Fine is pressed.
     * @throws NullPointerException if a required argument is null.
     */
    public FineCard(Fine fine, String bookTitle, boolean showPayAction, Runnable payAction) {
        Objects.requireNonNull(fine, "fine");
        Objects.requireNonNull(bookTitle, "bookTitle");

        GridPane details = new GridPane();
        details.setHgap(16);
        details.setVgap(5);
        addDetail(details, "Book", bookTitle, 0);
        addDetail(details, "Amount", "$" + fine.getAmount().toPlainString(), 1);
        addDetail(details, "Reason", fine.getReason(), 2);
        addDetail(details, "Issued", DATE_FORMATTER.format(fine.getIssuedDate()), 3);
        addDetail(details, "Status", fine.getStatus().toString(), 4);

        Label heading = new Label("Fine " + fine.getId());
        heading.getStyleClass().add("card-heading");
        VBox content = new VBox(8, heading, details);
        setCenter(content);

        if (showPayAction) {
            Objects.requireNonNull(payAction, "payAction");
            Button payButton = new Button("Pay Fine");
            payButton.getStyleClass().add("button-success");
            payButton.setOnAction(event -> payAction.run());
            HBox actions = new HBox(payButton);
            actions.setAlignment(Pos.CENTER_RIGHT);
            setRight(actions);
        }

        setPadding(new Insets(12));
        getStyleClass().add("fine-card");
    }

    private void addDetail(GridPane details, String fieldName, String value, int row) {
        Label fieldLabel = new Label(fieldName + ":");
        fieldLabel.getStyleClass().add("bold-label");
        details.add(fieldLabel, 0, row);
        details.add(new Label(value), 1, row);
    }
}
