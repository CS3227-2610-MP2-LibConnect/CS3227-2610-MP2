package libconnect.models;

import java.time.LocalDate;
import java.util.Objects;

import libconnect.util.ValidationUtils;

/**
 * Represents a library member account.
 */
public class Member extends User implements libconnect.storage.repositories.Identifiable {
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
        this.membershipId = ValidationUtils.requireNonBlank(membershipId, "membershipId");
        this.registrationDate = LocalDate.now();
    }

    /**
     * Restores a member account from persisted data.
     *
     * @param userId the stable identifier for the user.
     * @param name the member's display name.
     * @param email the member's email address.
     * @param passwordHash the hash of the member's password.
     * @param membershipId the stable identifier for the membership.
     * @param registrationDate the date on which the membership was registered.
     * @param status the current account status.
     * @throws IllegalArgumentException if a required value is invalid.
     * @throws NullPointerException if {@code registrationDate} or {@code status} is null.
     */
    public Member(String userId, String name, String email, String passwordHash,
                  String membershipId, LocalDate registrationDate, AccountStatus status) {
        super(userId, name, email, passwordHash, status);
        this.membershipId = ValidationUtils.requireNonBlank(membershipId, "membershipId");
        this.registrationDate = Objects.requireNonNull(registrationDate,
                "registrationDate cannot be null");
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

    @Override 
    public String getId() {
        return membershipId;
    }
}
