package libconnect.services.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import libconnect.models.AccountStatus;
import libconnect.models.AccountType;
import libconnect.models.Librarian;
import libconnect.models.Member;
import libconnect.models.User;
import libconnect.services.AuthenticationException;
import libconnect.services.AuthenticationService;
import libconnect.services.LibrarianService;
import libconnect.services.MemberService;
import libconnect.services.NotFoundException;
import libconnect.services.ServiceException;
import libconnect.services.UserService;
import libconnect.storage.StorageManager;
import libconnect.storage.file.FileLibrarianRepository;
import libconnect.storage.file.FileMemberRepository;

/** Tests account registration and authentication behavior. */
class AuthenticationServiceTest {
    @TempDir
    Path temporaryDirectory;

    private FileMemberRepository memberRepository;
    private FileLibrarianRepository librarianRepository;
    private LibrarianService librarianService;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        memberRepository = new FileMemberRepository(temporaryDirectory.resolve("members.json"));
        librarianRepository = new FileLibrarianRepository(new StorageManager(temporaryDirectory),
                temporaryDirectory.resolve("librarians.json"));
        MemberService memberService = new MemberService(memberRepository);
        librarianService = new LibrarianService(librarianRepository, memberRepository);
        UserService userService = new UserService(memberRepository, librarianRepository);
        authenticationService = new AuthenticationService(memberService, librarianService, userService);
    }

    @Test
    void registerMember_createsActiveMember() {
        User user = authenticationService.registerMember("Ada", "ada@example.com", "secret-password");

        Member member = assertInstanceOf(Member.class, user);
        assertEquals(AccountStatus.ACTIVE, member.getStatus());
        assertEquals(member, memberRepository.findByEmail("ada@example.com").orElseThrow());
    }

    @Test
    void authenticateMember_correctCredentialsIgnoringEmailCase_returnsMember() {
        authenticationService.registerMember("Ada", "ada@example.com", "secret-password");

        User authenticatedUser = authenticationService.authenticate(
                AccountType.MEMBER, " ADA@EXAMPLE.COM ", "secret-password");

        assertEquals("ada@example.com", authenticatedUser.getEmail());
    }

    @Test
    void authenticateMember_wrongPassword_throwsUsefulException() {
        authenticationService.registerMember("Ada", "ada@example.com", "secret-password");

        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate(
                        AccountType.MEMBER, "ada@example.com", "wrong-password"));

        assertEquals("Invalid email or password.", exception.getMessage());
    }

    @Test
    void authenticate_unknownEmail_throwsUsefulException() {
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate(
                        AccountType.MEMBER, "unknown@example.com", "secret-password"));

        assertEquals("Invalid email or password.", exception.getMessage());
    }

    @Test
    void authenticate_selectedAccountTypeMustMatchStoredUserType() {
        authenticationService.registerMember("Ada", "ada@example.com", "secret-password");

        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate(
                        AccountType.LIBRARIAN, "ada@example.com", "secret-password"));

        assertEquals("Invalid librarian account.", exception.getMessage());
    }

    @Test
    void authenticate_blankEmailOrPassword_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> authenticationService.authenticate(AccountType.MEMBER, "   ", "password"));
        assertThrows(IllegalArgumentException.class,
                () -> authenticationService.authenticate(AccountType.MEMBER, "ada@example.com", "   "));
    }

    @Test
    void authenticate_nullAccountType_rejected() {
        assertThrows(NullPointerException.class,
                () -> authenticationService.authenticate(null, "ada@example.com", "password"));
    }

    @Test
    void constructor_nullDependencies_rejected() {
        assertThrows(NullPointerException.class,
                () -> new AuthenticationService(null, librarianService, new UserService()));
        assertThrows(NullPointerException.class,
                () -> new AuthenticationService(new MemberService(memberRepository), null, new UserService()));
        assertThrows(NullPointerException.class,
                () -> new AuthenticationService(new MemberService(memberRepository), librarianService, null));
    }

    @Test
    void authenticateMember_deactivatedAccount_throwsUsefulException() {
        authenticationService.registerMember("Ada", "ada@example.com", "secret-password");
        Member member = memberRepository.findByEmail("ada@example.com").orElseThrow();
        new MemberService(memberRepository).deactivateMember(member.getMembershipId());

        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate(
                        AccountType.MEMBER, "ada@example.com", "secret-password"));

        assertEquals("This account is deactivated and cannot log in.", exception.getMessage());
    }

    @Test
    void registerLibrarian_createsActiveLibrarian() {
        User user = authenticationService.registerLibrarian(
                "e1", "Grace", "grace@example.com", "secret-password");

        Librarian librarian = assertInstanceOf(Librarian.class, user);
        assertEquals(AccountStatus.ACTIVE, librarian.getStatus());
        assertEquals(librarian, librarianRepository.findByEmail("grace@example.com").orElseThrow());
    }

    @Test
    void registerLibrarian_emailUsedByMember_rejected() {
        authenticationService.registerMember("Ada", "ada@example.com", "secret-password");

        assertThrows(IllegalStateException.class,
                () -> authenticationService.registerLibrarian(
                        "e1", "Grace", "ada@example.com", "secret-password"));
    }

    @Test
    void registerMember_accountCannotBeReloaded_reportsServiceFailure() {
        MemberService unavailableMemberService = new MemberService(memberRepository) {
            @Override
            public void registerMember(String name, String email, String password) {
                // Simulate a persistence layer that completes without returning the new record.
            }

            @Override
            public Member findMemberByEmail(String email) {
                throw new NotFoundException("Member not found");
            }
        };
        AuthenticationService service = new AuthenticationService(unavailableMemberService,
                librarianService, new UserService(memberRepository, librarianRepository));

        assertThrows(ServiceException.class,
                () -> service.registerMember("Ada", "ada@example.com", "secret-password"));
    }

    @Test
    void authenticateLibrarian_correctCredentials_returnsLibrarian() {
        authenticationService.registerLibrarian("e1", "Grace", "grace@example.com", "secret-password");

        User authenticatedUser = authenticationService.authenticate(
                AccountType.LIBRARIAN, "grace@example.com", "secret-password");

        assertInstanceOf(Librarian.class, authenticatedUser);
        assertEquals("grace@example.com", authenticatedUser.getEmail());
    }

}
