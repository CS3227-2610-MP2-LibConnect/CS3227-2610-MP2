package libconnect.services;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import libconnect.util.ValidationUtils;

/** Provides salted password hashing and constant-time password verification. */
public final class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 210_000;
    private static final int SALT_LENGTH = 16;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordHasher() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Hashes a plaintext password with a newly generated random salt.
     *
     * @param password the plaintext password to hash.
     * @return the algorithm, iteration count, salt, and hash in one encoded value.
     * @throws ServiceException if the configured password-hashing algorithm is unavailable.
     * @throws IllegalArgumentException if {@code password} is blank.
     */
    public static String hash(String password) {
        String requiredPassword = ValidationUtils.requireNonBlank(password, "password");
        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        return deriveEncodedHash(requiredPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
    }

    /**
     * Checks a plaintext password against a stored encoded hash.
     *
     * @param password the plaintext password to check.
     * @param encodedHash the stored encoded password hash.
     * @return true if the password matches; false if it does not or the stored value is invalid.
     * @throws IllegalArgumentException if {@code password} is blank.
     */
    public static boolean matches(String password, String encodedHash) {
        String requiredPassword = ValidationUtils.requireNonBlank(password, "password");
        if (encodedHash == null || encodedHash.isBlank()) {
            return false;
        }

        String[] parts = encodedHash.split("\\$", -1);
        if (parts.length != 4 || !ALGORITHM.equals(parts[0])) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
            if (iterations <= 0 || salt.length == 0 || expectedHash.length == 0) {
                return false;
            }

            char[] passwordCharacters = requiredPassword.toCharArray();
            try {
                byte[] actualHash = deriveHash(passwordCharacters, salt,
                        iterations, expectedHash.length * Byte.SIZE);
                return MessageDigest.isEqual(actualHash, expectedHash);
            } finally {
                Arrays.fill(passwordCharacters, '\0');
            }
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            return false;
        }
    }

    private static String deriveEncodedHash(char[] password, byte[] salt, int iterations,
                                            int keyLength) {
        try {
            byte[] hash = deriveHash(password, salt, iterations, keyLength);
            return ALGORITHM + "$" + iterations + "$"
                    + Base64.getEncoder().encodeToString(salt) + "$"
                    + Base64.getEncoder().encodeToString(hash);
        } catch (GeneralSecurityException exception) {
            throw new ServiceException("Unable to hash password", exception);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static byte[] deriveHash(char[] password, byte[] salt, int iterations, int keyLength)
            throws GeneralSecurityException {
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterations, keyLength);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(keySpec).getEncoded();
        } finally {
            keySpec.clearPassword();
        }
    }
}
