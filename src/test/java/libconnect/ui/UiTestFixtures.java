package libconnect.ui;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.stage.Stage;

import libconnect.models.AccountType;
import libconnect.models.Book;
import libconnect.models.BookCopy;
import libconnect.models.CopyStatus;
import libconnect.models.Loan;
import libconnect.models.LoanStatus;
import libconnect.models.Member;
import libconnect.models.User;
import libconnect.services.AuthenticationService;
import libconnect.services.BookCopyService;
import libconnect.services.BookSearchCriteria;
import libconnect.services.BookService;
import libconnect.services.BorrowService;
import libconnect.services.LoanService;
import libconnect.services.MemberService;
import libconnect.storage.repositories.BookCopyRepository;
import libconnect.storage.repositories.LoanRepository;

/** Supplies deterministic entities and service doubles for UI tests. */
public final class UiTestFixtures {
    private UiTestFixtures() {
    }

    public static Book book(String isbn, String title) {
        return new Book(isbn, title, "Author", "Publisher", "Category", 2020);
    }

    public static BookCopy copy(String copyId, String isbn, CopyStatus status) {
        return new BookCopy(copyId, isbn, status, "A-1");
    }

    public static Member member() {
        return new Member("USER-1", "Alex Member", "alex@example.com", "hash", "MEM-1");
    }

    public static Loan activeLoan(String loanId, String copyId) {
        return new Loan(loanId, "MEM-1", copyId, LocalDate.now().minusDays(5));
    }
    
    public static Loan renewedLoan(String loanId, String copyId) {
        return new Loan(loanId, "MEM-1", copyId, LocalDate.now(), LocalDate.now().plusDays(30), null,
                LoanStatus.ACTIVE, true);
    }

    public static Loan overdueLoan(String loanId, String copyId) {
        return new Loan(loanId, "MEM-1", copyId, LocalDate.now().minusDays(20), LocalDate.now().minusDays(5), null,
                LoanStatus.ACTIVE, false);
    }

    public static Loan returnedLoan(String loanId, String copyId) {
        LocalDate borrowDate = LocalDate.now().minusDays(20);
        return new Loan(loanId, "MEM-1", copyId, borrowDate, borrowDate.plusDays(10),
                borrowDate.plusDays(15), LoanStatus.RETURNED, false);
    }

    static SceneNavigator navigator(Stage stage, SessionManager sessionManager,
                                    TestAuthenticationService authenticationService,
                                    TestBookService bookService,
                                    TestBookCopyService bookCopyService,
                                    BorrowService borrowService,
                                    TestLoanService loanService,
                                    TestMemberService memberService) {
        return new SceneNavigator(stage, authenticationService, sessionManager, bookService,
                bookCopyService, borrowService, loanService, memberService);
    }

    static final class TestAuthenticationService extends AuthenticationService {
        User user = member();
        RuntimeException authenticateFailure;
        RuntimeException registerFailure;
        String lastEmail;
        String lastPassword;
        AccountType lastAccountType;
        String lastName;

        @Override
        public User authenticate(AccountType accountType, String email, String password) {
            lastAccountType = accountType;
            lastEmail = email;
            lastPassword = password;
            if (authenticateFailure != null) {
                throw authenticateFailure;
            }
            return user;
        }

        @Override
        public User register(AccountType accountType, String name, String email, String password) {
            lastAccountType = accountType;
            lastName = name;
            lastEmail = email;
            lastPassword = password;
            if (registerFailure != null) {
                throw registerFailure;
            }
            return user;
        }
    }

    static final class TestBookService extends BookService {
        final List<Book> books = new ArrayList<>();
        RuntimeException allBooksFailure;
        RuntimeException searchFailure;
        RuntimeException bookLookupFailure;
        BookSearchCriteria lastCriteria;

        @Override
        public List<Book> getAllBooks() {
            if (allBooksFailure != null) {
                throw allBooksFailure;
            }
            return List.copyOf(books);
        }

        @Override
        public List<Book> searchBooks(BookSearchCriteria criteria) {
            lastCriteria = criteria;
            if (searchFailure != null) {
                throw searchFailure;
            }
            return books.stream().filter(book -> criteria.isEmpty()
                    || book.getTitle().toLowerCase().contains(criteria.getTitle().toLowerCase()))
                    .toList();
        }

        @Override
        public Optional<Book> getBookByIsbn(String isbn) {
            if (bookLookupFailure != null) {
                throw bookLookupFailure;
            }
            return books.stream().filter(book -> book.getIsbn().equals(isbn)).findFirst();
        }
    }

    static final class TestBookCopyService extends BookCopyService {
        final Map<String, BookCopy> copies = new HashMap<>();
        RuntimeException lookupFailure;

        @Override
        public Optional<BookCopy> getCopyById(String copyId) {
            if (lookupFailure != null) {
                throw lookupFailure;
            }
            return Optional.ofNullable(copies.get(copyId));
        }

        @Override
        public List<BookCopy> getCopiesByIsbn(String isbn) {
            return copies.values().stream().filter(copy -> copy.getIsbn().equals(isbn)).toList();
        }
    }

    static final class TestLoanService extends LoanService {
        final List<Loan> loans = new ArrayList<>();
        RuntimeException loadFailure;
        RuntimeException renewFailure;
        String lastRenewedLoanId;

        @Override
        public List<Loan> getLoansByMemberId(String membershipId) {
            if (loadFailure != null) {
                throw loadFailure;
            }
            return List.copyOf(loans);
        }

        @Override
        public void renewLoan(String loanId) {
            lastRenewedLoanId = loanId;
            if (renewFailure != null) {
                throw renewFailure;
            }
        }
    }

    static final class TestMemberService extends MemberService {
        Member updatedMember = member();
        RuntimeException profileFailure;
        RuntimeException passwordFailure;
        String lastMembershipId;
        String lastName;
        String lastEmail;
        String lastPassword;

        @Override
        public Member updateMemberProfile(String membershipId, String name, String email) {
            lastMembershipId = membershipId;
            lastName = name;
            lastEmail = email;
            if (profileFailure != null) {
                throw profileFailure;
            }
            return updatedMember;
        }

        @Override
        public Member updatePassword(String membershipId, String newPassword) {
            lastMembershipId = membershipId;
            lastPassword = newPassword;
            if (passwordFailure != null) {
                throw passwordFailure;
            }
            return updatedMember;
        }
    }

    static final class InMemoryBookCopyRepository implements BookCopyRepository {
        final Map<String, BookCopy> copies = new HashMap<>();

        @Override
        public Optional<BookCopy> findById(String id) {
            return Optional.ofNullable(copies.get(id));
        }

        @Override
        public boolean deleteById(String id) {
            return copies.remove(id) != null;
        }

        @Override
        public void save(BookCopy entity) {
            copies.put(entity.getCopyId(), entity);
        }

        @Override
        public List<BookCopy> findAll() {
            return new ArrayList<>(copies.values());
        }

        @Override
        public List<BookCopy> findByIsbn(String isbn) {
            return copies.values().stream().filter(copy -> copy.getIsbn().equals(isbn)).toList();
        }

        @Override
        public void deleteByIsbn(String isbn) {
            copies.values().removeIf(copy -> copy.getIsbn().equals(isbn));
        }

        @Override
        public List<BookCopy> findByStatus(CopyStatus status) {
            return copies.values().stream().filter(copy -> copy.getStatus() == status).toList();
        }
    }

    static final class InMemoryLoanRepository implements LoanRepository {
        final Map<String, Loan> loans = new HashMap<>();

        @Override
        public Optional<Loan> findById(String id) {
            return Optional.ofNullable(loans.get(id));
        }

        @Override
        public boolean deleteById(String id) {
            return loans.remove(id) != null;
        }

        @Override
        public void save(Loan entity) {
            loans.put(entity.getLoanId(), entity);
        }

        @Override
        public List<Loan> findAll() {
            return new ArrayList<>(loans.values());
        }

        @Override
        public List<Loan> findByMemberId(String memberId) {
            return loans.values().stream().filter(loan -> loan.getMemberId().equals(memberId)).toList();
        }

        @Override
        public List<Loan> findByCopyId(String copyId) {
            return loans.values().stream().filter(loan -> loan.getCopyId().equals(copyId)).toList();
        }

        @Override
        public List<Loan> findByStatus(LoanStatus status) {
            return loans.values().stream().filter(loan -> loan.getStatus() == status).toList();
        }
    }
}
