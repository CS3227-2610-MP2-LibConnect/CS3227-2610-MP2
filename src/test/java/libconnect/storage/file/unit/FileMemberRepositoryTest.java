package libconnect.storage.file.unit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import libconnect.models.AccountStatus;
import libconnect.models.Member;
import libconnect.storage.file.FileMemberRepository;

/** Tests unit-level JSON persistence and member-specific queries. */
class FileMemberRepositoryTest extends AbstractFileRepositoryTest<Member, FileMemberRepository> {

    @Override
    protected FileMemberRepository createRepository(Path dataFile) {
        return new FileMemberRepository(dataFile);
    }

    @Override
    protected void save(FileMemberRepository repository, Member member) {
        repository.save(member);
    }

    @Override
    protected List<Member> findAll(FileMemberRepository repository) {
        return repository.findAll();
    }

    @Override
    protected String getIdentifier(Member member) {
        return member.getMembershipId();
    }

    @Override
    protected Member createEntity(String identifier, String variant) {
        return createMember("USER-" + variant, "Member " + variant,
                "member-" + variant.toLowerCase() + "@example.com", identifier,
                AccountStatus.ACTIVE, LocalDate.of(2026, 1, 1));
    }

    @Test
    void constructor_nullDataFile_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new FileMemberRepository(null));
    }

    @Test
    void save_preservesAllMemberFieldsAfterReload() {
        FileMemberRepository repository = createRepository(
                temporaryDirectory.resolve("members.json"));
        Member member = createMember("USER-1", "Alice Tan", "alice@example.com", "MEM-1",
                AccountStatus.DEACTIVATED, LocalDate.of(2025, 12, 31));

        repository.save(member);

        Member savedMember = repository.findByMembershipId("MEM-1").orElseThrow();
        assertAll(
                () -> assertEquals("USER-1", savedMember.getUserId()),
                () -> assertEquals("Alice Tan", savedMember.getName()),
                () -> assertEquals("alice@example.com", savedMember.getEmail()),
                () -> assertEquals("password-hash", savedMember.getPasswordHash()),
                () -> assertEquals("MEM-1", savedMember.getMembershipId()),
                () -> assertEquals(LocalDate.of(2025, 12, 31), savedMember.getRegistrationDate()),
                () -> assertEquals(AccountStatus.DEACTIVATED, savedMember.getStatus()));
    }

    @Test
    void findByMembershipId_matchingId_returnsMember() {
        Member expectedMember = createMember("USER-1", "Alice Tan", "alice@example.com", "MEM-1",
                AccountStatus.ACTIVE, LocalDate.of(2026, 1, 1));
        FileMemberRepository repository = createRepository(
                temporaryDirectory.resolve("members.json"));
        repository.save(expectedMember);

        assertEquals(Optional.of(expectedMember), repository.findByMembershipId(" MEM-1 "));
    }

    @Test
    void findByMembershipId_unknownId_returnsEmptyOptional() {
        FileMemberRepository repository = createRepository(
                temporaryDirectory.resolve("members.json"));

        assertTrue(repository.findByMembershipId("MEM-2").isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void findByMembershipId_blankId_throwsIllegalArgumentException(String membershipId) {
        FileMemberRepository repository = createRepository(
                temporaryDirectory.resolve("members.json"));

        assertThrows(IllegalArgumentException.class,
                () -> repository.findByMembershipId(membershipId));
    }

    @Test
    void findByUserId_matchingUserId_returnsMember() {
        Member expectedMember = createMember("USER-1", "Alice Tan", "alice@example.com", "MEM-1",
                AccountStatus.ACTIVE, LocalDate.of(2026, 1, 1));
        FileMemberRepository repository = createRepository(
                temporaryDirectory.resolve("members.json"));
        repository.save(expectedMember);

        assertEquals(Optional.of(expectedMember), repository.findByUserId("USER-1"));
    }

    @Test
    void findByUserId_unknownId_returnsEmptyOptional() {
        FileMemberRepository repository = createRepository(
                temporaryDirectory.resolve("members.json"));

        assertTrue(repository.findByUserId("USER-2").isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void findByUserId_blankId_throwsIllegalArgumentException(String userId) {
        FileMemberRepository repository = createRepository(
                temporaryDirectory.resolve("members.json"));

        assertThrows(IllegalArgumentException.class, () -> repository.findByUserId(userId));
    }

    @Test
    void findByEmail_matchingEmailIgnoringCaseAndWhitespace_returnsMember() {
        Member expectedMember = createMember("USER-1", "Alice Tan", "alice@example.com", "MEM-1",
                AccountStatus.ACTIVE, LocalDate.of(2026, 1, 1));
        FileMemberRepository repository = createRepository(
                temporaryDirectory.resolve("members.json"));
        repository.save(expectedMember);

        assertEquals(Optional.of(expectedMember), repository.findByEmail(" ALICE@EXAMPLE.COM "));
    }

    @Test
    void findByEmail_unknownEmail_returnsEmptyOptional() {
        FileMemberRepository repository = createRepository(
                temporaryDirectory.resolve("members.json"));

        assertTrue(repository.findByEmail("unknown@example.com").isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void findByEmail_blankEmail_throwsIllegalArgumentException(String email) {
        FileMemberRepository repository = createRepository(
                temporaryDirectory.resolve("members.json"));

        assertThrows(IllegalArgumentException.class, () -> repository.findByEmail(email));
    }

    @Test
    void findByName_caseInsensitivePartialMatch_returnsMatchingMembers() {
        Member firstMatch = createMember("USER-1", "Alice Tan", "alice@example.com", "MEM-1",
                AccountStatus.ACTIVE, LocalDate.of(2026, 1, 1));
        Member secondMatch = createMember("USER-2", "Malice Lim", "malice@example.com", "MEM-2",
                AccountStatus.ACTIVE, LocalDate.of(2026, 1, 1));
        Member nonMatch = createMember("USER-3", "Bob Tan", "bob@example.com", "MEM-3",
                AccountStatus.ACTIVE, LocalDate.of(2026, 1, 1));
        FileMemberRepository repository = createRepository(
                temporaryDirectory.resolve("members.json"));
        repository.save(firstMatch);
        repository.save(secondMatch);
        repository.save(nonMatch);

        assertEquals(List.of(firstMatch, secondMatch), repository.findByName(" ALI "));
    }

    @Test
    void findByName_noMatch_returnsEmptyList() {
        FileMemberRepository repository = createRepository(
                temporaryDirectory.resolve("members.json"));
        repository.save(createMember("USER-1", "Alice Tan", "alice@example.com", "MEM-1",
                AccountStatus.ACTIVE, LocalDate.of(2026, 1, 1)));

        assertTrue(repository.findByName("Bob").isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void findByName_blankName_throwsIllegalArgumentException(String name) {
        FileMemberRepository repository = createRepository(
                temporaryDirectory.resolve("members.json"));

        assertThrows(IllegalArgumentException.class, () -> repository.findByName(name));
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    void findByStatus_status_returnsMatchingMembers(AccountStatus status) {
        AccountStatus otherStatus = status == AccountStatus.ACTIVE
                ? AccountStatus.DEACTIVATED : AccountStatus.ACTIVE;
        Member matchingMember = createMember("USER-1", "Matching Member", "matching@example.com",
                "MEM-1", status, LocalDate.of(2026, 1, 1));
        Member otherMember = createMember("USER-2", "Other Member", "other@example.com", "MEM-2",
                otherStatus, LocalDate.of(2026, 1, 1));
        FileMemberRepository repository = createRepository(
                temporaryDirectory.resolve("members.json"));
        repository.save(matchingMember);
        repository.save(otherMember);

        assertEquals(List.of(matchingMember), repository.findByStatus(status));
    }

    @Test
    void findByStatus_nullStatus_throwsNullPointerException() {
        FileMemberRepository repository = createRepository(
                temporaryDirectory.resolve("members.json"));

        assertThrows(NullPointerException.class, () -> repository.findByStatus(null));
    }

    private static Member createMember(String userId, String name, String email,
                                       String membershipId, AccountStatus status,
                                       LocalDate registrationDate) {
        return new Member(userId, name, email, "password-hash", membershipId,
                registrationDate, status);
    }
}
