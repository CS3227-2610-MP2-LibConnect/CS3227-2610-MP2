package libconnect.ui;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import libconnect.integration.BookCopyManagement;
import libconnect.integration.BookDetails;
import libconnect.integration.BookManagement;
import libconnect.integration.BookSummary;
import libconnect.integration.LoanQuery;
import libconnect.integration.LoanSummary;
import libconnect.integration.MemberManagement;
import libconnect.integration.MemberSummary;
import libconnect.librarian.LibrarianController;
import libconnect.librarian.LibrarianView;
import libconnect.models.Book;
import libconnect.models.BookCopy;
import libconnect.models.Loan;
import libconnect.models.Member;
import libconnect.services.BookCopyService;
import libconnect.services.BookService;
import libconnect.services.FineService;
import libconnect.services.LibrarianService;
import libconnect.services.MemberService;
import libconnect.services.NotificationService;
import libconnect.services.PasswordHasher;
import libconnect.services.ReservationService;
import libconnect.services.UserIdGenerator;
import libconnect.services.UserService;
import libconnect.storage.StorageManager;
import libconnect.storage.file.FileBookCopyRepository;
import libconnect.storage.file.FileBookRepository;
import libconnect.storage.file.FileFineRepository;
import libconnect.storage.file.FileLibrarianRepository;
import libconnect.storage.file.FileLoanRepository;
import libconnect.storage.file.FileMemberRepository;
import libconnect.storage.file.FileNotificationRepository;
import libconnect.storage.file.FileReservationRepository;
import libconnect.storage.repositories.BookCopyRepository;
import libconnect.storage.repositories.BookRepository;
import libconnect.storage.repositories.LoanRepository;
import libconnect.storage.repositories.MemberRepository;

/** Composes the file-backed services required by the librarian UI. */
public final class LibrarianCompositionRoot {
    private LibrarianCompositionRoot() {
    }

    /**
     * Creates a librarian runtime using the supplied data directory, clock, and UI view.
     *
     * @param dataDirectory the directory containing the application data files.
     * @param clock the clock used by date-sensitive librarian operations.
     * @param view the view receiving controller feedback.
     * @return a fully wired librarian runtime.
     */
    public static LibrarianRuntime create(Path dataDirectory, Clock clock, LibrarianView view) {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        Objects.requireNonNull(clock, "clock");
        Objects.requireNonNull(view, "view");

        StorageManager storageManager = new StorageManager(dataDirectory);
        FileLibrarianRepository librarianRepository = new FileLibrarianRepository(storageManager,
                dataDirectory.resolve("librarians.json"));
        FileMemberRepository memberRepository = new FileMemberRepository(storageManager,
                dataDirectory.resolve("members.json"));
        FileBookRepository bookRepository = new FileBookRepository(storageManager,
                dataDirectory.resolve("books.json"));
        FileBookCopyRepository copyRepository = new FileBookCopyRepository(storageManager,
                dataDirectory.resolve("book-copies.json"));
        FileLoanRepository loanRepository = new FileLoanRepository(storageManager,
                dataDirectory.resolve("loans.json"));

        UserService userService = new UserService(memberRepository, librarianRepository);
        MemberService memberService = new MemberService(memberRepository, userService);
        BookCopyService bookCopyService = new BookCopyService(copyRepository);
        BookService bookService = new BookService(bookRepository, bookCopyService);
        LibrarianService librarianService = new LibrarianService(librarianRepository, memberRepository);

        MemberManagement members = new MemberAdapter(memberRepository, memberService, userService);
        BookManagement books = new BookAdapter(bookRepository, bookService, copyRepository);
        BookCopyManagement copies = new BookCopyAdapter(bookCopyService);
        LoanQuery loans = new LoanAdapter(loanRepository);

        ReservationService reservationService = new ReservationService(
                new FileReservationRepository(storageManager, dataDirectory.resolve("reservations.json")),
                members, books, clock, Period.ofDays(7));
        FineService fineService = new FineService(
                new FileFineRepository(storageManager, dataDirectory.resolve("fines.json")),
                loans, members, clock, BigDecimal.valueOf(1.50));
        NotificationService notificationService = new NotificationService(
                new FileNotificationRepository(storageManager, dataDirectory.resolve("notifications.json")),
                clock);
        LibrarianController controller = new LibrarianController(librarianService, reservationService,
                fineService, notificationService, loans, view, clock, members, books, copies);

        return new LibrarianRuntime(librarianService, reservationService, fineService,
                notificationService, controller);
    }

    private static final class MemberAdapter implements MemberManagement {
        private final MemberRepository repository;
        private final MemberService service;
        private final UserService userService;

        private MemberAdapter(MemberRepository repository, MemberService service, UserService userService) {
            this.repository = repository;
            this.service = service;
            this.userService = userService;
        }

        @Override
        public boolean exists(String memberId) {
            return repository.findByMembershipId(memberId).isPresent();
        }

        @Override
        public boolean isActive(String memberId) {
            return repository.findByMembershipId(memberId).map(Member::isActive).orElse(false);
        }

        @Override
        public void registerMember(String memberId, String name, String email) {
            if (exists(memberId)) {
                throw new IllegalStateException("A member with this membership ID already exists");
            }
            if (userService.isEmailInUse(email)) {
                throw new IllegalStateException("A member or librarian with this email already exists");
            }
            String userId = new UserIdGenerator(userService).generate();
            repository.save(new Member(userId, name, email, PasswordHasher.hash("change-me"), memberId));
        }

        @Override
        public void editMember(String memberId, String name, String email) {
            service.updateMemberProfile(memberId, name, email);
        }

        @Override
        public void deactivateMember(String memberId) {
            service.deactivateMember(memberId);
        }

        @Override
        public List<MemberSummary> searchMembers(String query) {
            String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
            return repository.findAll().stream()
                    .filter(member -> normalizedQuery.isEmpty()
                            || member.getMembershipId().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                            || member.getName().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                            || member.getEmail().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                    .sorted(Comparator.comparing(Member::getMembershipId))
                    .map(member -> new MemberSummary(member.getMembershipId(), member.getName(),
                            member.getEmail(), member.isActive()))
                    .toList();
        }
    }

    private static final class BookAdapter implements BookManagement {
        private final BookRepository repository;
        private final BookService service;
        private final BookCopyRepository copyRepository;

        private BookAdapter(BookRepository repository, BookService service, BookCopyRepository copyRepository) {
            this.repository = repository;
            this.service = service;
            this.copyRepository = copyRepository;
        }

        @Override
        public boolean exists(String bookId) {
            return repository.findByIsbn(bookId).isPresent();
        }

        @Override
        public boolean isAvailable(String bookId) {
            return copyRepository.findByIsbn(bookId).stream().anyMatch(BookCopy::isAvailable);
        }

        @Override
        public String addBook(BookDetails details) {
            service.createBook(details.isbn(), details.title(), details.author(), details.publisher(),
                    details.category(), details.publicationYear());
            return details.isbn();
        }

        @Override
        public void editBook(String bookId, BookDetails details) {
            service.updateBook(bookId, details.title(), details.author(), details.publisher(),
                    details.category(), details.publicationYear());
        }

        @Override
        public void removeBook(String bookId) {
            service.deleteBook(bookId);
        }

        @Override
        public List<BookSummary> searchBooks(String query) {
            String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
            return repository.findAll().stream()
                    .filter(book -> normalizedQuery.isEmpty() || matches(book, normalizedQuery))
                    .sorted(Comparator.comparing(Book::getIsbn))
                    .map(this::toSummary)
                    .toList();
        }

        private BookSummary toSummary(Book book) {
            BookDetails details = new BookDetails(book.getIsbn(), book.getTitle(), book.getAuthor(),
                    book.getPublisher(), book.getCategory(), book.getPublicationYear());
            int availableCopies = (int) copyRepository.findByIsbn(book.getIsbn()).stream()
                    .filter(BookCopy::isAvailable)
                    .count();
            return new BookSummary(book.getIsbn(), details, availableCopies);
        }

        private boolean matches(Book book, String query) {
            return book.getIsbn().toLowerCase(Locale.ROOT).contains(query)
                    || book.getTitle().toLowerCase(Locale.ROOT).contains(query)
                    || book.getAuthor().toLowerCase(Locale.ROOT).contains(query)
                    || book.getPublisher().toLowerCase(Locale.ROOT).contains(query)
                    || book.getCategory().toLowerCase(Locale.ROOT).contains(query);
        }
    }

    private static final class BookCopyAdapter implements BookCopyManagement {
        private final BookCopyService service;

        private BookCopyAdapter(BookCopyService service) {
            this.service = service;
        }

        @Override
        public void recordDamagedBook(String copyId) {
            service.markCopyAsDamaged(copyId);
        }

        @Override
        public void recordLostBook(String copyId) {
            service.markCopyAsLost(copyId);
        }
    }

    private static final class LoanAdapter implements LoanQuery {
        private final LoanRepository repository;

        private LoanAdapter(LoanRepository repository) {
            this.repository = repository;
        }

        @Override
        public Optional<LoanSummary> findById(String loanId) {
            return repository.findById(loanId).map(this::toSummary);
        }

        @Override
        public List<LoanSummary> findActiveLoans() {
            return repository.findAll().stream()
                    .filter(Loan::isActive)
                    .map(this::toSummary)
                    .toList();
        }

        @Override
        public List<LoanSummary> findOverdueLoans(LocalDate date) {
            return findActiveLoans().stream()
                    .filter(loan -> loan.isOverdueOn(date))
                    .toList();
        }

        private LoanSummary toSummary(Loan loan) {
            return new LoanSummary(loan.getLoanId(), loan.getMemberId(), loan.getCopyId(), loan.getDueDate());
        }
    }
}
