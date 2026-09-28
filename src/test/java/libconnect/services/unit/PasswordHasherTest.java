package libconnect.services.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import libconnect.services.PasswordHasher;

/** Tests password hashing, verification, and malformed-hash handling. */
class PasswordHasherTest {
    @Test
    void hash_matchesOriginalPasswordAndUsesExpectedFormat() {
        String encodedHash = PasswordHasher.hash("secret-password");

        assertTrue(encodedHash.startsWith("PBKDF2WithHmacSHA256$210000$"));
        assertTrue(PasswordHasher.matches("secret-password", encodedHash));
        assertFalse(PasswordHasher.matches("wrong-password", encodedHash));
    }

    @Test
    void hash_samePassword_generatesDifferentSaltedHashes() {
        assertNotEquals(PasswordHasher.hash("secret-password"), PasswordHasher.hash("secret-password"));
    }

    @Test
    void hashAndMatches_blankPasswords_rejected() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash("   "));
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.matches("   ", "invalid"));
    }

    @Test
    void matches_invalidStoredHashes_returnsFalse() {
        assertFalse(PasswordHasher.matches("password", null));
        assertFalse(PasswordHasher.matches("password", ""));
        assertFalse(PasswordHasher.matches("password", "invalid"));
        assertFalse(PasswordHasher.matches("password", "PBKDF2WithHmacSHA256$bad$salt$hash"));

        String validHash = PasswordHasher.hash("password");
        char finalCharacter = validHash.charAt(validHash.length() - 1);
        char replacementCharacter = finalCharacter == 'A' ? 'B' : 'A';
        String tamperedHash = validHash.substring(0, validHash.length() - 1) + replacementCharacter;
        assertFalse(PasswordHasher.matches("password", tamperedHash));
        assertEquals(false, PasswordHasher.matches("password", "PBKDF2WithHmacSHA256$0$AQ==$AQ=="));
    }
}
