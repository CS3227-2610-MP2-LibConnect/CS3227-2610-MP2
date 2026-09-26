package libconnect.models;

import java.util.Objects;

import libconnect.util.ValidationUtils;

/**
 * Represents a user account in LibConnect.
 */
public abstract class User implements libconnect.storage.repositories.Identifiable {
    private final String userId;
    private String name;
    private String email;
    private String passwordHash;
    private AccountStatus status;

    /**
     * Creates an active user account.
     *
     * @param userId the stable identifier for the user.
     * @param name the user's display name.
     * @param email the user's email address.
     * @param passwordHash the hash of the user's password.
     * @throws IllegalArgumentException if any required value is blank or the email is invalid.
     */
    public User(String userId, String name, String email, String passwordHash) {
        this(userId, name, email, passwordHash, AccountStatus.ACTIVE);
    }

    /**
     * Creates a user account with a supplied account status.
     *
     * @param userId the stable identifier for the user.
     * @param name the user's display name.
     * @param email the user's email address.
     * @param passwordHash the hash of the user's password.
     * @param status the account status to restore.
     * @throws IllegalArgumentException if any required value is blank or the email is invalid.
     * @throws NullPointerException if {@code status} is null.
     */
    public User(String userId, String name, String email, String passwordHash,
                AccountStatus status) {
        this.userId = ValidationUtils.requireNonBlank(userId, "userId");
        this.name = ValidationUtils.requireNonBlank(name, "name");
        this.email = validateEmail(email);
        this.passwordHash = ValidationUtils.requireNonBlank(passwordHash, "passwordHash");
        this.status = Objects.requireNonNull(status, "status cannot be null");
    }

    /**
     * Returns the stable identifier for this user.
     *
     * @return the user ID.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Returns the user's display name.
     *
     * @return the user's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the user's email address.
     *
     * @return the user's email address.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the stored password hash.
     *
     * @return the password hash.
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Returns the current account status.
     *
     * @return the account status.
     */
    public AccountStatus getStatus() {
        return status;
    }

    /**
     * Returns whether this account is active.
     *
     * @return true if the account is active.
     */
    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    /**
     * Updates the user's profile details.
     *
     * @param name the user's new display name.
     * @param email the user's new email address.
     * @throws IllegalArgumentException if either value is invalid.
     */
    public void updateProfile(String name, String email) {
        this.name = ValidationUtils.requireNonBlank(name, "name");
        this.email = validateEmail(email);
    }

    /**
     * Replaces the stored password hash.
     *
     * @param passwordHash the new password hash.
     * @throws IllegalArgumentException if the hash is blank.
     */
    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = ValidationUtils.requireNonBlank(passwordHash, "passwordHash");
    }

    /**
     * Deactivates this user account.
     */
    public void deactivateAccount() {
        status = AccountStatus.DEACTIVATED;
    }

    /**
     * Activates this user account.
     */
    public void activateAccount() {
        status = AccountStatus.ACTIVE;
    }

    // tests are not written for equals as it is not used in the application, 
    // but it is implemented for potential future use cases
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User otherUser)) {
            return false;
        }
        return userId.equals(otherUser.userId);
    }

    @Override
    public String toString() {
        return "User{" + "userId='" + userId + '\'' + ", name='" + name + '\''
                + ", email='" + email + '\'' + ", status=" + status + '}';
    }

    private static String validateEmail(String email) {
        String trimmedEmail = ValidationUtils.requireNonBlank(email, "email");
        if (!trimmedEmail.contains("@") || trimmedEmail.startsWith("@")
                || trimmedEmail.endsWith("@")) {
            throw new IllegalArgumentException("email must be valid");
        }
        return trimmedEmail;
    }

    @Override 
    public String getId() {
        return userId;
    }
}
