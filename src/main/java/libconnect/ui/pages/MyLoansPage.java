package libconnect.ui.pages;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import libconnect.models.Book;
import libconnect.models.BookCopy;
import libconnect.models.Loan;
import libconnect.models.Member;
import libconnect.models.User;
import libconnect.services.BookCopyService;
import libconnect.services.BookService;
import libconnect.services.BorrowService;
import libconnect.services.LoanService;
import libconnect.storage.repositories.RepositoryException;
import libconnect.ui.SceneNavigator;
import libconnect.ui.SessionManager;
import libconnect.ui.components.Navbar;
import libconnect.ui.components.FeedbackMessage;
import libconnect.ui.components.LoanCard;
import libconnect.ui.components.PageHeader;

/** Provides the authenticated member's current loans and loan history. */
public final class MyLoansPage extends BorderPane {
    private static final String STORAGE_ERROR_MESSAGE =
            "Unable to access loan data. Please try again.";
    private static final String ACTION_ERROR_MESSAGE =
            "Unable to update the loan. Please try again.";
    private final LoanService loanService;
    private final BookService bookService;
    private final BookCopyService bookCopyService;
    private final BorrowService borrowService;
    private final SessionManager sessionManager;
    private final VBox loanList;
    private final FeedbackMessage feedbackMessage;
    private final Button currentLoansButton;
    private final Button loanHistoryButton;
    private LoanView selectedView;

    private enum LoanView {
        CURRENT,
        HISTORY
    }

    /**
     * Creates a my loans page backed by the supplied services and session.
     *
     * @param loanService the service used to load and renew loans.
     * @param bookService the service used to load catalogue metadata.
     * @param bookCopyService the service used to load physical copy metadata.
     * @param borrowService the service used to return loans and copies together.
     * @param sessionManager the session containing the authenticated member.
     * @param sceneNavigator the navigator used for page transitions.
     * @throws NullPointerException if an argument is null.
     */
    public MyLoansPage(LoanService loanService, BookService bookService,
                       BookCopyService bookCopyService, BorrowService borrowService,
                       SessionManager sessionManager, SceneNavigator sceneNavigator) {
        this.loanService = Objects.requireNonNull(loanService, "loanService");
        this.bookService = Objects.requireNonNull(bookService, "bookService");
        this.bookCopyService = Objects.requireNonNull(bookCopyService, "bookCopyService");
        this.borrowService = Objects.requireNonNull(borrowService, "borrowService");
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
        Objects.requireNonNull(sceneNavigator, "sceneNavigator");
        loanList = new VBox(12);
        feedbackMessage = new FeedbackMessage();
        currentLoansButton = new Button("Current Loans");
        loanHistoryButton = new Button("Loan History");
        selectedView = LoanView.CURRENT;

        configureControls();
        setTop(createNavbar(sceneNavigator));
        setCenter(createContent());
        refreshLoans();
    }

    private Navbar createNavbar(SceneNavigator sceneNavigator) {
        return new Navbar(sceneNavigator, sessionManager.getCurrentUser(), Navbar.ActivePage.MY_LOANS, () -> {
            sessionManager.logout();
            sceneNavigator.showLoginPage();
        });
    }

    private VBox createContent() {
        HBox tabs = new HBox(8, currentLoansButton, loanHistoryButton);
        tabs.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(16,
                new PageHeader("My Loans", "Manage your current loans and view your loan history"),
                tabs,
                feedbackMessage,
                loanList);
        content.setPadding(new Insets(20));
        VBox.setVgrow(loanList, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox wrapper = new VBox(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return wrapper;
    }

    private void configureControls() {
        currentLoansButton.setOnAction(event -> showView(LoanView.CURRENT));
        loanHistoryButton.setOnAction(event -> showView(LoanView.HISTORY));
        updateTabStyles();
    }

    private void showView(LoanView view) {
        selectedView = view;
        updateTabStyles();
        refreshLoans();
    }

    private void updateTabStyles() {
        currentLoansButton.setDisable(selectedView == LoanView.CURRENT);
        loanHistoryButton.setDisable(selectedView == LoanView.HISTORY);
        updateTabStyle(currentLoansButton, selectedView == LoanView.CURRENT);
        updateTabStyle(loanHistoryButton, selectedView == LoanView.HISTORY);
    }

    private void updateTabStyle(Button button, boolean isSelected) {
        if (isSelected) {
            if (!button.getStyleClass().contains("selected-tab")) {
                button.getStyleClass().add("selected-tab");
            }
        } else {
            button.getStyleClass().remove("selected-tab");
        }
    }

    private void refreshLoans() {
        loanList.getChildren().clear();
        User currentUser = sessionManager.getCurrentUser();
        if (!(currentUser instanceof Member member)) {
            showError("Only an authenticated member can view loans.");
            return;
        }

        try {
            List<Loan> loans = loanService.getLoansByMemberId(member.getMembershipId());
            List<Loan> visibleLoans = selectedView == LoanView.CURRENT
                    ? getCurrentLoans(loans)
                    : getLoanHistory(loans);
            if (visibleLoans.isEmpty()) {
                loanList.getChildren().add(new Label(selectedView == LoanView.CURRENT
                        ? "You have no current loans."
                        : "You have no loan history."));
                return;
            }

            visibleLoans.stream()
                    .map(this::createLoanCard)
                    .forEach(loanList.getChildren()::add);
        } catch (RepositoryException exception) {
            showError(STORAGE_ERROR_MESSAGE);
        }
    }

    private List<Loan> getCurrentLoans(List<Loan> loans) {
        return loans.stream()
                .filter(Loan::isActive)
                .sorted(Comparator.comparing(Loan::getDueDate))
                .toList();
    }

    private List<Loan> getLoanHistory(List<Loan> loans) {
        return loans.stream()
                .filter(loan -> !loan.isActive())
                .sorted(Comparator.comparing(Loan::getReturnDate).reversed())
                .toList();
    }

    private LoanCard createLoanCard(Loan loan) {
        BookCopy copy = bookCopyService.getCopyById(loan.getCopyId()).orElse(null);
        Book book = copy == null ? null : bookService.getBookByIsbn(copy.getIsbn()).orElse(null);
        String title = book == null ? "Book information unavailable" : book.getTitle();
        String author = book == null ? "—" : book.getAuthor();
        String isbn = copy == null ? "—" : copy.getIsbn();
        boolean showActions = selectedView == LoanView.CURRENT;
        return new LoanCard(loan, title, author, isbn, showActions,
                () -> returnLoan(loan), () -> renewLoan(loan));
    }

    private void returnLoan(Loan loan) {
        try {
            borrowService.returnLoan(loan.getLoanId());
            feedbackMessage.showSuccess("The book was returned successfully.");
            refreshLoans();
        } catch (RuntimeException exception) {
            showError(getActionErrorMessage(exception));
        }
    }

    private void renewLoan(Loan loan) {
        try {
            loanService.renewLoan(loan.getLoanId());
            feedbackMessage.showSuccess("The loan was renewed successfully.");
            refreshLoans();
        } catch (RuntimeException exception) {
            showError(getActionErrorMessage(exception));
        }
    }

    private String getActionErrorMessage(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? ACTION_ERROR_MESSAGE : exception.getMessage();
    }

    private void showError(String message) {
        feedbackMessage.showError(message);
    }
}
