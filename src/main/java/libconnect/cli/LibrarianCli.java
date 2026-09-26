package libconnect.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import libconnect.integration.BookDetails;
import libconnect.integration.BookSummary;
import libconnect.integration.LoanSummary;
import libconnect.integration.MemberSummary;
import libconnect.librarian.LibrarianController;
import libconnect.models.Fine;
import libconnect.models.Librarian;
import libconnect.models.Notification;
import libconnect.models.Reservation;
import libconnect.services.FineService;
import libconnect.services.LibrarianService;
import libconnect.services.NotificationService;
import libconnect.services.ReservationService;

/** Provides an interactive command-line harness for manual librarian testing. */
public final class LibrarianCli {
    private final BufferedReader input;
    private final PrintWriter output;
    private final LibrarianService librarianService;
    private final ReservationService reservationService;
    private final FineService fineService;
    private final NotificationService notificationService;
    private final LibrarianController controller;
    private String currentEmployeeId;
    private boolean running;

    /** Creates a CLI with injected services, controller, and terminal streams. */
    LibrarianCli(Reader input, Writer output, LibrarianService librarianService,
                 ReservationService reservationService, FineService fineService,
                 NotificationService notificationService, LibrarianController controller) {
        this.input = new BufferedReader(Objects.requireNonNull(input, "input"));
        this.output = new PrintWriter(Objects.requireNonNull(output, "output"), true);
        this.librarianService = Objects.requireNonNull(librarianService, "librarianService");
        this.reservationService = Objects.requireNonNull(reservationService, "reservationService");
        this.fineService = Objects.requireNonNull(fineService, "fineService");
        this.notificationService = Objects.requireNonNull(notificationService, "notificationService");
        this.controller = Objects.requireNonNull(controller, "controller");
    }

    /** Runs the interactive prompt until the user exits or input ends. */
    public void run() {
        running = true;
        output.println("LibConnect librarian CLI. Type 'help' for commands.");
        while (running) {
            output.print("libconnect> ");
            output.flush();
            try {
                String line = input.readLine();
                if (line == null) {
                    running = false;
                } else if (!line.isBlank()) {
                    execute(line);
                }
            } catch (IOException exception) {
                output.println("ERROR: Unable to read CLI input");
                running = false;
            }
        }
    }

    private void execute(String line) {
        String[] arguments = line.trim().split("\\s+");
        try {
            switch (arguments[0].toLowerCase()) {
            case "help" -> printHelp();
            case "exit", "quit" -> running = false;
            case "seed" -> seedDemoData();
            case "login" -> login(arguments);
            case "logout" -> logout();
            case "librarians" -> handleLibrarians(arguments);
            case "members" -> handleMembers(arguments);
            case "books" -> handleBooks(arguments);
            case "loans" -> handleLoans(arguments);
            case "reservations" -> handleReservations(arguments);
            case "fines" -> handleFines(arguments);
            case "notifications" -> handleNotifications(arguments);
            case "alerts" -> handleAlerts(arguments);
            default -> throw new IllegalArgumentException("Unknown command; type 'help'");
            }
        } catch (RuntimeException exception) {
            output.println("ERROR: " + exception.getMessage());
        }
    }

    private void printHelp() {
        output.println("Commands:");
        output.println("  seed");
        output.println("  librarians register <employeeId> <name> <email>");
        output.println("  login <employeeId>              logout");
        output.println("  members search <query>          members register <id> <name> <email>");
        output.println("  members edit <id> <name> <email>   members deactivate <id>");
        output.println("  books search <query>            books add <isbn> <title> <author> <publisher> <category> <year>");
        output.println("  books edit <id> <isbn> <title> <author> <publisher> <category> <year>");
        output.println("  books remove <id>               books damaged|lost <copyId>");
        output.println("  loans list                      loans overdue");
        output.println("  reservations list [memberId]    reservations create <memberId> <bookId>");
        output.println("  reservations pending <bookId>   reservations cancel|fulfil <reservationId>");
        output.println("  fines list <memberId>            fines create <loanId>");
        output.println("  fines edit <fineId> <amount>     fines remove <fineId>");
        output.println("  notifications list <userId>     notifications read <notificationId>");
        output.println("  alerts overdue <loanId>         alerts reservation <reservationId>");
        output.println("  exit");
    }

    private void seedDemoData() {
        if (librarianService.search("e1").isEmpty()) {
            librarianService.register(new Librarian("e1", "Demo Librarian", "librarian@example.com",
                    libconnect.models.AccountStatus.ACTIVE));
        }
        output.println("OK: Demo data ready. Login with 'login e1'.");
    }

    private void login(String[] arguments) {
        requireLength(arguments, 2, "login <employeeId>");
        Librarian librarian = librarianService.requireActive(arguments[1]);
        currentEmployeeId = librarian.getId();
        output.println("OK: Logged in as " + librarian.getName());
    }

    private void logout() {
        currentEmployeeId = null;
        output.println("OK: Logged out");
    }

    private void handleLibrarians(String[] arguments) {
        requireLength(arguments, 5, "librarians register <employeeId> <name> <email>");
        if (!arguments[1].equalsIgnoreCase("register")) {
            throw new IllegalArgumentException("Unknown librarians command");
        }
        librarianService.register(new Librarian(arguments[2], arguments[3], arguments[4],
                libconnect.models.AccountStatus.ACTIVE));
        output.println("OK: Librarian registered");
    }

    private void handleMembers(String[] arguments) {
        requireLogin();
        requireAtLeast(arguments, 2, "members <search|register|edit|deactivate> ...");
        switch (arguments[1].toLowerCase()) {
        case "search" -> printMembers(controller.searchMembers(currentEmployeeId, joinFrom(arguments, 2)));
        case "register" -> {
            requireLength(arguments, 5, "members register <id> <name> <email>");
            controller.registerMember(currentEmployeeId, arguments[2], arguments[3], arguments[4]);
            output.println("OK: Member registered");
        }
        case "edit" -> {
            requireLength(arguments, 5, "members edit <id> <name> <email>");
            controller.editMember(currentEmployeeId, arguments[2], arguments[3], arguments[4]);
            output.println("OK: Member updated");
        }
        case "deactivate" -> {
            requireLength(arguments, 3, "members deactivate <id>");
            controller.deactivateMember(currentEmployeeId, arguments[2]);
            output.println("OK: Member deactivated");
        }
        default -> throw new IllegalArgumentException("Unknown members command");
        }
    }

    private void handleBooks(String[] arguments) {
        requireLogin();
        requireAtLeast(arguments, 2, "books <search|add|edit|remove|damaged|lost> ...");
        switch (arguments[1].toLowerCase()) {
        case "search" -> printBooks(controller.searchBooks(currentEmployeeId, joinFrom(arguments, 2)));
        case "add" -> {
            requireLength(arguments, 8, "books add <isbn> <title> <author> <publisher> <category> <year>");
            String bookId = controller.addBook(currentEmployeeId, parseBookDetails(arguments, 2));
            output.println("OK: Book added with ID " + bookId);
        }
        case "edit" -> {
            requireLength(arguments, 9, "books edit <id> <isbn> <title> <author> <publisher> <category> <year>");
            controller.editBook(currentEmployeeId, arguments[2], parseBookDetails(arguments, 3));
            output.println("OK: Book updated");
        }
        case "remove" -> {
            requireLength(arguments, 3, "books remove <id>");
            controller.removeBook(currentEmployeeId, arguments[2]);
            output.println("OK: Book removed");
        }
        case "damaged" -> {
            requireLength(arguments, 3, "books damaged <copyId>");
            controller.recordDamagedBook(currentEmployeeId, arguments[2]);
            output.println("OK: Copy recorded as damaged");
        }
        case "lost" -> {
            requireLength(arguments, 3, "books lost <copyId>");
            controller.recordLostBook(currentEmployeeId, arguments[2]);
            output.println("OK: Copy recorded as lost");
        }
        default -> throw new IllegalArgumentException("Unknown books command");
        }
    }

    private void handleLoans(String[] arguments) {
        requireLogin();
        requireLength(arguments, 2, "loans <list|overdue>");
        List<LoanSummary> loans;
        if (arguments[1].equalsIgnoreCase("overdue")) {
            loans = controller.viewOverdueLoans(currentEmployeeId);
        } else if (arguments[1].equalsIgnoreCase("list")) {
            loans = controller.viewLoans(currentEmployeeId);
        } else {
            throw new IllegalArgumentException("Unknown loans command");
        }
        loans.forEach(loan -> output.println(loan.getLoanId() + " member=" + loan.getMemberId()
                + " copy=" + loan.getBookCopyId() + " due=" + loan.getDueDate()));
    }

    private void handleReservations(String[] arguments) {
        requireLogin();
        requireAtLeast(arguments, 2, "reservations <list|create|pending|cancel|fulfil> ...");
        switch (arguments[1].toLowerCase()) {
        case "list" -> {
            List<Reservation> reservations = arguments.length == 2
                    ? controller.viewAllReservations(currentEmployeeId)
                    : controller.viewReservations(currentEmployeeId, arguments[2]);
            printReservations(reservations);
        }
        case "create" -> {
            requireLength(arguments, 4, "reservations create <memberId> <bookId>");
            Reservation reservation = reservationService.reserveBook(arguments[2], arguments[3]);
            output.println("OK: Reservation created with ID " + reservation.getId());
        }
        case "pending" -> {
            requireLength(arguments, 3, "reservations pending <bookId>");
            printReservations(reservationService.processPendingReservations(arguments[2]));
        }
        case "cancel" -> {
            requireLength(arguments, 3, "reservations cancel <reservationId>");
            reservationService.cancelReservation(arguments[2]);
            output.println("OK: Reservation cancelled");
        }
        case "fulfil" -> {
            requireLength(arguments, 3, "reservations fulfil <reservationId>");
            reservationService.fulfilReservation(arguments[2]);
            output.println("OK: Reservation fulfilled");
        }
        default -> throw new IllegalArgumentException("Unknown reservations command");
        }
    }

    private void handleFines(String[] arguments) {
        requireLogin();
        requireAtLeast(arguments, 2, "fines <list|create|edit|remove> ...");
        switch (arguments[1].toLowerCase()) {
        case "list" -> printFines(controller.viewFines(currentEmployeeId, requireArgument(arguments, 2)));
        case "create" -> {
            Fine fine = fineService.createFine(requireArgument(arguments, 2));
            output.println("OK: Fine " + fine.getId() + " amount=" + fine.getAmount());
        }
        case "edit" -> {
            requireLength(arguments, 4, "fines edit <fineId> <amount>");
            Fine fine = controller.editFine(currentEmployeeId, arguments[2], new BigDecimal(arguments[3]));
            output.println("OK: Fine updated amount=" + fine.getAmount());
        }
        case "remove" -> {
            requireLength(arguments, 3, "fines remove <fineId>");
            controller.removeFine(currentEmployeeId, arguments[2]);
            output.println("OK: Fine removed");
        }
        default -> throw new IllegalArgumentException("Unknown fines command");
        }
    }

    private void handleNotifications(String[] arguments) {
        requireLogin();
        requireLength(arguments, 3, "notifications <list|read> <id>");
        switch (arguments[1].toLowerCase()) {
        case "list" -> notificationService.getNotifications(arguments[2]).forEach(this::printNotification);
        case "read" -> printNotification(notificationService.markAsRead(arguments[2]));
        default -> throw new IllegalArgumentException("Unknown notifications command");
        }
    }

    private void handleAlerts(String[] arguments) {
        requireLogin();
        requireLength(arguments, 3, "alerts <overdue|reservation> <id>");
        if (arguments[1].equalsIgnoreCase("overdue")) {
            output.println("OK: Alert " + controller.sendOverdueAlert(currentEmployeeId, arguments[2]).getId());
        } else if (arguments[1].equalsIgnoreCase("reservation")) {
            output.println("OK: Reminder "
                    + controller.sendReservationReminder(currentEmployeeId, arguments[2]).getId());
        } else {
            throw new IllegalArgumentException("Unknown alerts command");
        }
    }

    private void printMembers(List<MemberSummary> members) {
        members.forEach(member -> output.println(member.getMemberId() + " " + member.getName()
                + " " + member.getEmail() + " active=" + member.isActive()));
    }

    private void printBooks(List<BookSummary> books) {
        books.forEach(book -> output.println(book.bookId() + " " + book.details().title()
                + " available=" + book.availableCopies()));
    }

    private void printReservations(List<Reservation> reservations) {
        reservations.forEach(reservation -> output.println(reservation.getId() + " member="
                + reservation.getMemberId() + " book=" + reservation.getBookId()
                + " status=" + reservation.getStatus()));
    }

    private void printFines(List<Fine> fines) {
        fines.forEach(fine -> output.println(fine.getId() + " loan=" + fine.getLoanId()
                + " amount=" + fine.getAmount() + " status=" + fine.getStatus()));
    }

    private void printNotification(Notification notification) {
        output.println(notification.getId() + " " + notification.getType() + " read="
                + notification.isRead() + " " + notification.getMessage());
    }

    private BookDetails parseBookDetails(String[] arguments, int offset) {
        return new BookDetails(arguments[offset], arguments[offset + 1], arguments[offset + 2],
                arguments[offset + 3], arguments[offset + 4], Integer.parseInt(arguments[offset + 5]));
    }

    private String joinFrom(String[] arguments, int startIndex) {
        return startIndex >= arguments.length ? "" : String.join(" ", Arrays.copyOfRange(arguments, startIndex,
                arguments.length));
    }

    private void requireLogin() {
        if (currentEmployeeId == null) {
            throw new IllegalStateException("Login required");
        }
    }

    private static String requireArgument(String[] arguments, int index) {
        if (index >= arguments.length) {
            throw new IllegalArgumentException("Missing command argument");
        }
        return arguments[index];
    }

    private static void requireAtLeast(String[] arguments, int length, String usage) {
        if (arguments.length < length) {
            throw new IllegalArgumentException("Usage: " + usage);
        }
    }

    private static void requireLength(String[] arguments, int length, String usage) {
        if (arguments.length != length) {
            throw new IllegalArgumentException("Usage: " + usage);
        }
    }
}
