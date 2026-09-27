package libconnect.ui.pages;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import libconnect.integration.BookDetails;
import libconnect.integration.BookSummary;
import libconnect.integration.LoanSummary;
import libconnect.integration.MemberSummary;
import libconnect.models.BookCopy;
import libconnect.models.CopyStatus;
import libconnect.models.Fine;
import libconnect.models.Notification;
import libconnect.models.Reservation;
import libconnect.ui.LibrarianRuntime;
import libconnect.ui.components.JavaFxLibrarianView;

/** Provides the authorized librarian workspace and its feature panels. */
public final class LibrarianPage extends BorderPane {
    private final LibrarianRuntime runtime;
    private final JavaFxLibrarianView view;
    private final String employeeId;
    private final Runnable onLogout;
    private final Label statusLabel = new Label();

    /** Creates an authorized librarian page for the supplied employee ID. */
    public LibrarianPage(LibrarianRuntime runtime, JavaFxLibrarianView view,
                         String employeeId, Runnable onLogout) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.view = Objects.requireNonNull(view, "view");
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId");
        this.onLogout = Objects.requireNonNull(onLogout, "onLogout");
        setId("librarian-page");
        view.bind(statusLabel);

        setTop(createHeader());
        setCenter(createNavigation());
        setBottom(statusLabel);
        BorderPane.setMargin(statusLabel, new Insets(8));
    }

    private Node createHeader() {
        String librarianName = runtime.librarianService().requireActive(employeeId).getName();
        Label title = new Label("LibConnect Librarian");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        Label user = new Label("Signed in: " + librarianName);
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
                tab("BookCopy", createBookCopies()),
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
        refresh.fire();
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
        register.setOnAction(event -> promptMemberRegistration(() -> search.fire()));
        Button edit = new Button("Edit selected");
        edit.setOnAction(event -> {
            MemberSummary selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                view.showError("Select a member first");
            } else {
                promptMember("Edit member", selected, () -> search.fire());
            }
        });
        Button resetPassword = new Button("Reset password");
        resetPassword.setOnAction(event -> {
            MemberSummary selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                view.showError("Select a member first");
            } else {
                promptMemberPasswordReset(selected);
            }
        });
        Button deactivate = new Button("Deactivate selected");
        deactivate.setOnAction(event -> execute(() -> {
            MemberSummary selected = requireSelection(table, "member");
            runtime.controller().deactivateMember(employeeId, selected.getMemberId());
            view.showMessage("Member deactivated");
            search.fire();
        }));
        Button activate = new Button("Activate selected");
        activate.setOnAction(event -> execute(() -> {
            MemberSummary selected = requireSelection(table, "member");
            runtime.controller().activateMember(employeeId, selected.getMemberId());
            view.showMessage("Member activated");
            search.fire();
        }));
        search.fire();
        return padded(new VBox(10,
                new HBox(8, query, search, register, edit, resetPassword, deactivate, activate), table));
    }

    /** Opens one form dialog for all fields required to register a member. */
    private void promptMemberRegistration(Runnable refreshMembers) {
        TextField name = new TextField();
        TextField email = new TextField();
        PasswordField password = new PasswordField();
        PasswordField confirmPassword = new PasswordField();

        name.setPromptText("Full name");
        email.setPromptText("Email address");
        password.setPromptText("Password");
        confirmPassword.setPromptText("Confirm password");

        GridPane fields = new GridPane();
        fields.setHgap(10);
        fields.setVgap(8);
        fields.add(new Label("Name"), 0, 0);
        fields.add(name, 1, 0);
        fields.add(new Label("Email"), 0, 1);
        fields.add(email, 1, 1);
        fields.add(new Label("Password"), 0, 2);
        fields.add(password, 1, 2);
        fields.add(new Label("Confirm password"), 0, 3);
        fields.add(confirmPassword, 1, 3);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Register member");
        dialog.setHeaderText("Enter the new member's details");
        dialog.getDialogPane().setContent(fields);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button registerButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        registerButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> name.getText().isBlank()
                        || email.getText().isBlank()
                        || password.getText().isBlank()
                        || !password.getText().equals(confirmPassword.getText()),
                name.textProperty(), email.textProperty(), password.textProperty(),
                confirmPassword.textProperty()));

        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(result -> execute(() -> {
            runtime.controller().registerMemberWithGeneratedId(employeeId, name.getText(), email.getText(),
                    password.getText());
            view.showMessage("Member registered");
            refreshMembers.run();
        }));
    }

    /** Opens one prefilled form dialog for editing the selected member. */
    private void promptMember(String title, MemberSummary selected, Runnable refreshMembers) {
        Label memberId = new Label(selected.getMemberId());
        TextField name = new TextField(selected.getName());
        TextField email = new TextField(selected.getEmail());

        GridPane fields = new GridPane();
        fields.setHgap(10);
        fields.setVgap(8);
        fields.add(new Label("Member ID"), 0, 0);
        fields.add(memberId, 1, 0);
        fields.add(new Label("Name"), 0, 1);
        fields.add(name, 1, 1);
        fields.add(new Label("Email"), 0, 2);
        fields.add(email, 1, 2);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Edit member details");
        dialog.getDialogPane().setContent(fields);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        saveButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> name.getText().isBlank() || email.getText().isBlank(),
                name.textProperty(), email.textProperty()));

        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(result -> execute(() -> {
            runtime.controller().editMember(employeeId, memberId.getText(), name.getText(), email.getText());
            view.showMessage("Member updated");
            refreshMembers.run();
        }));
    }

    /** Opens a dialog for resetting the selected member's password. */
    private void promptMemberPasswordReset(MemberSummary selected) {
        Label memberId = new Label(selected.getMemberId());
        Label name = new Label(selected.getName());
        Label email = new Label(selected.getEmail());
        PasswordField password = new PasswordField();
        PasswordField newPassword = new PasswordField();
        password.setPromptText("Password");
        newPassword.setPromptText("New password");

        GridPane fields = new GridPane();
        fields.setHgap(10);
        fields.setVgap(8);
        fields.add(new Label("Member ID"), 0, 0);
        fields.add(memberId, 1, 0);
        fields.add(new Label("Name"), 0, 1);
        fields.add(name, 1, 1);
        fields.add(new Label("Email"), 0, 2);
        fields.add(email, 1, 2);
        fields.add(new Label("Password"), 0, 3);
        fields.add(password, 1, 3);
        fields.add(new Label("New password"), 0, 4);
        fields.add(newPassword, 1, 4);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reset password");
        dialog.setHeaderText("Enter matching passwords for " + selected.getName());
        dialog.getDialogPane().setContent(fields);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button resetButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        resetButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> password.getText().isBlank()
                        || newPassword.getText().isBlank()
                        || !password.getText().equals(newPassword.getText()),
                password.textProperty(), newPassword.textProperty()));

        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(result -> execute(() -> {
            runtime.controller().resetMemberPassword(employeeId, memberId.getText(), newPassword.getText());
            view.showMessage("Member password reset");
        }));
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
            if (!confirmBookDeletion(selected)) {
                return;
            }
            runtime.controller().removeBook(employeeId, selected.details().isbn());
            view.showMessage("Book removed");
            search.fire();
        }));
        search.fire();
        return padded(new VBox(10, new HBox(8, query, search, add, edit, remove), table));
    }

    private boolean confirmBookDeletion(BookSummary book) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete book");
        confirmation.setHeaderText("Delete " + book.details().title() + "?");
        confirmation.setContentText("This will permanently delete the book and all copies with ISBN "
                + book.details().isbn() + ". Continue?");
        return confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
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

    private Node createBookCopies() {
        TextField query = new TextField();
        query.setPromptText("Search by copy ID, ISBN, or shelf");
        TableView<BookCopy> table = new TableView<>();
        table.getColumns().addAll(
                textColumn("Copy ID", BookCopy::getCopyId),
                textColumn("ISBN", BookCopy::getIsbn),
                textColumn("Status", copy -> copy.getStatus().toString()),
                textColumn("Shelf", BookCopy::getShelfLocation));

        Button search = new Button("Search");
        search.setOnAction(event -> execute(() -> table.setItems(FXCollections.observableArrayList(
                searchCopies(query.getText())))));
        Button add = new Button("Add");
        add.setOnAction(event -> promptBookCopy(null, search));
        Button edit = new Button("Edit selected");
        edit.setOnAction(event -> {
            BookCopy selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                view.showError("Select a book copy first");
            } else {
                promptBookCopy(selected, search);
            }
        });
        Button remove = new Button("Delete selected");
        remove.setOnAction(event -> execute(() -> {
            BookCopy selected = requireSelection(table, "book copy");
            runtime.bookCopyService().deleteCopy(selected.getCopyId());
            view.showMessage("Book copy deleted");
            search.fire();
        }));

        Button damaged = new Button("Mark damaged");
        damaged.setOnAction(event -> updateCopyStatus(table, query,
                runtime.bookCopyService()::markCopyAsDamaged, "Book copy marked damaged"));
        Button lost = new Button("Mark lost");
        lost.setOnAction(event -> updateCopyStatus(table, query,
                runtime.bookCopyService()::markCopyAsLost, "Book copy marked lost"));
        Button available = new Button("Mark available");
        available.setOnAction(event -> updateCopyStatus(table, query,
                runtime.bookCopyService()::markCopyAsAvailable, "Book copy marked available"));
        search.fire();
        return padded(new VBox(10, new HBox(8, query, search, add, edit, remove), table,
                new HBox(8, damaged, lost, available)));
    }

    private void updateCopyStatus(TableView<BookCopy> table, TextField query,
                                  Consumer<String> statusUpdater, String successMessage) {
        execute(() -> {
            BookCopy selected = requireSelection(table, "book copy");
            statusUpdater.accept(selected.getCopyId());
            table.getItems().setAll(searchCopies(query.getText()));
            table.refresh();
            table.getItems().stream()
                    .filter(copy -> copy.getCopyId().equals(selected.getCopyId()))
                    .findFirst()
                    .ifPresent(copy -> table.getSelectionModel().select(copy));
            view.showMessage(successMessage);
        });
    }

    private List<BookCopy> searchCopies(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return runtime.bookCopyService().getAllCopies().stream()
                .filter(copy -> normalizedQuery.isEmpty()
                        || copy.getCopyId().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || copy.getIsbn().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || copy.getShelfLocation().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .sorted((first, second) -> first.getCopyId().compareToIgnoreCase(second.getCopyId()))
                .toList();
    }

    private void promptBookCopy(BookCopy selected, Button search) {
        String copyId = selected == null ? prompt("Book copy details", "Copy ID", "") : selected.getCopyId();
        String isbn = selected == null ? prompt("Book copy details", "ISBN", "") : selected.getIsbn();
        String shelfLocation = prompt("Book copy details", "Shelf location",
                selected == null ? "" : selected.getShelfLocation());
        if (copyId == null || isbn == null || shelfLocation == null) {
            return;
        }

        execute(() -> {
            if (selected == null) {
                if (runtime.bookService().getBookByIsbn(isbn).isEmpty()) {
                    throw new IllegalArgumentException("Cannot add a copy for an unknown ISBN: " + isbn);
                }
                runtime.bookCopyService().createCopy(copyId, isbn, CopyStatus.AVAILABLE,
                        shelfLocation);
                view.showMessage("Book copy added");
            } else {
                runtime.bookCopyService().updateCopyShelfLocation(selected.getCopyId(), shelfLocation);
                view.showMessage("Book copy updated");
            }
            search.fire();
        });
    }

    private Node createLoans() {
        TableView<LoanSummary> table = new TableView<>();
        table.getColumns().addAll(
                textColumn("Loan ID", LoanSummary::getLoanId),
                textColumn("Member", LoanSummary::getMemberId),
                textColumn("Copy", LoanSummary::getBookCopyId),
                textColumn("Due date", loan -> loan.getDueDate().toString()),
                textColumn("Status", this::getLoanStatus));
        Button all = new Button("All");
        all.setOnAction(event -> execute(() -> table.setItems(FXCollections.observableArrayList(
                runtime.controller().viewLoans(employeeId)))));
        Button active = new Button("Active");
        active.setOnAction(event -> execute(() -> table.setItems(FXCollections.observableArrayList(
                filterLoans(runtime.controller().viewLoans(employeeId), false)))));
        Button overdue = new Button("Overdue");
        overdue.setOnAction(event -> execute(() -> table.setItems(FXCollections.observableArrayList(
                runtime.controller().viewOverdueLoans(employeeId)))));
        Button alert = new Button("Alert selected member");
        alert.setOnAction(event -> execute(() -> {
            LoanSummary selected = requireSelection(table, "loan");
            runtime.controller().sendOverdueAlert(employeeId, selected.getLoanId());
        }));
        execute(() -> table.setItems(FXCollections.observableArrayList(
                runtime.controller().viewLoans(employeeId))));
        return padded(new VBox(10, new HBox(8, all, active, overdue, alert), table));
    }

    private List<LoanSummary> filterLoans(List<LoanSummary> loans, boolean overdue) {
        LocalDate today = LocalDate.now();
        return loans.stream()
                .filter(loan -> loan.isOverdueOn(today) == overdue)
                .toList();
    }

    private String getLoanStatus(LoanSummary loan) {
        return loan.isOverdueOn(LocalDate.now()) ? "Overdue" : "Active";
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
                        ? listAllReservations()
                        : listReservations(memberId.getText())))));
        Button create = new Button("Create");
        create.setOnAction(event -> execute(() -> {
            requireActiveLibrarian();
            runtime.reservationService().reserveBook(memberId.getText(), bookId.getText());
            view.showMessage("Reservation created");
            list.fire();
        }));
        Button pending = new Button("Pending for book");
        pending.setOnAction(event -> execute(() -> table.setItems(FXCollections.observableArrayList(
                pendingReservations(bookId.getText())))));
        Button cancel = new Button("Cancel selected");
        cancel.setOnAction(event -> execute(() -> {
            Reservation selected = requireSelection(table, "reservation");
            requireActiveLibrarian();
            runtime.reservationService().cancelReservation(selected.getId());
            view.showMessage("Reservation cancelled");
            list.fire();
        }));
        Button fulfil = new Button("Fulfil selected");
        fulfil.setOnAction(event -> execute(() -> {
            Reservation selected = requireSelection(table, "reservation");
            requireActiveLibrarian();
            runtime.reservationService().fulfilReservation(selected.getId());
            view.showMessage("Reservation fulfilled");
            list.fire();
        }));
        Button reminder = new Button("Send reminder");
        reminder.setOnAction(event -> execute(() -> {
            Reservation selected = requireSelection(table, "reservation");
            runtime.controller().sendReservationReminder(employeeId, selected.getId());
        }));
        list.fire();
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
            requireActiveLibrarian();
            runtime.fineService().createFine(loanId.getText());
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
                listNotifications(userId.getText())))));
        Button read = new Button("Mark selected read");
        read.setOnAction(event -> execute(() -> {
            Notification selected = requireSelection(table, "notification");
            requireActiveLibrarian();
            runtime.notificationService().markAsRead(selected.getId());
            view.showMessage("Notification marked read");
            list.fire();
        }));
        return padded(new VBox(10, new HBox(8, userId, list, read), table));
    }

    private List<Reservation> listAllReservations() {
        requireActiveLibrarian();
        return runtime.reservationService().getAllReservations();
    }

    private List<Reservation> listReservations(String memberId) {
        requireActiveLibrarian();
        return runtime.reservationService().getReservations(memberId);
    }

    private List<Reservation> pendingReservations(String bookId) {
        requireActiveLibrarian();
        return runtime.reservationService().processPendingReservations(bookId);
    }

    private List<Notification> listNotifications(String userId) {
        requireActiveLibrarian();
        return runtime.notificationService().getNotifications(userId);
    }

    private void requireActiveLibrarian() {
        runtime.librarianService().requireActive(employeeId);
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
