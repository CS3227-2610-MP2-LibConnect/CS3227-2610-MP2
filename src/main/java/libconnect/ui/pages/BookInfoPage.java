package libconnect.ui.pages;

import java.util.List;
import java.util.Objects;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import libconnect.models.Book;
import libconnect.models.BookCopy;
import libconnect.models.CopyStatus;
import libconnect.services.BookCopyService;
import libconnect.services.BookService;
import libconnect.storage.repositories.RepositoryException;
import libconnect.ui.SceneNavigator;
import libconnect.ui.SessionManager;
import libconnect.ui.components.DashboardNavbar;
import libconnect.ui.components.PageHeader;

/** Displays catalogue metadata and visible physical copies for one book. */
public final class BookInfoPage extends VBox {
    private static final String STORAGE_ERROR_MESSAGE =
            "Unable to access book data. Please try again.";

    private final BookService bookService;
    private final BookCopyService bookCopyService;
    private final String isbn;
    private final VBox content;

    /**
     * Creates a book information page backed by catalogue and copy services.
     *
     * @param bookService the service used to load book metadata.
     * @param bookCopyService the service used to load physical copies.
     * @param sessionManager the session containing the authenticated user.
     * @param isbn the ISBN of the book to display.
     * @param sceneNavigator the navigator used by the back button.
     */
    public BookInfoPage(BookService bookService, BookCopyService bookCopyService,
                        SessionManager sessionManager, String isbn, SceneNavigator sceneNavigator) {
        this.bookService = Objects.requireNonNull(bookService, "bookService");
        this.bookCopyService = Objects.requireNonNull(bookCopyService, "bookCopyService");
        Objects.requireNonNull(sessionManager, "sessionManager");
        this.isbn = Objects.requireNonNull(isbn, "isbn");
        Objects.requireNonNull(sceneNavigator, "sceneNavigator");
        content = new VBox(16);
        content.setPadding(new Insets(20));

        DashboardNavbar navbar = new DashboardNavbar(() -> {
            sessionManager.logout();
            sceneNavigator.showLoginPage();
        });
        Button backButton = new Button("Back to Dashboard");
        backButton.setOnAction(event -> sceneNavigator.showDashboardPage());
        HBox navigation = new HBox(backButton);
        navigation.setAlignment(Pos.CENTER_LEFT);
        navigation.setPadding(new Insets(12, 20, 0, 20));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        setSpacing(0);
        getChildren().addAll(navbar, navigation, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        loadBookDetails();
    }

    /** Loads the selected book and its visible copies into the page. */
    private void loadBookDetails() {
        try {
            Book book = bookService.getBookByIsbn(isbn).orElse(null);
            if (book == null) {
                showMessage("Book not found.");
                return;
            }

            content.getChildren().add(new PageHeader(book.getTitle(),
                    "Book information and copy availability"));
            content.getChildren().add(createBookDetails(book));
            content.getChildren().add(createCopySection(bookCopyService.getCopiesByIsbn(isbn)));
        } catch (RepositoryException exception) {
            showMessage(STORAGE_ERROR_MESSAGE);
        }
    }

    /**
     * Creates the metadata grid for the selected book.
     *
     * @param book the book whose metadata should be displayed.
     * @return the metadata grid.
     */
    private GridPane createBookDetails(Book book) {
        GridPane details = new GridPane();
        details.setHgap(16);
        details.setVgap(8);
        addBookField(details, "Title", book.getTitle(), 0);
        addBookField(details, "Author", book.getAuthor(), 1);
        addBookField(details, "ISBN", book.getIsbn(), 2);
        addBookField(details, "Publication year", String.valueOf(book.getPublicationYear()), 3);
        addBookField(details, "Publisher", book.getPublisher(), 4);
        addBookField(details, "Category", book.getCategory(), 5);
        return details;
    }

    /**
     * Adds one labelled metadata field to the book details grid.
     *
     * @param details the grid receiving the field.
     * @param fieldName the displayed field name.
     * @param value the displayed field value.
     * @param row the target row.
     */
    private void addBookField(GridPane details, String fieldName, String value, int row) {
        Label fieldLabel = new Label(fieldName + ":");
        fieldLabel.setStyle("-fx-font-weight: bold;");
        details.add(fieldLabel, 0, row);
        details.add(new Label(value), 1, row);
    }

    /**
     * Creates the visible-copy section, excluding lost and damaged copies.
     *
     * @param copies all copies associated with the selected book.
     * @return the visible-copy section.
     */
    private VBox createCopySection(List<BookCopy> copies) {
        VBox copySection = new VBox(8);
        Label heading = new Label("Copies");
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        copySection.getChildren().add(heading);

        List<BookCopy> visibleCopies = copies.stream()
                .filter(this::isVisibleCopy)
                .toList();
        if (visibleCopies.isEmpty()) {
            copySection.getChildren().add(new Label("No available or borrowed copies."));
            return copySection;
        }

        visibleCopies.stream()
                .map(this::createCopyRow)
                .forEach(copySection.getChildren()::add);
        return copySection;
    }

    /**
     * Creates a row describing one available or borrowed copy.
     *
     * @param copy the copy represented by the row.
     * @return the copy row.
     */
    private Label createCopyRow(BookCopy copy) {
        String availability = copy.getStatus() == CopyStatus.AVAILABLE
                ? "Shelf location: " + copy.getShelfLocation()
                : "Borrowed";
        return new Label(copy.getCopyId() + " — " + availability);
    }

    /**
     * Returns whether a copy should be visible to catalogue users.
     *
     * @param copy the copy to inspect.
     * @return true if the copy is available or borrowed.
     */
    private boolean isVisibleCopy(BookCopy copy) {
        return copy.getStatus() == CopyStatus.AVAILABLE
                || copy.getStatus() == CopyStatus.BORROWED;
    }

    /** Displays a message when the requested book cannot be rendered. */
    private void showMessage(String message) {
        content.getChildren().clear();
        content.getChildren().add(new Label(message));
    }
}
