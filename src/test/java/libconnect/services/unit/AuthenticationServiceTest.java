package libconnect.services.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import libconnect.models.AccountStatus;
import libconnect.models.AccountType;
import libconnect.models.Member;
import libconnect.models.User;
import libconnect.services.AuthenticationException;
import libconnect.services.AuthenticationService;
import libconnect.services.LibrarianService;
import libconnect.services.MemberService;
import libconnect.services.ServiceException;
import libconnect.storage.file.FileMemberRepository;

/** Tests account registration and authentication behavior. */
class AuthenticationServiceTest {
    @TempDir
    Path temporaryDirectory;

    private FileMemberRepository memberRepository;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        memberRepository = new FileMemberRepository(temporaryDirectory.resolve("members.json"));
        authenticationService = new AuthenticationService(memberRepository, new LibrarianService());
    }

    @Test
    void registerMember_createsActiveMember() {
        User user = authenticationService.registerMember(
                AccountType.MEMBER, "Ada", "ada@example.com", "secret-password");

        Member member = assertInstanceOf(Member.class, user);
        assertEquals(AccountStatus.ACTIVE, member.getStatus());
        assertEquals(member, memberRepository.findByEmail("ada@example.com").orElseThrow());
    }

    @Test
    void authenticateMember_correctCredentialsIgnoringEmailCase_returnsMember() {
        authenticationService.registerMember(
                AccountType.MEMBER, "Ada", "ada@example.com", "secret-password");

        User authenticatedUser = authenticationService.authenticate(
                AccountType.MEMBER, " ADA@EXAMPLE.COM ", "secret-password");

        assertEquals("ada@example.com", authenticatedUser.getEmail());
    }

    @Test
    void authenticateMember_wrongPassword_throwsUsefulException() {
        authenticationService.registerMember(
                AccountType.MEMBER, "Ada", "ada@example.com", "secret-password");

        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate(
                        AccountType.MEMBER, "ada@example.com", "wrong-password"));

        assertEquals("Invalid email or password.", exception.getMessage());
    }

    @Test
    void authenticateMember_deactivatedAccount_throwsUsefulException() {
        authenticationService.registerMember(
                AccountType.MEMBER, "Ada", "ada@example.com", "secret-password");
        Member member = memberRepository.findByEmail("ada@example.com").orElseThrow();
        new MemberService(memberRepository).deactivateMember(member.getMembershipId());

        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate(
                        AccountType.MEMBER, "ada@example.com", "secret-password"));

        assertEquals("This account is deactivated and cannot log in.", exception.getMessage());
    }

    @Test
    // TODO: Replace this test with a proper librarian registration test once a librarian model and persistence service are available.
    void registerLibrarian_beforeLibrarianImplementation_throwsServiceException() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> authenticationService.registerMember(
                        AccountType.LIBRARIAN, "Grace", "grace@example.com", "secret-password"));

        assertEquals("Librarian account registration is not supported yet.", exception.getMessage());
    }

    @Test
    // TODO: Replace this test with a proper librarian authentication test once a librarian model and persistence service are available.
    void authenticateLibrarian_beforeLibrarianImplementation_throwsAuthenticationException() {
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate(
                        AccountType.LIBRARIAN, "grace@example.com", "secret-password"));

        assertEquals("Librarian authentication is not supported yet.", exception.getMessage());
    }

    @Test
    // TODO: Replace this test with a proper librarian service test once a librarian model and persistence service are available.
    void librarianServiceShell_hasNoUserIds() {
        assertFalse(new LibrarianService().findByUserId("USER-1").isPresent());
    }
}
