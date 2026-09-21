package libconnect.models;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a library member account.
 */
public class Member extends User {
    private final String membershipId;
    private final LocalDate registrationDate;

    /**
     * Creates an active library member account with today's date as the registration date.
     *
     * @param userId the stable identifier for the user.
     * @param name the member's display name.
     * @param email the member's email address.
     * @param passwordHash the hash of the member's password.
     * @param membershipId the stable identifier for the membership.
     * @throws IllegalArgumentException if a required value is invalid.
     */
    public Member(String userId, String name, String email, String passwordHash,
                  String membershipId) {
        super(userId, name, email, passwordHash);
        if (membershipId == null || membershipId.isBlank()) {
            throw new IllegalArgumentException("membershipId cannot be blank");
        }
        this.membershipId = membershipId.trim();
        this.registrationDate = LocalDate.now();
    }

    /**
     * Returns the stable identifier for this membership.
     *
     * @return the membership ID.
     */
    public String getMembershipId() {
        return membershipId;
    }

    /**
     * Returns the date on which this membership was registered.
     *
     * @return the registration date.
     */
    public LocalDate getRegistrationDate() {
        return registrationDate;
    }
}
