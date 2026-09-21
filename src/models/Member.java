package models;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a library member account.
 */
public class Member extends User {
    private final String membershipId;
    private final LocalDate registrationDate;

    /**
     * Creates an active library member account.
     *
     * @param userId the stable identifier for the user.
     * @param name the member's display name.
     * @param email the member's email address.
     * @param passwordHash the hash of the member's password.
     * @param membershipId the stable identifier for the membership.
     * @param registrationDate the date on which the membership was registered.
     * @throws IllegalArgumentException if a required value is invalid.
     * @throws NullPointerException if the registration date is null.
     */
    public Member(String userId, String name, String email, String passwordHash,
                  String membershipId, LocalDate registrationDate) {
        super(userId, name, email, passwordHash);
        if (membershipId == null || membershipId.isBlank()) {
            throw new IllegalArgumentException("membershipId cannot be blank");
        }
        this.membershipId = membershipId.trim();
        this.registrationDate = Objects.requireNonNull(registrationDate, "registrationDate cannot be null");
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
