package libconnect.ui.components;

import java.util.List;
import java.util.Objects;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import libconnect.models.Book;

/** Displays catalogue search results in a scrollable list of book cards. */
public final class BookListView extends ScrollPane {
    private final VBox bookCards;

    /** Creates an empty scrollable book list. */
    public BookListView() {
        bookCards = new VBox(10);
        bookCards.setPadding(new Insets(4));
        setContent(bookCards);
        setFitToWidth(true);
        setPrefHeight(320);
    }

    /**
     * Replaces the displayed results with the supplied books.
     *
     * @param books the books to display.
     * @throws NullPointerException if {@code books} is null.
     */
    public void showBooks(List<Book> books) {
        Objects.requireNonNull(books, "books cannot be null");
        bookCards.getChildren().clear();
        if (books.isEmpty()) {
            bookCards.getChildren().add(new Label("No books match your search."));
            return;
        }

        books.stream().map(this::createBookCard).forEach(bookCards.getChildren()::add);
    }

    /**
     * Creates a visual card for one book.
     *
     * @param book the book represented by the card.
     * @return the book card.
     */
    private VBox createBookCard(Book book) {
        Label titleLabel = new Label(book.getTitle());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label detailsLabel = new Label("Author: " + book.getAuthor() + " | ISBN: "
                + book.getIsbn() + " | Published: " + book.getPublicationYear());
        Label catalogueLabel = new Label("Publisher: " + book.getPublisher() + " | Category: "
                + book.getCategory());

        VBox card = new VBox(4, titleLabel, detailsLabel, catalogueLabel);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #dddddd;"
                + " -fx-border-radius: 4; -fx-background-radius: 4;");
        return card;
    }
}
