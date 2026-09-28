package libconnect.services.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import libconnect.models.AccountStatus;
import libconnect.models.Librarian;
import libconnect.models.Member;
import libconnect.models.User;
import libconnect.services.UserService;

/** Tests account lookup across member and librarian repositories. */
class UserServiceTest {
    @Test
    void isEmailInUse_checksBothAccountTypes() {
        ServiceUnitTestDoubles.Members members = new ServiceUnitTestDoubles.Members();
        ServiceUnitTestDoubles.Librarians librarians = new ServiceUnitTestDoubles.Librarians();
        members.save(member("USER-1", "MEMBER-1", "member@example.com"));
        librarians.save(librarian("USER-2", "EMP-1", "librarian@example.com"));
        UserService service = new UserService(members, librarians);

        assertTrue(service.isEmailInUse("member@example.com"));
        assertTrue(service.isEmailInUse("LIBRARIAN@EXAMPLE.COM"));
        assertFalse(service.isEmailInUse("unknown@example.com"));
    }

    @Test
    void isUserIdInUse_checksBothAccountTypes() {
        ServiceUnitTestDoubles.Members members = new ServiceUnitTestDoubles.Members();
        ServiceUnitTestDoubles.Librarians librarians = new ServiceUnitTestDoubles.Librarians();
        members.save(member("USER-1", "MEMBER-1", "member@example.com"));
        librarians.save(librarian("USER-2", "EMP-1", "librarian@example.com"));
        UserService service = new UserService(members, librarians);

        assertTrue(service.isUserIdInUse("USER-1"));
        assertTrue(service.isUserIdInUse("USER-2"));
        assertFalse(service.isUserIdInUse("USER-3"));
    }

    @Test
    void findUserByEmail_returnsCorrectConcreteAccountType() {
        ServiceUnitTestDoubles.Members members = new ServiceUnitTestDoubles.Members();
        ServiceUnitTestDoubles.Librarians librarians = new ServiceUnitTestDoubles.Librarians();
        Member member = member("USER-1", "MEMBER-1", "member@example.com");
        Librarian librarian = librarian("USER-2", "EMP-1", "librarian@example.com");
        members.save(member);
        librarians.save(librarian);
        UserService service = new UserService(members, librarians);

        assertEquals(member, service.findUserByEmail("member@example.com").orElseThrow());
        assertEquals(librarian, service.findUserByEmail("LIBRARIAN@EXAMPLE.COM").orElseThrow());
        assertTrue(service.findUserByEmail("unknown@example.com").isEmpty());
    }

    private static Member member(String userId, String membershipId, String email) {
        return new Member(userId, "Member", email, "password-hash", membershipId,
                java.time.LocalDate.of(2026, 1, 1), AccountStatus.ACTIVE);
    }

    private static Librarian librarian(String userId, String employeeId, String email) {
        return new Librarian(userId, employeeId, "Librarian", email, "password-hash", AccountStatus.ACTIVE);
    }
}
