package libconnect.models;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class MemberTest {
    @Test
    void constructor_validValues_createsActiveMember() {
        Member member = createMember();

        assertAll(
                () -> assertEquals("USER-1", member.getUserId()),
                () -> assertEquals("Alice Tan", member.getName()),
                () -> assertEquals("alice@example.com", member.getEmail()),
                () -> assertEquals("hashed-password", member.getPasswordHash()),
                () -> assertEquals(AccountStatus.ACTIVE, member.getStatus()),
                () -> assertEquals("MEMBER-1", member.getMembershipId()),
                () -> assertEquals(LocalDate.now(), member.getRegistrationDate()));
    }

    @Test
    void constructor_surroundingWhitespace_trimsMembershipId() {
        Member member = new Member("USER-1", "Alice Tan", "alice@example.com",
                "hashed-password", " MEMBER-1 ");

        assertEquals("MEMBER-1", member.getMembershipId());
    }

    @Test
    void constructor_nullMembershipId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Member("USER-1", "Alice Tan", "alice@example.com",
                        "hashed-password", null));
    }

    @Test
    void constructor_blankMembershipId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Member("USER-1", "Alice Tan", "alice@example.com",
                        "hashed-password", "   "));
    }

    @Test
    void constructor_invalidInheritedUserDetails_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Member("", "Alice Tan", "alice@example.com",
                        "hashed-password", "MEMBER-1"));
    }

    @Test
    void getMembershipId_returnsStoredMembershipId() {
        Member member = createMember();

        assertEquals("MEMBER-1", member.getMembershipId());
    }

    @Test
    void getRegistrationDate_returnsStoredRegistrationDate() {
        Member member = createMember();

        assertEquals(LocalDate.now(), member.getRegistrationDate());
    }

    private static Member createMember() {
        return new Member("USER-1", "Alice Tan", "alice@example.com",
                "hashed-password", "MEMBER-1");
    }
}
