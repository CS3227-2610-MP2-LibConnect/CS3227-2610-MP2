package libconnect.models;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class UserTest {
    @Test
    void constructor_validValues_createsActiveUser() {
        User user = createUser();

        assertAll(
                () -> assertEquals("USER-1", user.getUserId()),
                () -> assertEquals("Alice Tan", user.getName()),
                () -> assertEquals("alice@example.com", user.getEmail()),
                () -> assertEquals("hashed-password", user.getPasswordHash()),
                () -> assertEquals(AccountStatus.ACTIVE, user.getStatus()),
                () -> assertTrue(user.isActive()));
    }

    @Test
    void constructor_surroundingWhitespace_trimsTextValues() {
        User user = new User(" USER-1 ", " Alice Tan ", " alice@example.com ",
                " hashed-password ");

        assertAll(
                () -> assertEquals("USER-1", user.getUserId()),
                () -> assertEquals("Alice Tan", user.getName()),
                () -> assertEquals("alice@example.com", user.getEmail()),
                () -> assertEquals("hashed-password", user.getPasswordHash()));
    }

    @ParameterizedTest
    @MethodSource("invalidRequiredValues")
    void constructor_blankRequiredValue_throwsIllegalArgumentException(
            String userId, String name, String passwordHash) {
        assertThrows(IllegalArgumentException.class,
                () -> new User(userId, name, "alice@example.com", passwordHash));
    }

    private static Stream<Arguments> invalidRequiredValues() {
        return Stream.of(
                Arguments.of(null, "Alice Tan", "hashed-password"),
                Arguments.of("", "Alice Tan", "hashed-password"),
                Arguments.of("   ", "Alice Tan", "hashed-password"),
                Arguments.of("USER-1", null, "hashed-password"),
                Arguments.of("USER-1", "", "hashed-password"),
                Arguments.of("USER-1", "   ", "hashed-password"),
                Arguments.of("USER-1", "Alice Tan", null),
                Arguments.of("USER-1", "Alice Tan", ""),
                Arguments.of("USER-1", "Alice Tan", "   "));
    }

    @ParameterizedTest
    @MethodSource("invalidEmails")
    void constructor_invalidEmail_throwsIllegalArgumentException(String email) {
        assertThrows(IllegalArgumentException.class,
                () -> new User("USER-1", "Alice Tan", email, "hashed-password"));
    }

    private static Stream<String> invalidEmails() {
        return Stream.of(null, "", "   ", "plainaddress", "@example.com", "alice@");
    }

    @Test
    void constructor_minimallyAcceptedEmail_createsUser() {
        User user = new User("USER-1", "Alice Tan", "a@b", "hashed-password");

        assertEquals("a@b", user.getEmail());
    }

    @Test
    void updateProfile_validValues_updatesNameAndEmail() {
        User user = createUser();

        user.updateProfile(" Bob Lim ", " bob@example.com ");

        assertAll(
                () -> assertEquals("Bob Lim", user.getName()),
                () -> assertEquals("bob@example.com", user.getEmail()));
    }

    @Test
    void updateProfile_invalidName_throwsExceptionAndPreservesProfile() {
        User user = createUser();

        assertThrows(IllegalArgumentException.class,
                () -> user.updateProfile("   ", "bob@example.com"));

        assertAll(
                () -> assertEquals("Alice Tan", user.getName()),
                () -> assertEquals("alice@example.com", user.getEmail()));
    }

    @Test
    void updateProfile_invalidEmail_throwsException() {
        User user = createUser();

        assertThrows(IllegalArgumentException.class,
                () -> user.updateProfile("Bob Lim", "invalid-email"));
    }

    @Test
    void updatePasswordHash_validValue_updatesHash() {
        User user = createUser();

        user.updatePasswordHash("new-hashed-password");

        assertEquals("new-hashed-password", user.getPasswordHash());
    }

    @ParameterizedTest
    @MethodSource("invalidPasswordHashes")
    void updatePasswordHash_invalidValue_throwsExceptionAndPreservesHash(String passwordHash) {
        User user = createUser();

        assertThrows(IllegalArgumentException.class,
                () -> user.updatePasswordHash(passwordHash));
        assertEquals("hashed-password", user.getPasswordHash());
    }

    private static Stream<String> invalidPasswordHashes() {
        return Stream.of(null, "", "   ");
    }

    @Test
    void deactivateAccount_activeUser_marksUserDeactivated() {
        User user = createUser();

        user.deactivateAccount();

        assertAll(
                () -> assertEquals(AccountStatus.DEACTIVATED, user.getStatus()),
                () -> assertFalse(user.isActive()));
    }

    @Test
    void deactivateAccount_alreadyDeactivatedUser_remainsDeactivated() {
        User user = createUser();
        user.deactivateAccount();

        user.deactivateAccount();

        assertEquals(AccountStatus.DEACTIVATED, user.getStatus());
    }

    @Test
    void activateAccount_deactivatedUser_marksUserActive() {
        User user = createUser();
        user.deactivateAccount();

        user.activateAccount();

        assertAll(
                () -> assertEquals(AccountStatus.ACTIVE, user.getStatus()),
                () -> assertTrue(user.isActive()));
    }

    @Test
    void activateAccount_alreadyActiveUser_remainsActive() {
        User user = createUser();

        user.activateAccount();

        assertEquals(AccountStatus.ACTIVE, user.getStatus());
    }

    @Test
    void toString_containsNonSensitiveUserDetails() {
        User user = createUser();
        String representation = user.toString();

        assertAll(
                () -> assertTrue(representation.contains("USER-1")),
                () -> assertTrue(representation.contains("Alice Tan")),
                () -> assertTrue(representation.contains("alice@example.com")),
                () -> assertTrue(representation.contains("ACTIVE")));
    }

    @Test
    void toString_doesNotExposePasswordHash() {
        User user = createUser();

        assertFalse(user.toString().contains("hashed-password"));
    }

    private static User createUser() {
        return new User("USER-1", "Alice Tan", "alice@example.com", "hashed-password");
    }
}
