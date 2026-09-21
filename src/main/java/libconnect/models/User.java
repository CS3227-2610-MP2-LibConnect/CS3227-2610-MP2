package libconnect.models;

/**
 * Represents a user account in LibConnect.
 */
public class User {
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
        this.userId = requireNonBlank(userId, "userId");
        this.name = requireNonBlank(name, "name");
        this.email = validateEmail(email);
        this.passwordHash = requireNonBlank(passwordHash, "passwordHash");
        this.status = AccountStatus.ACTIVE;
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
        this.name = requireNonBlank(name, "name");
        this.email = validateEmail(email);
    }

    /**
     * Replaces the stored password hash.
     *
     * @param passwordHash the new password hash.
     * @throws IllegalArgumentException if the hash is blank.
     */
    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = requireNonBlank(passwordHash, "passwordHash");
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

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }

    private static String validateEmail(String email) {
        String trimmedEmail = requireNonBlank(email, "email");
        if (!trimmedEmail.contains("@") || trimmedEmail.startsWith("@")
                || trimmedEmail.endsWith("@")) {
            throw new IllegalArgumentException("email must be valid");
        }
        return trimmedEmail;
    }
}
