package libconnect.ui.components;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import libconnect.models.Book;

/** Displays a randomly selected book recommendation from the catalogue. */
public final class RecommendationBanner extends VBox {
    private static final String EMPTY_CATALOGUE_MESSAGE = "no books in database";
    private static final String BANNER_STYLE = "-fx-background-color: #e8f1fb;"
            + " -fx-border-color: #b7cde5; -fx-border-radius: 6; -fx-background-radius: 6;";

    private final Label recommendationLabel;

    /** Creates an empty recommendation banner. */
    public RecommendationBanner() {
        Label heading = new Label("Book recommendation");
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        recommendationLabel = new Label();
        recommendationLabel.setWrapText(true);

        setSpacing(6);
        setPadding(new Insets(14));
        setStyle(BANNER_STYLE);
        getChildren().addAll(heading, recommendationLabel);
    }

    /**
     * Displays a random recommendation or the empty-catalogue placeholder.
     *
     * @param books the books from which the recommendation is selected.
     * @throws NullPointerException if {@code books} is null.
     */
    public void showRecommendation(List<Book> books) {
        Objects.requireNonNull(books, "books cannot be null");
        if (books.isEmpty()) {
            recommendationLabel.setText(EMPTY_CATALOGUE_MESSAGE);
            return;
        }

        Book recommendedBook = books.get(ThreadLocalRandom.current().nextInt(books.size()));
        recommendationLabel.setText("Try \"" + recommendedBook.getTitle() + "\" by "
                + recommendedBook.getAuthor() + " (" + recommendedBook.getPublicationYear() + ").");
    }
}
