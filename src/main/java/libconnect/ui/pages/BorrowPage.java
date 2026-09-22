package libconnect.ui.pages;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import libconnect.models.Book;
import libconnect.models.BookCopy;
import libconnect.models.CopyStatus;
import libconnect.models.Member;
import libconnect.models.User;
import libconnect.services.BookCopyService;
import libconnect.services.BookService;
import libconnect.services.BorrowService;
import libconnect.storage.repositories.RepositoryException;
import libconnect.ui.SceneNavigator;
import libconnect.ui.SessionManager;
import libconnect.ui.components.BorrowBookCard;
import libconnect.ui.components.DashboardNavbar;
import libconnect.ui.components.FeedbackMessage;
import libconnect.ui.components.PageHeader;

/** Provides the page for selecting and confirming a multi-copy borrowing transaction. */
public final class BorrowPage extends BorderPane {
    private static final int MAX_BOOKS = 10;
    private static final String STORAGE_ERROR_MESSAGE =
            "Unable to access book data. Please try again.";
    private static final String TRANSACTION_ERROR_MESSAGE =
            "Unable to complete the borrowing transaction. Please try again.";

    private final BookService bookService;
    private final BookCopyService bookCopyService;
    private final BorrowService borrowService;
    private final SessionManager sessionManager;
    private final SceneNavigator sceneNavigator;
    private final TextField copyIdField;
    private final Button addBookButton;
    private final Button confirmButton;
    private final Label countLabel;
    private final FeedbackMessage feedbackMessage;
    private final VBox bookList;
    private final Map<String, PendingBook> pendingBooks;

    /**
     * Creates a borrowing page backed by catalogue, copy, and transaction services.
     *
     * @param bookService the service used to load catalogue metadata.
     * @param bookCopyService the service used to look up physical copies.
     * @param borrowService the service used to complete borrowing transactions.
     * @param sessionManager the session containing the authenticated member.
     * @param sceneNavigator the navigator used for page transitions.
     * @throws NullPointerException if an argument is null.
     */
    public BorrowPage(BookService bookService, BookCopyService bookCopyService,
                      BorrowService borrowService, SessionManager sessionManager,
                      SceneNavigator sceneNavigator) {
        this.bookService = Objects.requireNonNull(bookService, "bookService");
        this.bookCopyService = Objects.requireNonNull(bookCopyService, "bookCopyService");
        this.borrowService = Objects.requireNonNull(borrowService, "borrowService");
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
        this.sceneNavigator = Objects.requireNonNull(sceneNavigator, "sceneNavigator");
        copyIdField = new TextField();
        addBookButton = new Button("Add Book");
        confirmButton = new Button("Confirm Borrowing");
        countLabel = new Label();
        feedbackMessage = new FeedbackMessage();
        bookList = new VBox(12);
        pendingBooks = new LinkedHashMap<>();

        configureControls();
        setTop(createNavbar());
        setCenter(createContent());
        updateControls();
    }

    private DashboardNavbar createNavbar() {
        return new DashboardNavbar(sceneNavigator::showDashboardPage,
                sceneNavigator::showBorrowPage, sceneNavigator::showMyLoansPage,
                sceneNavigator::showProfilePage, () -> {
            sessionManager.logout();
            sceneNavigator.showLoginPage();
        }, DashboardNavbar.ActivePage.BORROW);
    }

    private VBox createContent() {
        copyIdField.setPromptText("Enter book copy ID");
        copyIdField.setOnAction(event -> addBook());
        HBox inputRow = new HBox(10, copyIdField, addBookButton);
        HBox.setHgrow(copyIdField, Priority.ALWAYS);

        HBox confirmationRow = new HBox(confirmButton);
        confirmationRow.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(16,
                new PageHeader("Borrow Books", "Select up to 10 available book copies"),
                inputRow,
                feedbackMessage,
                countLabel,
                bookList,
                confirmationRow);
        content.setPadding(new Insets(20));
        VBox.setVgrow(bookList, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox wrapper = new VBox(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return wrapper;
    }

    private void configureControls() {
        addBookButton.setOnAction(event -> addBook());
        confirmButton.setOnAction(event -> confirmBorrowing());
    }

    private void addBook() {
        feedbackMessage.clearMessage();
        String copyId = copyIdField.getText().trim();
        if (copyId.isBlank()) {
            showError("Enter a book copy ID.");
            return;
        }
        if (pendingBooks.containsKey(copyId)) {
            showError("This book copy has already been added.");
            return;
        }
        if (pendingBooks.size() >= MAX_BOOKS) {
            showError("A transaction cannot contain more than 10 books.");
            return;
        }

        try {
            BookCopy copy = bookCopyService.getCopyById(copyId).orElse(null);
            if (copy == null) {
                showError("Book copy not found: " + copyId);
                return;
            }
            if (copy.getStatus() != CopyStatus.AVAILABLE) {
                showError("Book copy " + copyId + " cannot be borrowed because it is "
                        + copy.getStatus().toString().toLowerCase(Locale.ROOT) + ".");
                return;
            }

            Book book = bookService.getBookByIsbn(copy.getIsbn()).orElse(null);
            if (book == null) {
                showError("The catalogue book for copy " + copyId + " was not found.");
                return;
            }

            pendingBooks.put(copyId, new PendingBook(book, copy));
            copyIdField.clear();
            refreshBookList();
        } catch (RepositoryException exception) {
            showError(STORAGE_ERROR_MESSAGE);
        }
    }

    private void removeBook(String copyId) {
        pendingBooks.remove(copyId);
        feedbackMessage.clearMessage();
        refreshBookList();
    }

    private void confirmBorrowing() {
        feedbackMessage.clearMessage();
        if (pendingBooks.isEmpty()) {
            showError("Add at least one available book before confirming.");
            return;
        }

        User currentUser = sessionManager.getCurrentUser();
        if (!(currentUser instanceof Member member)) {
            showError("Only an authenticated member can borrow books.");
            return;
        }

        setControlsDisabled(true);
        try {
            borrowService.borrowCopies(member.getMembershipId(), pendingBooks.keySet().stream().toList(),
                    LocalDate.now());
            sceneNavigator.showDashboardPage("Successfully borrowed " + pendingBooks.size()
                    + " book(s).");
        } catch (RuntimeException exception) {
            setControlsDisabled(false);
            String message = exception.getMessage();
            showError(message == null || message.isBlank() ? TRANSACTION_ERROR_MESSAGE : message);
        }
    }

    private void refreshBookList() {
        bookList.getChildren().clear();
        pendingBooks.forEach((copyId, pendingBook) -> bookList.getChildren().add(
                new BorrowBookCard(pendingBook.book(), pendingBook.copy(),
                        () -> removeBook(copyId))));
        updateControls();
    }

    private void updateControls() {
        countLabel.setText(pendingBooks.size() + " / " + MAX_BOOKS + " books selected");
        addBookButton.setDisable(pendingBooks.size() >= MAX_BOOKS);
        confirmButton.setDisable(pendingBooks.isEmpty());
    }

    private void setControlsDisabled(boolean disabled) {
        copyIdField.setDisable(disabled);
        addBookButton.setDisable(disabled || pendingBooks.size() >= MAX_BOOKS);
        confirmButton.setDisable(disabled || pendingBooks.isEmpty());
        bookList.setDisable(disabled);
    }

    private void showError(String message) {
        feedbackMessage.showError(message);
    }

    private record PendingBook(Book book, BookCopy copy) {
    }
}
