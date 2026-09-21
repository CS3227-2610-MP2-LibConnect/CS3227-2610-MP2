package libconnect.storage.file.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import libconnect.models.AccountStatus;
import libconnect.models.Member;
import libconnect.models.User;
import libconnect.storage.file.FileMemberRepository;
import libconnect.storage.repositories.UserRepository;

/** Tests unit-level member filtering, searching, validation, and delegation. */
class FileMemberRepositoryTest {

    @Test
    void constructor_nullUserRepository_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new FileMemberRepository((UserRepository) null));
    }

    @Test
    void findAll_mixedUsers_returnsOnlyMembers() {
        User regularUser = createUser("USER-1", "user@example.com");
        Member firstMember = createMember("MEMBER-1", "first@example.com", "MEM-1",
                AccountStatus.ACTIVE);
        Member secondMember = createMember("MEMBER-2", "second@example.com", "MEM-2",
                AccountStatus.DEACTIVATED);
        FileMemberRepository repository = createRepository(regularUser, firstMember, secondMember);

        assertEquals(List.of(firstMember, secondMember), repository.findAll());
    }

    @Test
    void findAll_noMembers_returnsEmptyList() {
        FileMemberRepository repository = createRepository(
                createUser("USER-1", "user@example.com"));

        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void findByMembershipId_matchingId_returnsMember() {
        Member expectedMember = createMember("USER-1", "member@example.com", "MEM-1",
                AccountStatus.ACTIVE);
        FileMemberRepository repository = createRepository(expectedMember);

        assertEquals(Optional.of(expectedMember), repository.findByMembershipId("MEM-1"));
    }

    @Test
    void findByMembershipId_unknownId_returnsEmpty() {
        FileMemberRepository repository = createRepository(
                createMember("USER-1", "member@example.com", "MEM-1", AccountStatus.ACTIVE));

        assertTrue(repository.findByMembershipId("MEM-2").isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void findByMembershipId_blankId_throwsIllegalArgumentException(String membershipId) {
        FileMemberRepository repository = createRepository();

        assertThrows(IllegalArgumentException.class,
                () -> repository.findByMembershipId(membershipId));
    }

    @Test
    void findByUserId_matchingMemberId_returnsMember() {
        Member expectedMember = createMember("USER-1", "member@example.com", "MEM-1",
                AccountStatus.ACTIVE);
        FileMemberRepository repository = createRepository(expectedMember);

        assertEquals(Optional.of(expectedMember), repository.findByUserId("USER-1"));
    }

    @Test
    void findByUserId_regularUserId_returnsEmpty() {
        FileMemberRepository repository = createRepository(
                createUser("USER-1", "user@example.com"));

        assertTrue(repository.findByUserId("USER-1").isEmpty());
    }

    @Test
    void findByUserId_unknownId_returnsEmpty() {
        FileMemberRepository repository = createRepository(
                createMember("USER-1", "member@example.com", "MEM-1", AccountStatus.ACTIVE));

        assertTrue(repository.findByUserId("USER-2").isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void findByUserId_blankId_throwsIllegalArgumentException(String userId) {
        FileMemberRepository repository = createRepository();

        assertThrows(IllegalArgumentException.class, () -> repository.findByUserId(userId));
    }

    @Test
    void findByEmail_matchingMemberEmail_returnsMemberAndDelegatesLookup() {
        Member expectedMember = createMember("USER-1", "member@example.com", "MEM-1",
                AccountStatus.ACTIVE);
        InMemoryUserRepository userRepository = new InMemoryUserRepository(expectedMember);
        FileMemberRepository repository = new FileMemberRepository(userRepository);

        assertEquals(Optional.of(expectedMember), repository.findByEmail("member@example.com"));
        assertEquals("member@example.com", userRepository.lastRequestedEmail);
    }

    @Test
    void findByEmail_regularUserEmail_returnsEmpty() {
        User regularUser = createUser("USER-1", "user@example.com");
        FileMemberRepository repository = createRepository(regularUser);

        assertTrue(repository.findByEmail("user@example.com").isEmpty());
    }

    @Test
    void findByEmail_unknownEmail_returnsEmpty() {
        FileMemberRepository repository = createRepository(
                createMember("USER-1", "member@example.com", "MEM-1", AccountStatus.ACTIVE));

        assertTrue(repository.findByEmail("unknown@example.com").isEmpty());
    }

    @Test
    void findByName_caseInsensitivePartialMatch_returnsMatchingMembers() {
        Member firstMatch = createMember("USER-1", "Alice Tan", "alice@example.com", "MEM-1",
                AccountStatus.ACTIVE);
        Member secondMatch = createMember("USER-2", "Malice Lim", "malice@example.com", "MEM-2",
                AccountStatus.ACTIVE);
        Member nonMatch = createMember("USER-3", "Bob Tan", "bob@example.com", "MEM-3",
                AccountStatus.ACTIVE);
        FileMemberRepository repository = createRepository(firstMatch, secondMatch, nonMatch);

        assertEquals(List.of(firstMatch, secondMatch), repository.findByName(" ALI "));
    }

    @Test
    void findByName_noMatch_returnsEmptyList() {
        FileMemberRepository repository = createRepository(
                createMember("USER-1", "Alice Tan", "alice@example.com", "MEM-1",
                        AccountStatus.ACTIVE));

        assertTrue(repository.findByName("Bob").isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void findByName_blankName_throwsIllegalArgumentException(String name) {
        FileMemberRepository repository = createRepository();

        assertThrows(IllegalArgumentException.class, () -> repository.findByName(name));
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    void findByStatus_status_returnsMatchingMembers(AccountStatus status) {
        AccountStatus otherStatus = status == AccountStatus.ACTIVE
                ? AccountStatus.DEACTIVATED : AccountStatus.ACTIVE;
        Member matchingMember = createMember("USER-1", "matching@example.com", "MEM-1", status);
        Member otherMember = createMember("USER-2", "other@example.com", "MEM-2", otherStatus);
        FileMemberRepository repository = createRepository(matchingMember, otherMember);

        assertEquals(List.of(matchingMember), repository.findByStatus(status));
    }

    @Test
    void findByStatus_nullStatus_throwsNullPointerException() {
        FileMemberRepository repository = createRepository();

        assertThrows(NullPointerException.class, () -> repository.findByStatus(null));
    }

    @Test
    void save_member_delegatesToUserRepository() {
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        FileMemberRepository repository = new FileMemberRepository(userRepository);
        Member member = createMember("USER-1", "member@example.com", "MEM-1",
                AccountStatus.ACTIVE);

        repository.save(member);

        assertSame(member, userRepository.lastSavedUser);
    }

    @Test
    void save_nullMember_throwsNullPointerException() {
        FileMemberRepository repository = createRepository();

        assertThrows(NullPointerException.class, () -> repository.save(null));
    }

    private static FileMemberRepository createRepository(User... users) {
        return new FileMemberRepository(new InMemoryUserRepository(users));
    }

    private static User createUser(String userId, String email) {
        return new User(userId, "Regular User", email, "password-hash");
    }

    private static Member createMember(String userId, String email, String membershipId,
                                       AccountStatus status) {
        return createMember(userId, "Member User", email, membershipId, status);
    }

    private static Member createMember(String userId, String name, String email,
                                       String membershipId, AccountStatus status) {
        return new Member(userId, name, email, "password-hash", membershipId,
                LocalDate.of(2026, 1, 1), status);
    }

    /** Provides an in-memory user repository for isolated adapter tests. */
    private static final class InMemoryUserRepository implements UserRepository {
        private final List<User> users;
        private String lastRequestedEmail;
        private User lastSavedUser;

        private InMemoryUserRepository(User... users) {
            this.users = new ArrayList<>(List.of(users));
        }

        @Override
        public Optional<User> findByUserId(String userId) {
            return users.stream()
                    .filter(user -> user.getUserId().equals(userId))
                    .findFirst();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            lastRequestedEmail = email;
            return users.stream()
                    .filter(user -> user.getEmail().equals(email))
                    .findFirst();
        }

        @Override
        public List<User> findAll() {
            return List.copyOf(users);
        }

        @Override
        public void save(User user) {
            lastSavedUser = user;
        }
    }
}
