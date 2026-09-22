package libconnect.ui.components;

import java.util.Objects;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import libconnect.models.Book;
import libconnect.models.BookCopy;

/** Displays the catalogue and copy details for one pending borrowing selection. */
public final class BorrowBookCard extends BorderPane {
    private static final String CARD_STYLE = "-fx-border-color: #d0d7de;"
            + " -fx-border-radius: 6; -fx-background-radius: 6;"
            + " -fx-background-color: white;";

    private final String copyId;

    /**
     * Creates a card for a selected book copy.
     *
     * @param book the catalogue book associated with the copy.
     * @param copy the selected physical book copy.
     * @param removeAction the action run when the remove button is pressed.
     * @throws NullPointerException if an argument is null.
     */
    public BorrowBookCard(Book book, BookCopy copy, Runnable removeAction) {
        Objects.requireNonNull(book, "book");
        Objects.requireNonNull(copy, "copy");
        Objects.requireNonNull(removeAction, "removeAction");
        copyId = copy.getCopyId();

        GridPane details = new GridPane();
        details.setHgap(16);
        details.setVgap(5);
        addDetail(details, "Title", book.getTitle(), 0);
        addDetail(details, "Author", book.getAuthor(), 1);
        addDetail(details, "ISBN", book.getIsbn(), 2);
        addDetail(details, "Publisher", book.getPublisher(), 3);
        addDetail(details, "Category", book.getCategory(), 4);
        addDetail(details, "Publication year", String.valueOf(book.getPublicationYear()), 5);
        addDetail(details, "Copy ID", copy.getCopyId(), 6);
        addDetail(details, "Status", copy.getStatus().toString(), 7);
        addDetail(details, "Shelf location", copy.getShelfLocation(), 8);

        Label heading = new Label(book.getTitle());
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        VBox content = new VBox(8, heading, details);

        Button removeButton = new Button("Remove");
        removeButton.setOnAction(event -> removeAction.run());
        BorderPane.setAlignment(removeButton, Pos.CENTER_RIGHT);

        setCenter(content);
        setRight(removeButton);
        setPadding(new Insets(12));
        setStyle(CARD_STYLE);
    }

    /** Returns the identifier of the physical copy represented by this card. */
    public String getCopyId() {
        return copyId;
    }

    private void addDetail(GridPane details, String fieldName, String value, int row) {
        Label fieldLabel = new Label(fieldName + ":");
        fieldLabel.setStyle("-fx-font-weight: bold;");
        details.add(fieldLabel, 0, row);
        details.add(new Label(value), 1, row);
    }
}
