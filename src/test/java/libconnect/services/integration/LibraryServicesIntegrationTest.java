package libconnect.services.integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import libconnect.models.AccountStatus;
import libconnect.models.BookCopy;
import libconnect.models.CopyStatus;
import libconnect.models.LoanStatus;
import libconnect.models.Member;
import libconnect.services.BookCopyService;
import libconnect.services.BookService;
import libconnect.services.LoanService;
import libconnect.services.MemberService;
import libconnect.storage.file.FileBookCopyRepository;
import libconnect.storage.file.FileBookRepository;
import libconnect.storage.file.FileLoanRepository;
import libconnect.storage.file.FileMemberRepository;

/** Verifies workflows that span multiple services and persisted repositories. */
class LibraryServicesIntegrationTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void memberBorrowRenewReturnWorkflow_persistsCoordinatedState() {
        FileMemberRepository memberRepository = new FileMemberRepository(
                temporaryDirectory.resolve("members.json"));
        FileBookRepository bookRepository = new FileBookRepository(
                temporaryDirectory.resolve("books.json"));
        FileBookCopyRepository copyRepository = new FileBookCopyRepository(
                temporaryDirectory.resolve("copies.json"));
        FileLoanRepository loanRepository = new FileLoanRepository(
                temporaryDirectory.resolve("loans.json"));

        MemberService memberService = new MemberService(memberRepository);
        BookService bookService = new BookService(bookRepository, new BookCopyService(copyRepository));
        BookCopyService copyService = new BookCopyService(copyRepository);
        LoanService loanService = new LoanService(loanRepository);

        memberService.registerMember("Ada", "ada@example.com", "secret-password");
        Member member = memberRepository.findByEmail("ada@example.com").orElseThrow();
        assertDoesNotThrow(() -> bookService.createBook("978-1", "Algorithms", "Grace Hopper", "Tech Press",
                "Technology", 2020));
        assertDoesNotThrow(() -> copyService.createCopy("COPY-1", "978-1", CopyStatus.AVAILABLE, "A-1"));
        assertDoesNotThrow(() -> loanService.createLoan("LOAN-1", member.getMembershipId(), "COPY-1",
                LocalDate.of(2026, 1, 1)));

        copyService.borrowCopy("COPY-1");
        loanService.renewLoan("LOAN-1");
        loanService.returnLoan("LOAN-1");
        copyService.returnCopy("COPY-1");

        Member persistedMember = memberRepository.findByMembershipId(member.getMembershipId()).orElseThrow();
        BookCopy persistedCopy = new BookCopyService(copyRepository).getCopyById("COPY-1").orElseThrow();

        assertEquals(AccountStatus.ACTIVE, persistedMember.getStatus());
        assertEquals(CopyStatus.AVAILABLE, persistedCopy.getStatus());
        assertEquals(LoanStatus.RETURNED, new LoanService(loanRepository)
                .getLoanById("LOAN-1").orElseThrow().getStatus());
        assertTrue(new BookService(bookRepository, new BookCopyService(copyRepository))
                .getBookByIsbn("978-1").isPresent());
    }

    @Test
    void deletingBook_removesCopiesButLeavesOtherCatalogueData() {
        FileBookRepository bookRepository = new FileBookRepository(
                temporaryDirectory.resolve("books.json"));
        FileBookCopyRepository copyRepository = new FileBookCopyRepository(
                temporaryDirectory.resolve("copies.json"));
        BookCopyService copyService = new BookCopyService(copyRepository);
        BookService bookService = new BookService(bookRepository, copyService);

        bookService.createBook("978-1", "To Delete", "Author", "Publisher", "Category", 2020);
        bookService.createBook("978-2", "To Keep", "Author", "Publisher", "Category", 2021);
        copyService.createCopy("COPY-1", "978-1", CopyStatus.AVAILABLE, "A-1");
        copyService.createCopy("COPY-2", "978-2", CopyStatus.AVAILABLE, "A-2");

        bookService.deleteBook("978-1");

        assertFalse(bookService.getBookByIsbn("978-1").isPresent());
        assertTrue(copyService.getCopiesByIsbn("978-1").isEmpty());
        assertTrue(bookService.getBookByIsbn("978-2").isPresent());
        assertEquals(1, copyService.getCopiesByIsbn("978-2").size());
    }
}
