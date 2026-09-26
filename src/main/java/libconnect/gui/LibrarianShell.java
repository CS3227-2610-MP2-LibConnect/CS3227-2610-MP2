package libconnect.gui;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Function;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import libconnect.app.LibrarianRuntime;
import libconnect.integration.BookDetails;
import libconnect.integration.BookSummary;
import libconnect.integration.LoanSummary;
import libconnect.integration.MemberSummary;
import libconnect.models.Fine;
import libconnect.models.Librarian;
import libconnect.models.Notification;
import libconnect.models.Reservation;

/** Provides the single-window librarian workspace and its feature panels. */
public final class LibrarianShell extends BorderPane {
    private final LibrarianRuntime runtime;
    private final JavaFxLibrarianView view;
    private final String employeeId;
    private final Runnable onLogout;
    private final Label statusLabel = new Label();

    /** Creates an authorized librarian shell with simple navigable feature panels. */
    public LibrarianShell(LibrarianRuntime runtime, JavaFxLibrarianView view,
                          String employeeId, Runnable onLogout) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.view = Objects.requireNonNull(view, "view");
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId");
        this.onLogout = Objects.requireNonNull(onLogout, "onLogout");
        setId("librarian-shell");
        view.bind(statusLabel);

        setTop(createHeader());
        setCenter(createNavigation());
        setBottom(statusLabel);
        BorderPane.setMargin(statusLabel, new Insets(8));
    }

    private Node createHeader() {
        Librarian librarian = runtime.librarianService().requireActive(employeeId);
        Label title = new Label("LibConnect Librarian");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        Label user = new Label("Signed in: " + librarian.getName());
        Button logout = new Button("Log out");
        logout.setId("logout-button");
        logout.setOnAction(event -> onLogout.run());
        HBox header = new HBox(16, title, user, logout);
        header.setPadding(new Insets(12));
        HBox.setHgrow(user, Priority.ALWAYS);
        return header;
    }

    private TabPane createNavigation() {
        TabPane navigation = new TabPane();
        navigation.setId("librarian-navigation");
        navigation.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        navigation.getTabs().addAll(
                tab("Dashboard", createDashboard()),
                tab("Members", createMembers()),
                tab("Books", createBooks()),
                tab("Loans", createLoans()),
                tab("Reservations", createReservations()),
                tab("Fines", createFines()),
                tab("Notifications", createNotifications()));
        return navigation;
    }

    private Tab tab(String title, Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private Node createDashboard() {
        Label summary = new Label("Select refresh to load current counts.");
        Button refresh = new Button("Refresh summary");
        refresh.setOnAction(event -> execute(() -> {
            int activeLoans = runtime.controller().viewLoans(employeeId).size();
            int overdueLoans = runtime.controller().viewOverdueLoans(employeeId).size();
            int reservations = runtime.controller().viewAllReservations(employeeId).size();
            int fines = runtime.controller().viewAllFines(employeeId).size();
            summary.setText("Active loans: " + activeLoans + " | Overdue: " + overdueLoans
                    + " | Reservations: " + reservations + " | Fines: " + fines);
            view.showMessage("Dashboard refreshed");
        }));
        return padded(new VBox(12, new Label("Librarian dashboard"), summary, refresh));
    }

    private Node createMembers() {
        TextField query = new TextField();
        query.setPromptText("Search by ID, name, or email");
        TableView<MemberSummary> table = new TableView<>();
        table.getColumns().addAll(
                textColumn("ID", MemberSummary::getMemberId),
                textColumn("Name", MemberSummary::getName),
                textColumn("Email", MemberSummary::getEmail),
                textColumn("Active", member -> Boolean.toString(member.isActive())));
        Button search = new Button("Search");
        search.setId("members-search");
        search.setOnAction(event -> execute(() -> table.setItems(FXCollections.observableArrayList(
                runtime.controller().searchMembers(employeeId, query.getText())))));
        Button register = new Button("Register");
        register.setOnAction(event -> promptMember("Register member", null));
        Button edit = new Button("Edit selected");
        edit.setOnAction(event -> {
            MemberSummary selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                view.showError("Select a member first");
            } else {
                promptMember("Edit member", selected);
            }
        });
        Button deactivate = new Button("Deactivate selected");
        deactivate.setOnAction(event -> execute(() -> {
            MemberSummary selected = requireSelection(table, "member");
            runtime.controller().deactivateMember(employeeId, selected.getMemberId());
            view.showMessage("Member deactivated");
            search.fire();
        }));
        return padded(new VBox(10, new HBox(8, query, search, register, edit, deactivate), table));
    }

    private void promptMember(String title, MemberSummary selected) {
        String memberId = selected == null ? prompt(title, "Member ID", "") : selected.getMemberId();
        String name = prompt(title, "Name", selected == null ? "" : selected.getName());
        String email = prompt(title, "Email", selected == null ? "" : selected.getEmail());
        if (memberId == null || name == null || email == null) {
            return;
        }
        execute(() -> {
            if (selected == null) {
                runtime.controller().registerMember(employeeId, memberId, name, email);
                view.showMessage("Member registered");
            } else {
                runtime.controller().editMember(employeeId, memberId, name, email);
                view.showMessage("Member updated");
            }
        });
    }

    private Node createBooks() {
        TextField query = new TextField();
        query.setPromptText("Search by ID, title, author, or category");
        TableView<BookSummary> table = new TableView<>();
        table.getColumns().addAll(
                textColumn("ID", BookSummary::bookId),
                textColumn("Title", book -> book.details().title()),
                textColumn("Author", book -> book.details().author()),
                textColumn("Available", book -> Integer.toString(book.availableCopies())));
        Button search = new Button("Search");
        search.setOnAction(event -> execute(() -> table.setItems(FXCollections.observableArrayList(
                runtime.controller().searchBooks(employeeId, query.getText())))));
        Button add = new Button("Add");
        add.setOnAction(event -> promptBook(null));
        Button edit = new Button("Edit selected");
        edit.setOnAction(event -> {
            BookSummary selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                view.showError("Select a book first");
            } else {
                promptBook(selected);
            }
        });
        Button remove = new Button("Remove selected");
        remove.setOnAction(event -> execute(() -> {
            BookSummary selected = requireSelection(table, "book");
            runtime.controller().removeBook(employeeId, selected.bookId());
            view.showMessage("Book removed");
            search.fire();
        }));
        TextField copyId = new TextField();
        copyId.setPromptText("Copy ID");
        Button damaged = new Button("Mark damaged");
        damaged.setOnAction(event -> execute(() -> {
            runtime.controller().recordDamagedBook(employeeId, copyId.getText());
            view.showMessage("Copy recorded as damaged");
        }));
        Button lost = new Button("Mark lost");
        lost.setOnAction(event -> execute(() -> {
            runtime.controller().recordLostBook(employeeId, copyId.getText());
            view.showMessage("Copy recorded as lost");
        }));
        return padded(new VBox(10, new HBox(8, query, search, add, edit, remove), table,
                new HBox(8, copyId, damaged, lost)));
    }

    private void promptBook(BookSummary selected) {
        String isbn = prompt("Book details", "ISBN", selected == null ? "" : selected.details().isbn());
        String title = prompt("Book details", "Title", selected == null ? "" : selected.details().title());
        String author = prompt("Book details", "Author", selected == null ? "" : selected.details().author());
        String publisher = prompt("Book details", "Publisher",
                selected == null ? "" : selected.details().publisher());
        String category = prompt("Book details", "Category", selected == null ? "" : selected.details().category());
        String year = prompt("Book details", "Publication year",
                selected == null ? "" : Integer.toString(selected.details().publicationYear()));
        if (isbn == null || title == null || author == null || publisher == null || category == null || year == null) {
            return;
        }
        execute(() -> {
            BookDetails details = new BookDetails(isbn, title, author, publisher, category, Integer.parseInt(year));
            if (selected == null) {
                runtime.controller().addBook(employeeId, details);
                view.showMessage("Book added");
            } else {
                runtime.controller().editBook(employeeId, selected.bookId(), details);
                view.showMessage("Book updated");
            }
        });
    }

    private Node createLoans() {
        TableView<LoanSummary> table = new TableView<>();
        table.getColumns().addAll(
                textColumn("Loan ID", LoanSummary::getLoanId),
                textColumn("Member", LoanSummary::getMemberId),
                textColumn("Copy", LoanSummary::getBookCopyId),
                textColumn("Due date", loan -> loan.getDueDate().toString()));
        Button active = new Button("Active loans");
        active.setOnAction(event -> execute(() -> table.setItems(FXCollections.observableArrayList(
                runtime.controller().viewLoans(employeeId)))));
        Button overdue = new Button("Overdue loans");
        overdue.setOnAction(event -> execute(() -> table.setItems(FXCollections.observableArrayList(
                runtime.controller().viewOverdueLoans(employeeId)))));
        Button alert = new Button("Alert selected member");
        alert.setOnAction(event -> execute(() -> {
            LoanSummary selected = requireSelection(table, "loan");
            runtime.controller().sendOverdueAlert(employeeId, selected.getLoanId());
        }));
        return padded(new VBox(10, new HBox(8, active, overdue, alert), table));
    }

    private Node createReservations() {
        TextField memberId = new TextField();
        memberId.setPromptText("Member ID");
        TextField bookId = new TextField();
        bookId.setPromptText("Book ID");
        TableView<Reservation> table = new TableView<>();
        table.getColumns().addAll(
                textColumn("ID", Reservation::getId),
                textColumn("Member", Reservation::getMemberId),
                textColumn("Book", Reservation::getBookId),
                textColumn("Status", reservation -> reservation.getStatus().toString()));
        Button list = new Button("List");
        list.setOnAction(event -> execute(() -> table.setItems(FXCollections.observableArrayList(
                memberId.getText().isBlank()
                        ? runtime.controller().viewAllReservations(employeeId)
                        : runtime.controller().viewReservations(employeeId, memberId.getText())))));
        Button create = new Button("Create");
        create.setOnAction(event -> execute(() -> {
            runtime.controller().createReservation(employeeId, memberId.getText(), bookId.getText());
            view.showMessage("Reservation created");
            list.fire();
        }));
        Button pending = new Button("Pending for book");
        pending.setOnAction(event -> execute(() -> table.setItems(FXCollections.observableArrayList(
                runtime.controller().viewPendingReservations(employeeId, bookId.getText())))));
        Button cancel = new Button("Cancel selected");
        cancel.setOnAction(event -> execute(() -> {
            Reservation selected = requireSelection(table, "reservation");
            runtime.controller().cancelReservation(employeeId, selected.getId());
            view.showMessage("Reservation cancelled");
            list.fire();
        }));
        Button fulfil = new Button("Fulfil selected");
        fulfil.setOnAction(event -> execute(() -> {
            Reservation selected = requireSelection(table, "reservation");
            runtime.controller().fulfilReservation(employeeId, selected.getId());
            view.showMessage("Reservation fulfilled");
            list.fire();
        }));
        Button reminder = new Button("Send reminder");
        reminder.setOnAction(event -> execute(() -> {
            Reservation selected = requireSelection(table, "reservation");
            runtime.controller().sendReservationReminder(employeeId, selected.getId());
        }));
        return padded(new VBox(10, new HBox(8, memberId, bookId),
                new HBox(8, list, create, pending, cancel, fulfil, reminder), table));
    }

    private Node createFines() {
        TextField memberId = new TextField();
        memberId.setPromptText("Member ID");
        TextField loanId = new TextField();
        loanId.setPromptText("Loan ID for new fine");
        TableView<Fine> table = new TableView<>();
        table.getColumns().addAll(
                textColumn("ID", Fine::getId),
                textColumn("Loan", Fine::getLoanId),
                textColumn("Member", Fine::getMemberId),
                textColumn("Amount", fine -> fine.getAmount().toString()),
                textColumn("Status", fine -> fine.getStatus().toString()));
        Button list = new Button("List member fines");
        list.setOnAction(event -> execute(() -> table.setItems(FXCollections.observableArrayList(
                runtime.controller().viewFines(employeeId, memberId.getText())))));
        Button listAll = new Button("List all fines");
        listAll.setOnAction(event -> execute(() -> table.setItems(FXCollections.observableArrayList(
                runtime.controller().viewAllFines(employeeId)))));
        Button create = new Button("Create from loan");
        create.setOnAction(event -> execute(() -> {
            runtime.controller().createFine(employeeId, loanId.getText());
            view.showMessage("Fine created");
            listAll.fire();
        }));
        Button edit = new Button("Edit selected");
        edit.setOnAction(event -> execute(() -> {
            Fine selected = requireSelection(table, "fine");
            String amount = prompt("Edit fine", "Amount", selected.getAmount().toString());
            if (amount != null) {
                runtime.controller().editFine(employeeId, selected.getId(), new BigDecimal(amount));
                view.showMessage("Fine updated");
                listAll.fire();
            }
        }));
        Button remove = new Button("Remove selected");
        remove.setOnAction(event -> execute(() -> {
            Fine selected = requireSelection(table, "fine");
            runtime.controller().removeFine(employeeId, selected.getId());
            view.showMessage("Fine removed");
            listAll.fire();
        }));
        return padded(new VBox(10, new HBox(8, memberId, loanId),
                new HBox(8, list, listAll, create, edit, remove), table));
    }

    private Node createNotifications() {
        TextField userId = new TextField();
        userId.setPromptText("User/member ID");
        TableView<Notification> table = new TableView<>();
        table.getColumns().addAll(
                textColumn("ID", Notification::getId),
                textColumn("Type", notification -> notification.getType().toString()),
                textColumn("Read", notification -> Boolean.toString(notification.isRead())),
                textColumn("Message", Notification::getMessage));
        Button list = new Button("List");
        list.setOnAction(event -> execute(() -> table.setItems(FXCollections.observableArrayList(
                runtime.controller().viewNotifications(employeeId, userId.getText())))));
        Button read = new Button("Mark selected read");
        read.setOnAction(event -> execute(() -> {
            Notification selected = requireSelection(table, "notification");
            runtime.controller().markNotificationRead(employeeId, selected.getId());
            view.showMessage("Notification marked read");
            list.fire();
        }));
        return padded(new VBox(10, new HBox(8, userId, list, read), table));
    }

    private void execute(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            runtime.controller().showError(exception);
        }
    }

    private <T> T requireSelection(TableView<T> table, String itemName) {
        T selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            throw new IllegalArgumentException("Select a " + itemName + " first");
        }
        return selected;
    }

    private String prompt(String title, String header, String initialValue) {
        TextInputDialog dialog = new TextInputDialog(initialValue);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        return dialog.showAndWait().orElse(null);
    }

    private Node padded(Node node) {
        VBox container = new VBox(node);
        container.setPadding(new Insets(12));
        VBox.setVgrow(node, Priority.ALWAYS);
        return container;
    }

    private static <T> TableColumn<T, String> textColumn(String title, Function<T, String> valueExtractor) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(valueExtractor.apply(cell.getValue())));
        column.setPrefWidth(150);
        return column;
    }
}
