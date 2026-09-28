package libconnect.services.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import libconnect.models.AccountStatus;
import libconnect.models.Member;
import libconnect.services.UserIdGenerator;
import libconnect.services.UserService;

/** Tests generation of globally unique user identifiers. */
class UserIdGeneratorTest {
    @Test
    void generate_returnsUserPrefixedIdentifiers() {
        UserIdGenerator generator = new UserIdGenerator(new UserService(
                new ServiceUnitTestDoubles.Members(), new ServiceUnitTestDoubles.Librarians()));

        String firstId = generator.generate();
        String secondId = generator.generate();

        assertTrue(firstId.startsWith("USER-"));
        assertTrue(secondId.startsWith("USER-"));
        assertNotEquals(firstId, secondId);
    }

    @Test
    void generate_doesNotReuseExistingMemberOrLibrarianIds() {
        ServiceUnitTestDoubles.Members members = new ServiceUnitTestDoubles.Members();
        ServiceUnitTestDoubles.Librarians librarians = new ServiceUnitTestDoubles.Librarians();
        members.save(new Member("USER-existing-member", "Member", "member@example.com", "hash", "MEMBER-1",
                java.time.LocalDate.of(2026, 1, 1), AccountStatus.ACTIVE));
        librarians.save(new libconnect.models.Librarian("USER-existing-librarian", "EMP-1", "Librarian",
                "librarian@example.com", "hash", AccountStatus.ACTIVE));
        UserIdGenerator generator = new UserIdGenerator(new UserService(members, librarians));

        Set<String> generatedIds = new HashSet<>();
        for (int index = 0; index < 20; index++) {
            generatedIds.add(generator.generate());
        }

        assertFalse(generatedIds.contains("USER-existing-member"));
        assertFalse(generatedIds.contains("USER-existing-librarian"));
        assertEquals(20, generatedIds.size());
    }
}
