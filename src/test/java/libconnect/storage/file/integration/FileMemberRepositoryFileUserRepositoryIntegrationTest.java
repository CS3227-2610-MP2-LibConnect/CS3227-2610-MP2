package libconnect.storage.file.integration;

import libconnect.storage.file.FileMemberRepository;
import libconnect.storage.file.FileUserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import libconnect.models.AccountStatus;
import libconnect.models.Member;
import libconnect.models.User;

/** Tests interoperability between the member and shared user file repositories. */
class FileMemberRepositoryFileUserRepositoryIntegrationTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void saveMemberThroughMemberRepository_isReadableThroughUserRepository() {
        Path dataFile = dataFile();
        FileMemberRepository memberRepository = new FileMemberRepository(dataFile);
        FileUserRepository userRepository = new FileUserRepository(dataFile);
        Member member = createMember("USER-1", "Alice Tan", "alice@example.com", "MEM-1",
                AccountStatus.ACTIVE);

        memberRepository.save(member);

        Member restoredMember = (Member) userRepository.findByUserId("USER-1").orElseThrow();
        assertMemberEquals(member, restoredMember);
    }

    @Test
    void saveMemberThroughUserRepository_isReturnedByMemberRepository() {
        Path dataFile = dataFile();
        FileUserRepository userRepository = new FileUserRepository(dataFile);
        FileMemberRepository memberRepository = new FileMemberRepository(dataFile);
        Member member = createMember("USER-1", "Alice Tan", "alice@example.com", "MEM-1",
                AccountStatus.ACTIVE);

        userRepository.save(member);

        assertEquals(List.of(member), memberRepository.findAll());
        assertEquals(member, memberRepository.findByUserId("USER-1").orElseThrow());
        assertEquals(member, memberRepository.findByMembershipId("MEM-1").orElseThrow());
    }

    @Test
    void mixedUsersAndMembers_memberRepositoryReturnsOnlyMembers() {
        Path dataFile = dataFile();
        FileUserRepository userRepository = new FileUserRepository(dataFile);
        FileMemberRepository memberRepository = new FileMemberRepository(dataFile);
        User user = createUser("USER-1", "user@example.com");
        Member firstMember = createMember("USER-2", "Alice Tan", "alice@example.com", "MEM-1",
                AccountStatus.ACTIVE);
        Member secondMember = createMember("USER-3", "Bob Lim", "bob@example.com", "MEM-2",
                AccountStatus.DEACTIVATED);

        userRepository.save(user);
        userRepository.save(firstMember);
        userRepository.save(secondMember);

        assertEquals(List.of(firstMember, secondMember), memberRepository.findAll());
        assertEquals(3, userRepository.findAll().size());
    }

    @Test
    void saveMember_replacesExistingUserWithSameUserIdAcrossRepositories() {
        Path dataFile = dataFile();
        FileMemberRepository memberRepository = new FileMemberRepository(dataFile);
        FileUserRepository userRepository = new FileUserRepository(dataFile);
        Member originalMember = createMember("USER-1", "Alice Tan", "alice@example.com", "MEM-1",
                AccountStatus.ACTIVE);
        Member replacementMember = createMember("USER-1", "Alice Lim", "alice.lim@example.com",
                "MEM-2", AccountStatus.DEACTIVATED);

        memberRepository.save(originalMember);
        memberRepository.save(replacementMember);

        assertEquals(1, userRepository.findAll().size());
        assertMemberEquals(replacementMember,
                (Member) userRepository.findByUserId("USER-1").orElseThrow());
    }

    @Test
    void memberSavedByOneRepository_isVisibleToFreshRepositoryInstances() {
        Path dataFile = dataFile();
        Member member = createMember("USER-1", "Alice Tan", "alice@example.com", "MEM-1",
                AccountStatus.ACTIVE);

        new FileMemberRepository(dataFile).save(member);

        FileMemberRepository freshMemberRepository = new FileMemberRepository(dataFile);
        FileUserRepository freshUserRepository = new FileUserRepository(dataFile);
        assertEquals(List.of(member), freshMemberRepository.findAll());
        assertEquals(member, freshUserRepository.findByUserId("USER-1").orElseThrow());
    }

    @Test
    void memberLookupByEmail_usesSharedPersistedData() {
        Path dataFile = dataFile();
        FileUserRepository userRepository = new FileUserRepository(dataFile);
        FileMemberRepository memberRepository = new FileMemberRepository(dataFile);
        Member member = createMember("USER-1", "Alice Tan", "alice@example.com", "MEM-1",
                AccountStatus.ACTIVE);
        userRepository.save(member);

        assertEquals(member,
                memberRepository.findByEmail("  ALICE@EXAMPLE.COM  ").orElseThrow());
    }

    @Test
    void memberStatusUpdate_isObservedThroughBothRepositories() {
        Path dataFile = dataFile();
        FileMemberRepository memberRepository = new FileMemberRepository(dataFile);
        FileUserRepository userRepository = new FileUserRepository(dataFile);
        Member member = createMember("USER-1", "Alice Tan", "alice@example.com", "MEM-1",
                AccountStatus.ACTIVE);
        memberRepository.save(member);

        member.deactivateAccount();
        memberRepository.save(member);

        assertEquals(List.of(member), memberRepository.findByStatus(AccountStatus.DEACTIVATED));
        assertEquals(AccountStatus.DEACTIVATED,
                userRepository.findByUserId("USER-1").orElseThrow().getStatus());
    }

    @Test
    void regularUserAndMemberWithConflictingEmail_cannotBePersisted() {
        Path dataFile = dataFile();
        FileUserRepository userRepository = new FileUserRepository(dataFile);
        FileMemberRepository memberRepository = new FileMemberRepository(dataFile);
        User user = createUser("USER-1", "shared@example.com");
        Member conflictingMember = createMember("USER-2", "Alice Tan", "SHARED@EXAMPLE.COM",
                "MEM-1", AccountStatus.ACTIVE);
        userRepository.save(user);

        assertThrows(IllegalArgumentException.class, () -> memberRepository.save(conflictingMember));

        assertEquals(List.of(user), userRepository.findAll());
        assertTrue(memberRepository.findAll().isEmpty());
    }

    private Path dataFile() {
        return temporaryDirectory.resolve("users.json");
    }

    private static User createUser(String userId, String email) {
        return new User(userId, "Regular User", email, "password-hash");
    }

    private static Member createMember(String userId, String name, String email,
                                       String membershipId, AccountStatus status) {
        return new Member(userId, name, email, "password-hash", membershipId,
                LocalDate.of(2026, 1, 1), status);
    }

    private static void assertMemberEquals(Member expected, Member actual) {
        assertEquals(expected.getUserId(), actual.getUserId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getEmail(), actual.getEmail());
        assertEquals(expected.getPasswordHash(), actual.getPasswordHash());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getMembershipId(), actual.getMembershipId());
        assertEquals(expected.getRegistrationDate(), actual.getRegistrationDate());
    }
}
